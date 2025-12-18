package com.movie.hbase;

// 导入HBase相关的类库
import org.apache.hadoop.hbase.Cell;  // HBase的单元格类，代表一个具体的数据单元（行键+列族+列名+值）
import org.apache.hadoop.hbase.CellUtil;  // HBase单元格工具类，提供操作Cell的便捷方法
import org.apache.hadoop.hbase.client.Result;  // HBase查询结果类，包含一行的所有列数据
import org.apache.hadoop.hbase.client.ResultScanner;  // HBase结果扫描器，用来遍历查询结果
import org.apache.hadoop.hbase.util.Bytes;  // HBase字节转换工具类，用来在字符串和字节数组之间转换
// 导入日志相关的类库
import org.slf4j.Logger;  // 日志记录器接口
import org.slf4j.LoggerFactory;  // 日志工厂，用来创建日志记录器

// 导入Java标准库
import java.io.IOException;  // IO异常类，HBase操作出错时会抛出
import java.util.List;  // 列表接口

/**
 * 电影评分数据查询程序 - 就像一个"数据查看器"，用来查询和验证HBase中的数据
 * 
 * 这个程序的主要功能：
 * 1. 查询指定电影的详细信息
 * 2. 扫描和浏览表中的数据
 * 3. 统计数据库中的记录数量
 * 4. 测试数据删除功能
 * 5. 验证数据导入的完整性
 * 
 * 为什么需要查询程序？
 * - 验证数据导入是否成功
 * - 调试和排查数据问题
 * - 演示HBase的各种查询操作
 * - 为Web应用提供数据查询的参考实现
 * 
 * 查询操作类型：
 * 1. 精确查询：根据行键查询特定记录
 * 2. 扫描查询：遍历表中的多条记录
 * 3. 统计查询：计算表中的记录总数
 * 4. 删除操作：测试数据删除功能
 *
 * @author Movie Rating System
 * @version 1.0
 */
public class MovieRatingQuery {
  // 日志记录器，用来记录查询过程中的信息
  private static final Logger logger = LoggerFactory.getLogger(MovieRatingQuery.class);

  /**
   * 查询指定电影信息 - 就像"在图书馆找一本特定的书"
   * 
   * 查询流程：
   * 1. 根据电影ID（行键）精确定位到对应的行
   * 2. 从结果中提取电影标题和类型信息
   * 3. 如果找不到记录，给出友好的提示
   * 
   * HBase查询特点：
   * - 根据行键查询是最快的操作（O(1)时间复杂度）
   * - 如果行不存在，返回空的Result对象，不会抛出异常
   * - 需要手动检查结果是否为空
   * 
   * @param movieId 电影ID，作为HBase表的行键
   * @throws IOException 如果HBase查询失败会抛出IO异常
   */
  public static void getMovieById(String movieId) throws IOException {
    logger.info("查询电影ID: {}", movieId);

    // 调用HBaseUtil工具类查询数据，传入表名和行键
    Result result = HBaseUtil.getData(MovieRatingImporter.MOVIES_TABLE, movieId);

    // 检查查询结果是否为空
    if (result.isEmpty()) {
      logger.warn("未找到电影ID: {}", movieId);
      return;  // 提前返回，不继续处理
    }

    // 从查询结果中提取具体的列数据
    // getValue方法需要传入列族名和列名的字节数组
    String title = Bytes.toString(result.getValue(
        Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),  // 列族名转换为字节数组
        Bytes.toBytes("title")));  // 列名转换为字节数组
    String genres = Bytes.toString(result.getValue(
        Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
        Bytes.toBytes("genres")));

