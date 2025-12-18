package com.movie.spark;

import com.movie.hbase.HBaseUtil;
import com.movie.hbase.MovieRatingImporter;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Spark实时计算类
 * 实现电影评分数据的实时分析计算
 */
public class RealTimeProcessing {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeProcessing.class);
    
    /**
     * 执行实时计算
     * 包括：实时评分监控、实时热门电影推荐等
     */
    public static void executeRealTimeProcessing() {
        logger.info("开始执行Spark实时计算...");
        
        try {
            // 创建Spark配置
            SparkConf conf = new SparkConf()
                    .setAppName("MovieRating-RealTimeProcessing")
                    .setMaster("local[*]"); // 使用本地模式运行
            
            // 创建Streaming上下文，设置批次间隔为5秒
            JavaStreamingContext ssc = new JavaStreamingContext(conf, Durations.seconds(5));
            
            // 1. 实时监控评分数据流
            monitorRealTimeRatings(ssc);
            
            // 2. 实时热门电影推荐
            recommendPopularMovies(ssc);
            
            // 启动Streaming context
            ssc.start();
            
            // 等待计算完成（实际应用中会持续运行）
            ssc.awaitTermination();
            
            logger.info("Spark实时计算完成");
            
        } catch (Exception e) {
            logger.error("实时计算执行失败", e);
            throw new RuntimeException("实时计算失败", e);
        }
    }
    
    /**
     * 实时监控评分数据流
     * 监控新产生的评分数据
     */
    private static void monitorRealTimeRatings(JavaStreamingContext ssc) {
        logger.info("开始实时监控评分数据流...");
        
        try {
            // 创建一个简单的数据流（实际应用中可以从Kafka、Socket等数据源获取）
            // 这里我们模拟实时数据流
            JavaDStream<String> ratingStream = ssc.textFileStream("temp/ratings"); // 模拟数据源
            
            // 对评分数据进行处理
            ratingStream.foreachRDD(rdd -> {
                if (!rdd.isEmpty()) {
                    long count = rdd.count();
                    logger.info("收到 {} 条新的评分记录", count);
                    
                    // 可以在这里添加具体的实时分析逻辑
                    // 例如：实时统计、异常检测等
                }
            });
            
        } catch (Exception e) {
            logger.error("实时监控评分数据流失败", e);
        }
    }
    
    /**
     * 实时热门电影推荐
     * 根据实时评分数据计算热门电影
     */
    private static void recommendPopularMovies(JavaStreamingContext ssc) {
        logger.info("开始实时热门电影推荐...");
        
        try {
            // 创建一个简单的数据流（实际应用中可以从Kafka、Socket等数据源获取）
            JavaDStream<String> ratingStream = ssc.textFileStream("temp/ratings"); // 模拟数据源
            
            // 对评分数据进行处理，计算实时热门电影
            ratingStream.foreachRDD(rdd -> {
                if (!rdd.isEmpty()) {
                    // 这里可以实现实时热门电影计算逻辑
                    // 例如：滑动窗口计算评分趋势、实时排名等
                    
                    long count = rdd.count();
                    logger.info("处理 {} 条评分记录用于热门电影计算", count);
                }
            });
            
        } catch (Exception e) {
            logger.error("实时热门电影推荐失败", e);
        }
    }
    
    /**
     * 从HBase中读取评分数据用于实时处理准备
     * @return 评分数据列表
     */
    private static List<String> readRatingsForRealTime() {
        List<String> ratings = new ArrayList<>();
        
        try {
            // 扫描HBase中的评分表
            ResultScanner scanner = HBaseUtil.scanTable(MovieRatingImporter.RATINGS_TABLE);
            
            for (Result result : scanner) {
                // 提取各列数据
                String userId = Bytes.toString(result.getValue(
                        Bytes.toBytes(MovieRatingImporter.RATINGS_CF_INFO),
                        Bytes.toBytes("userId")));
                String movieId = Bytes.toString(result.getValue(
                        Bytes.toBytes(MovieRatingImporter.RATINGS_CF_INFO),
                        Bytes.toBytes("movieId")));
                String rating = Bytes.toString(result.getValue(
                        Bytes.toBytes(MovieRatingImporter.RATINGS_CF_INFO),
                        Bytes.toBytes("rating")));
                String timestamp = Bytes.toString(result.getValue(
                        Bytes.toBytes(MovieRatingImporter.RATINGS_CF_INFO),
                        Bytes.toBytes("timestamp")));
                
                // 组合成CSV格式
                ratings.add(String.format("%s,%s,%s,%s", userId, movieId, rating, timestamp));
            }
            
            scanner.close();
            
        } catch (IOException e) {
            logger.error("从HBase读取评分数据失败", e);
        }
        
        return ratings;
    }
}