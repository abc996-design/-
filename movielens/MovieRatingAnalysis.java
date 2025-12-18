package movielens;


import org.apache.spark.SparkConf;
import org.apache.spark.sql.*;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * spark 计算电影平均评分，批量写入hbase
 */
public class MovieRatingAnalysis {

    public static void main(String[] args) throws IOException {
        SparkSession spark = SparkSession.builder()
                .master("local[*]")
                .appName("WriteToHBase")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        // 电影评分数据
        Dataset<Row> userRatingDF = spark.read()
                .option("sep", ",")
                .option("header", true)
                .option("encoding", "utf-8")
                .schema("userId int,movieId int,rating double,timestamp long")
                .csv("file:///D:/workspace/movie-data-test/data/ml-latest-small/ratings.csv")

//                .csv("file:///E:/workspace/spark-movie/data/ratings.csv")
                ;
        System.out.println("打印评分信息数据");
        userRatingDF.show();

        // 读取电影信息数据
        Dataset<Row> movieInfoDF = spark.read()
                .option("inferSchema", true)
                .option("header", true)
                .option("encoding", "utf-8")
                .csv("file:///D:/workspace/movie-data-test/data/ml-latest-small/movies.csv")
//                .csv("file:///E:/workspace/spark-movie/data/movies.csv")
        ;


        System.out.println("打印电影信息数据");
        movieInfoDF.show(false);

        // 计算每部电影的最高分，最低分，平均分
        Dataset<Row> movieRatingRDD = userRatingDF.groupBy("movieId").agg(
                functions.count(functions.col("userId")).cast(DataTypes.IntegerType).alias("rating_user_count"),
                functions.round(functions.sum(functions.col("rating")),3).cast(DataTypes.DoubleType).alias("sum_rating"),
                functions.round(functions.max(functions.col("rating")),3).cast(DataTypes.DoubleType).alias("max_rating"),
                functions.round(functions.avg(functions.col("rating")),3).cast(DataTypes.DoubleType).alias("avg_rating"),
                functions.round(functions.min(functions.col("rating")),3).cast(DataTypes.DoubleType).alias("min_rating")

        );

        System.out.println("打印电影评分信息");
        movieRatingRDD.show(false);

        // movieId字段, 电影信息左连接平均分
        Dataset<Row> movieRatingInfoRDD = movieInfoDF
                .join(movieRatingRDD, movieRatingRDD.col("movieId").equalTo(movieInfoDF.col("movieId")), "left")
                .select(movieInfoDF.col("movieId"), movieInfoDF.col("title"), movieInfoDF.col("genres")
                        , movieRatingRDD.col("max_rating"),movieRatingRDD.col("avg_rating"),
                        movieRatingRDD.col("min_rating")

                )
                .withColumn("max_rating",functions.when(functions.col("max_rating").isNull(), 0).otherwise(functions.col("max_rating")))
                .withColumn("avg_rating",functions.when(functions.col("avg_rating").isNull(), 0).otherwise(functions.col("avg_rating")))
                .withColumn("min_rating",functions.when(functions.col("min_rating").isNull(), 0).otherwise(functions.col("min_rating")))

                ;

        System.out.println("打印电影和评分信息");
        movieRatingInfoRDD.show(false);

        // 并行化写入 HBase
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(dateFormat.format(new Date())+" 开始写入 "+movieRatingInfoRDD.count()+" 条电影信息和评分到hbase");

        movieRatingInfoRDD.foreachPartition(iterator ->{
            HbaseUtil hbaseUtil = new HbaseUtil(new SparkConf());

            while(iterator.hasNext()){
                Row row = iterator.next();
                if(row.size() < 6){continue;}
//                System.out.println(row.toString());
                String movieId = String.valueOf(row.getInt(0));
                String title = row.getString(1);
                String genres = row.getString(2);
                String max_rating = String.valueOf(row.getDouble(3));
                String avg_rating = String.valueOf(row.getDouble(4));
                String min_rating = String.valueOf(row.getDouble(5));


                String table = "movie_rate_test";   //hbase 表名
                String rowkey = movieId;
                String cf1 = "movieinfo"; // 列族1
                String cf2 = "rateinfo";  // 列族2
                String movieIdColumn = "movieId";
                String titleColumn = "title";
                String genresColumn = "genres";
                String timestampColumn = "timestamp";

                String maxRateColumn = "max_rating";
                String avgRateColumn = "avg_rating";
                String minRateColumn = "min_rating";

                String ts = dateFormat.format(new Date());

                hbaseUtil.putData(table, rowkey, cf1, movieIdColumn, movieId);
                hbaseUtil.putData(table, rowkey, cf1, titleColumn, title);
                hbaseUtil.putData(table, rowkey, cf1, genresColumn, genres);
//                hbaseUtil.putData(table, rowkey, cf1, timestampColumn, ts);

                hbaseUtil.putData(table, rowkey, cf2, maxRateColumn, max_rating);
                hbaseUtil.putData(table, rowkey, cf2, avgRateColumn, avg_rating);
                hbaseUtil.putData(table, rowkey, cf2, minRateColumn, min_rating);
                hbaseUtil.putData(table, rowkey, cf2, timestampColumn, ts);
            }

        });
        System.out.println(dateFormat.format(new Date())+" 写入完成");

        // 电影评分数据并行化写入 HBase
//        userRatingDF.foreachPartition(iterator ->{
//            HbaseUtil hbaseUtil = new HbaseUtil(new SparkConf());
//
//            while(iterator.hasNext()){
//                Row row = iterator.next();
//                Integer userId = row.getInt(0);
//                Integer movieId = row.getInt(1);
//                Double rating = row.getDouble(2);
//                long timestamp = row.getLong(3);
//
//                String table = "movie_rate_test";
//                String rowkey = String.valueOf(movieId);
//                String cf = "rateinfo";
//                String movieIdColumn = "movieId";
//                String userIdColumn = "userId";
//                String ratingColumn = "rating";
//                String timestampColumn = "timestamp";
//                long ts = new Date().getTime();
//
//                hbaseUtil.putData(table, rowkey, cf, userIdColumn, String.valueOf(userId));
//                hbaseUtil.putData(table, rowkey, cf, movieIdColumn, String.valueOf(movieId));
//                hbaseUtil.putData(table, rowkey, cf, ratingColumn, String.valueOf(rating));
//                hbaseUtil.putData(table, rowkey, cf, timestampColumn, String.valueOf(ts));
//
//            }
//
//        });


    }

}