    // 输出查询结果
    logger.info("电影信息 - ID: {}, 标题: {}, 类型: {}", movieId, title, genres);
  }

  /**
   * 扫描电影数据 - 就像"翻阅图书馆的书籍目录"
   * 
   * 扫描操作说明：
   * 1. 扫描会遍历表中的所有行（或指定数量的行）
   * 2. 按行键的字典序返回结果
   * 3. 可以设置限制条件，避免一次性返回太多数据
   * 
   * 性能注意事项：
   * - 扫描大表会很慢，建议设置合理的限制
   * - 使用try-with-resources确保Scanner被正确关闭
   * - 扫描操作会占用HBase资源，不要长时间持有Scanner
   * 
   * @param limit 限制扫描的行数，避免输出过多数据
   * @throws IOException 如果HBase扫描失败会抛出IO异常
   */
  public static void scanMovies(int limit) throws IOException {
    logger.info("扫描电影数据，限制 {} 条记录", limit);

    // 使用HBaseUtil的限制扫描方法，返回指定数量的结果
    List<Result> results = HBaseUtil.scanTableWithLimit(MovieRatingImporter.MOVIES_TABLE, limit);

    // 遍历扫描结果
    for (Result result : results) {
      // 获取行键（电影ID）
      String movieId = Bytes.toString(result.getRow());
      
      // 提取列数据
      String title = Bytes.toString(result.getValue(
          Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
          Bytes.toBytes("title")));
      String genres = Bytes.toString(result.getValue(
          Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
          Bytes.toBytes("genres")));

      // 输出每条记录的信息
      logger.info("电影 - ID: {}, 标题: {}, 类型: {}", movieId, title, genres);
    }

    logger.info("扫描完成，共显示 {} 条电影记录", results.size());
  }

  /**
   * 扫描评分数据 - 就像"查看用户评分记录"
   * 
   * 评分数据的行键格式：userId_movieId
   * 例如：1_1 表示用户1对电影1的评分
   * 
   * 这种行键设计的优势：
   * 1. 保证唯一性：每个用户对每部电影只能有一条评分
   * 2. 便于查询：可以快速找到特定用户对特定电影的评分
   * 3. 支持前缀查询：可以查询某用户的所有评分
   * 
   * @param limit 限制扫描的行数
   * @throws IOException 如果HBase扫描失败会抛出IO异常
   */
  public static void scanRatings(int limit) throws IOException {
    logger.info("扫描评分数据，限制 {} 条记录", limit);

    // 扫描评分表
    List<Result> results = HBaseUtil.scanTableWithLimit(MovieRatingImporter.RATINGS_TABLE, limit);

    // 遍历扫描结果
    for (Result result : results) {
      // 获取行键（userId_movieId格式）
      String rowKey = Bytes.toString(result.getRow());
      
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

      // 输出评分记录信息
      logger.info("评分 - 行键: {}, 用户: {}, 电影: {}, 评分: {}, 时间: {}", 
          rowKey, userId, movieId, rating, timestamp);
    }

    logger.info("扫描完成，共显示 {} 条评分记录", results.size());
  }

  /**
   * 统计数据 - 就像"清点图书馆的藏书数量"
   * 
   * 统计操作说明：
   * 1. 使用HBase的行数统计功能
   * 2. 统计结果是精确的，不是估算值
   * 3. 大表统计可能比较慢，请耐心等待
   * 
   * 为什么要统计数据？
   * - 验证数据导入是否完整
   * - 监控数据库的数据量变化
   * - 为系统容量规划提供依据
   * - 调试和排查数据问题
   * 
   * @throws IOException 如果HBase统计失败会抛出IO异常
   */
  public static void statistics() throws IOException {
    logger.info("开始统计HBase表数据...");

    // 统计电影表的行数
    long movieCount = HBaseUtil.countRows(MovieRatingImporter.MOVIES_TABLE);
    logger.info("电影表 [{}] 总行数: {}", MovieRatingImporter.MOVIES_TABLE, movieCount);

    // 统计评分表的行数
    long ratingCount = HBaseUtil.countRows(MovieRatingImporter.RATINGS_TABLE);
    logger.info("评分表 [{}] 总行数: {}", MovieRatingImporter.RATINGS_TABLE, ratingCount);

    // 输出汇总统计信息
    logger.info("====== 数据统计汇总 ======");
    logger.info("总电影数量: {}", movieCount);
    logger.info("总评分记录: {}", ratingCount);
    
    // 计算平均每部电影的评分数（如果有电影数据的话）
    if (movieCount > 0) {
      double avgRatingsPerMovie = (double) ratingCount / movieCount;
      logger.info("平均每部电影评分数: {:.2f}", avgRatingsPerMovie);
    }
  }

  /**
   * 测试删除操作 - 就像"测试图书管理系统的删除功能"
   * 
   * 删除操作测试内容：
   * 1. 删除指定的列数据
   * 2. 删除整行数据
   * 3. 验证删除结果
   * 
   * 为什么要测试删除？
   * - 确保删除功能正常工作
   * - 演示HBase的删除操作
   * - 为实际应用提供删除操作的参考
   * 
   * 注意事项：
   * - 删除操作不可逆，请谨慎使用
   * - 建议在测试环境中执行
   * - 删除前最好先备份重要数据
   * 
   * @throws IOException 如果HBase删除操作失败会抛出IO异常
   */
  public static void testDelete() throws IOException {
    logger.info("开始测试删除操作...");

    // 测试删除指定列
    String testMovieId = "999999";  // 使用一个不太可能存在的电影ID进行测试
    
    // 先插入一条测试数据
    logger.info("插入测试数据 - 电影ID: {}", testMovieId);
    HBaseUtil.putData(MovieRatingImporter.MOVIES_TABLE, testMovieId, 
        MovieRatingImporter.MOVIES_CF_INFO, "title", "测试电影");
    HBaseUtil.putData(MovieRatingImporter.MOVIES_TABLE, testMovieId, 
        MovieRatingImporter.MOVIES_CF_INFO, "genres", "测试类型");

    // 验证数据插入成功
    Result result = HBaseUtil.getData(MovieRatingImporter.MOVIES_TABLE, testMovieId);
    if (!result.isEmpty()) {
      logger.info("✓ 测试数据插入成功");
      
      // 显示插入的数据
      String title = Bytes.toString(result.getValue(
          Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
          Bytes.toBytes("title")));
      String genres = Bytes.toString(result.getValue(
          Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
          Bytes.toBytes("genres")));
      logger.info("插入的数据 - 标题: {}, 类型: {}", title, genres);
    }

    // 测试删除指定列
    logger.info("测试删除指定列 - 删除genres列");
    HBaseUtil.deleteColumn(MovieRatingImporter.MOVIES_TABLE, testMovieId, 
        MovieRatingImporter.MOVIES_CF_INFO, "genres");

    // 验证列删除结果
    result = HBaseUtil.getData(MovieRatingImporter.MOVIES_TABLE, testMovieId);
    if (!result.isEmpty()) {
      String title = Bytes.toString(result.getValue(
          Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
          Bytes.toBytes("title")));
      String genres = Bytes.toString(result.getValue(
          Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
          Bytes.toBytes("genres")));
      
      if (title != null && genres == null) {
        logger.info("✓ 列删除测试成功 - title仍存在: {}, genres已删除", title);
      } else {
        logger.warn("✗ 列删除测试失败");
      }
    }

    // 测试删除整行
    logger.info("测试删除整行数据");
    HBaseUtil.deleteData(MovieRatingImporter.MOVIES_TABLE, testMovieId);

    // 验证行删除结果
    result = HBaseUtil.getData(MovieRatingImporter.MOVIES_TABLE, testMovieId);
    if (result.isEmpty()) {
      logger.info("✓ 行删除测试成功 - 数据已完全删除");
    } else {
      logger.warn("✗ 行删除测试失败 - 数据仍然存在");
    }

    logger.info("删除操作测试完成");
  }

  /**
   * 主程序入口 - 整个查询和验证流程的"控制中心"
   * 
   * 程序执行流程：
   * 1. 统计数据库中的数据量
   * 2. 查询指定的电影信息
   * 3. 扫描和展示部分电影数据
   * 4. 扫描和展示部分评分数据
   * 5. 测试删除功能
   * 6. 清理资源
   * 
   * 使用场景：
   * - 数据导入后的验证
   * - 系统功能测试
   * - 数据查询演示
   * - 问题排查和调试
   * 
   * 运行前提：
   * 1. HBase服务正在运行
   * 2. 已经执行过数据导入程序
   * 3. movies和ratings表已存在且有数据
   * 
   * @param args 命令行参数（当前未使用）
   */
  public static void main(String[] args) {
    try {
      logger.info("====== HBase 电影评分数据查询程序 ======");

      // 第1步：统计数据
      // 先了解数据库中有多少数据，为后续查询提供参考
      statistics();

      // 第2步：查询指定电影
      // 演示精确查询功能，查询几个具体的电影
      logger.info("\n----- 查询指定电影 -----");
      getMovieById("1");    // 查询电影ID为1的电影
      getMovieById("10");   // 查询电影ID为10的电影
      getMovieById("100");  // 查询电影ID为100的电影

      // 第3步：扫描前10部电影
      // 演示扫描查询功能，浏览部分电影数据
      logger.info("\n----- 扫描电影数据 -----");
      scanMovies(10);  // 只显示前10条记录，避免输出过多

      // 第4步：扫描前10条评分记录
      // 演示评分数据的查询
      logger.info("\n----- 扫描评分数据 -----");
      scanRatings(10);  // 只显示前10条记录

      // 第5步：测试删除操作
      // 演示和验证删除功能
      logger.info("\n----- 测试删除操作 -----");
      testDelete();

      logger.info("\n查询程序执行完成");

    } catch (IOException e) {
      // 捕获所有IO异常（HBase连接失败、查询失败等）
      logger.error("查询失败", e);
      System.exit(1);  // 异常退出，返回错误码1
    } finally {
      // 无论成功还是失败，都要关闭HBase连接，释放资源
      HBaseUtil.close();
    }
  }
}
