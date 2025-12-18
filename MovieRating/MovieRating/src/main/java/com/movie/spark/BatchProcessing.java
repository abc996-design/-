package com.movie.spark;

import com.movie.hbase.HBaseUtil;
import com.movie.hbase.MovieRatingImporter;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Spark批处理计算类
 * 实现电影评分数据的批处理分析计算
 */
public class BatchProcessing {
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessing.class);
    
    /**
     * 执行批处理计算
     * 包括：平均评分计算、热门电影排行等分析
     */
    public static void executeBatchProcessing() {
        logger.info("开始执行Spark批处理计算...");
        
        try {
            // 创建Spark配置
            SparkConf conf = new SparkConf()
                    .setAppName("MovieRating-BatchProcessing")
                    .setMaster("local[*]"); // 使用本地模式运行
            
            // 创建Spark上下文
            JavaSparkContext sc = new JavaSparkContext(conf);
            SparkSession spark = SparkSession.builder()
                    .config(conf)
                    .getOrCreate();
            
            // 1. 计算平均评分
            calculateAverageRating(sc, spark);
            
            // 2. 计算热门电影排行
            calculatePopularMovies(sc, spark);
            
            // 3. 计算用户评分统计
            calculateUserRatings(sc, spark);
            
            // 关闭Spark上下文
            sc.close();
            spark.close();
            
            logger.info("Spark批处理计算完成");
            
        } catch (Exception e) {
            logger.error("批处理计算执行失败", e);
            throw new RuntimeException("批处理计算失败", e);
        }
    }
    
    /**
     * 计算平均评分
     * 从HBase中读取评分数据，计算所有电影的平均评分
     */
    private static void calculateAverageRating(JavaSparkContext sc, SparkSession spark) {
        logger.info("开始计算平均评分...");
        
        try {
            // 从HBase读取评分数据
            List<String> ratingsData = readRatingsFromHBase();
            
            // 转换为Spark RDD
            JavaRDD<String> ratingsRDD = sc.parallelize(ratingsData);
            
            // 计算平均评分（这里只是一个示例，实际可以做更复杂的计算）
            double averageRating = ratingsRDD.map(line -> {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        return Double.parseDouble(parts[2]); // 评分字段
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                }
                return 0.0;
            }).reduce((a, b) -> a + b) / ratingsRDD.count();
            
            logger.info("所有电影的平均评分为: {:.2f}", averageRating);
            
        } catch (Exception e) {
            logger.error("计算平均评分失败", e);
        }
    }
    
    /**
     * 计算热门电影排行
     * 根据评分数量和平均评分计算热门电影
     */
    private static void calculatePopularMovies(JavaSparkContext sc, SparkSession spark) {
        logger.info("开始计算热门电影排行...");
        
        try {
            // 从HBase读取评分数据
            List<String> ratingsData = readRatingsFromHBase();
            
            // 转换为Spark RDD
            JavaRDD<String> ratingsRDD = sc.parallelize(ratingsData);
            
            // 统计每部电影的评分数量和总评分
            // 这里简化处理，实际可以做更复杂的分析
            long totalRatings = ratingsRDD.count();
            
            logger.info("总评分数量: {}", totalRatings);
            
            // 按电影ID分组，计算每部电影的平均评分
            // 这里只是示例，实际需要更复杂的处理逻辑
            
        } catch (Exception e) {
            logger.error("计算热门电影排行失败", e);
        }
    }
    
    /**
     * 计算用户评分统计
     * 统计每个用户的评分行为
     */
    private static void calculateUserRatings(JavaSparkContext sc, SparkSession spark) {
        logger.info("开始计算用户评分统计...");
        
        try {
            // 从HBase读取评分数据
            List<String> ratingsData = readRatingsFromHBase();
            
            // 转换为Spark RDD
            JavaRDD<String> ratingsRDD = sc.parallelize(ratingsData);
            
            // 统计每个用户的评分数量
            long userCount = ratingsRDD.map(line -> {
                String[] parts = line.split(",");
                if (parts.length >= 1) {
                    return parts[0]; // 用户ID
                }
                return "";
            }).distinct().count();
            
            logger.info("评分用户总数: {}", userCount);
            
        } catch (Exception e) {
            logger.error("计算用户评分统计失败", e);
        }
    }
    
    /**
     * 从HBase中读取评分数据
     * @return 评分数据列表
     */
    private static List<String> readRatingsFromHBase() {
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