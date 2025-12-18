package com.movie;

import com.movie.hbase.HBaseUtil;
import com.movie.hbase.MovieRatingImporter;
import com.movie.hbase.MovieRatingQuery;
import com.movie.spark.BatchProcessing;
import com.movie.spark.RealTimeProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 电影评分数据处理主程序
 * 
 * 实现三个实验步骤：
 * 1. 完整数据导入 - 将CSV数据导入到HBase
 * 2. 批处理计算 - 使用Spark进行数据分析
 * 3. 实时计算 - 使用Spark Streaming进行实时处理
 */
public class MainApp {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        try {
            logger.info("====== 开始电影评分数据处理实验 ======");
            
            // 步骤1: 完整数据导入
            logger.info("开始执行步骤1: 完整数据导入");
            executeDataImport();
            
            // 步骤2: 批处理计算
            logger.info("开始执行步骤2: 批处理计算");
            executeBatchProcessing();
            
            // 步骤3: 实时计算
            logger.info("开始执行步骤3: 实时计算");
            executeRealTimeProcessing();
            
            logger.info("====== 电影评分数据处理实验完成 ======");
            
        } catch (Exception e) {
            logger.error("程序执行出错", e);
            System.exit(1);
        } finally {
            // 关闭HBase连接
            HBaseUtil.close();
        }
    }

    /**
     * 执行完整数据导入步骤
     * 将movies.csv和ratings.csv文件中的数据导入到HBase中
     */
    private static void executeDataImport() {
        try {
            // 初始化HBase表结构
            MovieRatingImporter.initTables();
            
            // 导入电影数据
            String moviesFile = "ml-latest-small/movies.csv";
            int movieCount = MovieRatingImporter.importMovies(moviesFile);
            logger.info("电影数据导入完成，共导入 {} 条记录", movieCount);
            
            // 导入评分数据
            String ratingsFile = "ml-latest-small/ratings.csv";
            int ratingCount = MovieRatingImporter.importRatings(ratingsFile);
            logger.info("评分数据导入完成，共导入 {} 条记录", ratingCount);
            
            // 验证数据完整性
            long actualMovieCount = HBaseUtil.countRows(MovieRatingImporter.MOVIES_TABLE);
            long actualRatingCount = HBaseUtil.countRows(MovieRatingImporter.RATINGS_TABLE);
            
            logger.info("数据验证 - 电影实际数量: {}, 评分实际数量: {}", actualMovieCount, actualRatingCount);
            
        } catch (IOException e) {
            logger.error("数据导入失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行批处理计算步骤
     * 使用Spark对HBase中的数据进行分析计算
     */
    private static void executeBatchProcessing() {
        try {
            logger.info("批处理计算开始...");
            
            // 执行Spark批处理计算
            BatchProcessing.executeBatchProcessing();
            
            logger.info("批处理计算完成");
            
        } catch (Exception e) {
            logger.error("批处理计算失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行实时计算步骤
     * 使用Spark Streaming对实时数据进行处理
     */
    private static void executeRealTimeProcessing() {
        try {
            logger.info("实时计算开始...");
            
            // 执行Spark实时计算
            RealTimeProcessing.executeRealTimeProcessing();
            
            logger.info("实时计算完成");
            
        } catch (Exception e) {
            logger.error("实时计算失败", e);
            throw new RuntimeException(e);
        }
    }
}