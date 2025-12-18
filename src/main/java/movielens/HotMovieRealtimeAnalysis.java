package movielens;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * spark结构化流实时消费kafka中的音乐评分数据
 */
public class HotMovieRealtimeAnalysis {

    public static void main(String[] args) throws Exception {
        // 初始化SparkSession
        SparkSession spark = SparkSession
                .builder()
                .appName("JavaStructuredStreamingWordCount")
                .master("local[2]")
                .config("spark.sql.shuffle.partitions", "3")
                .getOrCreate();
        spark.sparkContext().setLogLevel("WARN");

        //读取电影信息
        JavaPairRDD<Integer, String> movieInfoPairRDD = spark.read()
//                .option("inferSchema", true)
                .option("header", true)
                .option("encoding", "utf-8")
                .schema("movieId int, title string, genres string, timestamp long")
                .csv("file:///D:/workspace/movie-data-test/data/ml-latest-small/movies.csv")
                .select("movieId", "title")
                .toJavaRDD()
                .mapToPair(new PairFunction<Row, Integer, String>() {
                            @Override
                            public Tuple2<Integer, String> call(Row row) {
                                return new Tuple2<>(row.getInt(0), row.getString(1));
                            }
                        }
                )
                ;
        Map<Integer, String> movieHM = movieInfoPairRDD.collectAsMap();

        //注册自定义函数
        UserDefinedFunction myFunc = functions.udf((Integer input) -> {
            return movieHM.getOrDefault(input, "未知"); // 示例：将输入字符串转换为大写
        }, DataTypes.StringType);

        spark.udf().register("myFunc", myFunc);

        // mysql连接参数
        String jdbcUrl = "jdbc:mysql://192.168.150.137:3306/test?useUnicode=true&characterEncoding=UTF-8&useSSL=false&autoConnect=true";
        String tableName = "hot_movie_rates";
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", "root");
        connectionProperties.put("password", "123456");
        connectionProperties.put("driver", "com.mysql.jdbc.Driver");
        connectionProperties.put("maximumPoolSize", "10");
        connectionProperties.put("minimumIdle", "2");
        connectionProperties.put("connectionTimeout", "30000");
        connectionProperties.put("idleTimeout", "600000");

        // 定义电影评分的数据结构
        StructType messageSchema = new StructType(new StructField[]{
                new StructField("userId", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("movieId", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("rating", DataTypes.DoubleType, true, Metadata.empty()),
                new StructField("timestamp", DataTypes.LongType, true, Metadata.empty()),
                new StructField("createTime", DataTypes.StringType, true, Metadata.empty())
        });

        // 读取Kafka数据流
        Dataset<Row> kafkaStream = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "192.168.150.137:9092")
                .option("subscribe", "movie_rate")
                .option("startingOffsets", "latest")
                .option("enable.auto.commit", "true")
                .option("group.id", "movie_group1")
                .load();

        // 处理数据,并转换
        Dataset<Row> userRatingDF = kafkaStream.selectExpr("CAST(value AS STRING)")
                .select(functions.from_json(functions.col("value"), messageSchema).alias("data"))
                .selectExpr("data.userId", "data.movieId","data.rating", "data.timestamp", "data.createTime")
                .withColumn("name", myFunc.apply(functions.col("movieId")))
                .withColumn("createTimeTs", functions.to_timestamp(functions.col("createTime"), "yyyy-MM-dd HH:mm:ss"))
        ;

        // 分流处理一，实时统计最近10分钟的热门电影评分次数,定义10分钟的滚动窗口
        Dataset<Row> recentTenMovieRatingCountDF = userRatingDF
             .groupBy(functions.window(functions.col("createTimeTs"), "10 minutes", "10 minutes"), functions.col("name"))
                .agg(
                    functions.count(functions.col("userId")).cast(DataTypes.IntegerType).alias("count"),
                    functions.round(functions.avg(functions.col("rating")),3).cast(DataTypes.DoubleType).alias("avgRating")

                )
                .withColumn("startTime", functions.date_format(functions.col("window.start"), "yyyy-MM-dd HH:mm:ss")) // 格式化开始时间
                .withColumn("endTime", functions.date_format(functions.col("window.end"), "yyyy-MM-dd HH:mm:ss")) // 格式化结束时间
                .withColumn("updateTime", functions.date_format(functions.current_timestamp(), "yyyy-MM-dd HH:mm:ss")) // 格式化结束时间
                .select("startTime", "endTime", "name", "count", "avgRating", "updateTime")
                .orderBy( functions.col("startTime").desc(), functions.col("endTime").desc(), functions.col("count").desc())  // 降序排列, 取前10
                .limit(10)
                ;

        StreamingQuery query = recentTenMovieRatingCountDF
                .writeStream()
//                .format("console")
                .option("checkpointLocation", "file:///D:/workspace/movie-data-test/data/movie_checkpoints")
                .outputMode(OutputMode.Complete()) // 使用Append模式将新数据追加到表中
                .trigger(Trigger.ProcessingTime(60, TimeUnit.SECONDS))  // 处理时间间隔, 60秒
                .foreachBatch((Dataset<Row> batch, Long batchId) -> {
                    System.out.println("热点评分次数更新至MySQL...统计批次"+batchId);
                    batch.write().mode(SaveMode.Overwrite).jdbc(jdbcUrl, tableName, connectionProperties);
                })
                .start();
        query.awaitTermination();

        // 分流处理二，评分数据立刻写入hbase
//        StreamingQuery query1 = userRatingRDD
//                .writeStream()
//                .option("checkpointLocation", "/data/movie_checkpoints1") // 检查点位置，用于故障恢复
//                .outputMode(OutputMode.Append()) // 使用Append模式将新数据追加到表中
//                .trigger(Trigger.ProcessingTime(100)) // 处理时间100毫秒, 尽可能快的处理
//                .foreachBatch((Dataset<Row> batch, Long batchId) -> {
//
//                    batch.foreachPartition(iterator -> {
//                        HbaseUtil hbaseUtil = new HbaseUtil(new SparkConf());
//                        //写hbase
//                        while(iterator.hasNext()){
//                            Row row = iterator.next();
//                            Integer userId = row.getInt(0);
//                            Integer movieId = row.getInt(1);
//                            Double rating = row.getDouble(2);
//                            long timestamp = row.getLong(3);
//
//                            String table = "movie_rate";
//                            String rowkey = String.valueOf(movieId);
//                            String cf = "rateinfo";
//                            String movieIdColumn = "movieId";
//                            String userIdColumn = "userId";
//                            String ratingColumn = "rating";
//                            String timestampColumn = "timestamp";
//                            long ts = new Date().getTime();
//
//                            hbaseUtil.putData(table, rowkey, cf, userIdColumn, String.valueOf(userId));
//                            hbaseUtil.putData(table, rowkey, cf, movieIdColumn, String.valueOf(movieId));
//                            hbaseUtil.putData(table, rowkey, cf, ratingColumn, String.valueOf(rating));
//                            hbaseUtil.putData(table, rowkey, cf, timestampColumn, String.valueOf(ts));
//                        }
//                    });
//                })
//                .start()
//                ;
//        query1.awaitTermination();// 等待流处理完成（在本地模式下，这通常意味着等待你手动停止应用）

        spark.stop();
    }
}
