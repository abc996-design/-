package com.movie.hbase;

// 导入CSV文件处理相关的类库
import com.opencsv.CSVReader;  // OpenCSV库的CSV文件读取器，专门用来解析CSV格式文件
import com.opencsv.exceptions.CsvException;  // CSV解析异常类，当CSV文件格式有问题时会抛出
// 导入HBase相关的类库
import org.apache.hadoop.hbase.TableName;  // HBase表名类，用来表示和操作表名
import org.apache.hadoop.hbase.client.Put;  // HBase的Put操作类，用来插入数据
import org.apache.hadoop.hbase.client.Table;  // HBase表操作接口，用来获取表的操作句柄
import org.apache.hadoop.hbase.util.Bytes;  // HBase字节转换工具类，用来在字符串和字节数组之间转换
// 导入日志相关的类库
import org.slf4j.Logger;  // 日志记录器接口
import org.slf4j.LoggerFactory;  // 日志工厂，用来创建日志记录器

// 导入Java标准库
import java.io.FileReader;  // 文件读取器，用来读取本地文件
import java.io.IOException;  // IO异常类，文件操作出错时会抛出
import java.util.ArrayList;  // 动态数组，用来存储批量插入的数据
import java.util.List;  // 列表接口
import java.util.Map;  // Map接口（虽然这里没用到，但保留原有导入）

/**
 * 电影评分数据导入程序 - 就像一个"数据搬运工"，把CSV文件中的数据搬到HBase数据库里
 * 
 * 这个程序的主要工作：
 * 1. 读取MovieLens数据集的CSV文件（movies.csv 和 ratings.csv）
 * 2. 解析CSV文件中的每一行数据
 * 3. 将数据转换成HBase能理解的格式
 * 4. 批量插入到HBase表中，提高导入效率
 * 
 * 为什么要批量导入？
 * - 单条插入太慢：每次插入都要网络通信，效率低
 * - 批量插入更快：一次性插入多条数据，减少网络开销
 * - 内存控制：设置批次大小，避免一次性加载太多数据导致内存不足
 * 
 * 数据表设计：
 * 1. movies表：存储电影基本信息（ID、标题、类型）
 * 2. ratings表：存储用户评分记录（用户ID、电影ID、评分、时间戳）
 *
 * @author Movie Rating System
 * @version 1.0
 */
public class MovieRatingImporter {
  // 日志记录器，用来记录导入过程中的信息
  private static final Logger logger = LoggerFactory.getLogger(MovieRatingImporter.class);

  // ========== 表名和列族定义 ==========
  // 这些常量定义了HBase中的表结构，就像数据库的表名和字段名
  
  /** 电影信息表名 - 存储所有电影的基本信息 */
  public static final String MOVIES_TABLE = "movies";
  /** 电影信息表的列族名 - HBase中列的分组，所有电影相关字段都放在这个列族下 */
  public static final String MOVIES_CF_INFO = "info";

  /** 评分记录表名 - 存储所有用户的评分记录 */
  public static final String RATINGS_TABLE = "ratings";
  /** 评分记录表的列族名 - 所有评分相关字段都放在这个列族下 */
  public static final String RATINGS_CF_INFO = "info";

  // ========== 性能优化参数 ==========
  /** 批量插入的批次大小 - 每次批量插入多少条数据 */
  private static final int BATCH_SIZE = 1000;  // 1000条是一个比较好的平衡点：既不会太慢，也不会占用太多内存

  /**
   * 初始化HBase表结构 - 就像"搭建房子的框架"，先把表和列族创建好
   * 
   * 这个方法会：
   * 1. 检查表是否已经存在，避免重复创建
   * 2. 如果不存在就创建新表
   * 3. 设置列族的参数（比如数据保留时间、压缩方式等）
   * 
   * 为什么要先创建表？
   * - HBase不像MySQL，不能在插入数据时自动创建表
   * - 必须先定义好表结构，包括列族
   * - 列族一旦创建就不容易修改，所以要提前规划好
   *
   * @throws IOException 如果创建表失败会抛出IO异常
   */
  public static void initTables() throws IOException {
    logger.info("开始初始化HBase表结构...");

    // 创建电影信息表
    // 检查表是否存在，避免重复创建（重复创建会报错）
    if (!HBaseUtil.tableExists(MOVIES_TABLE)) {
      logger.info("创建电影信息表: {}", MOVIES_TABLE);
      // 创建表，指定表名和列族名
      HBaseUtil.createTable(MOVIES_TABLE, MOVIES_CF_INFO);
      logger.info("✓ 电影信息表创建成功");
    } else {
      logger.info("电影信息表已存在，跳过创建");
    }

    // 创建评分记录表
    if (!HBaseUtil.tableExists(RATINGS_TABLE)) {
      logger.info("创建评分记录表: {}", RATINGS_TABLE);
      HBaseUtil.createTable(RATINGS_TABLE, RATINGS_CF_INFO);
      logger.info("✓ 评分记录表创建成功");
    } else {
      logger.info("评分记录表已存在，跳过创建");
    }

    logger.info("表结构初始化完成");
  }

  /**
   * 导入电影数据 - 就像"把电影信息录入到数据库"
   * 
   * CSV文件格式（movies.csv）：
   * movieId,title,genres
   * 1,Toy Story (1995),Adventure|Animation|Children|Comedy|Fantasy
   * 2,Jumanji (1995),Adventure|Children|Fantasy
   * 
   * 数据处理流程：
   * 1. 逐行读取CSV文件
   * 2. 解析每行的电影ID、标题、类型
   * 3. 创建Put对象（HBase的插入操作）
   * 4. 批量插入到HBase表中
   * 5. 记录导入进度和统计信息
   *
   * @param csvFilePath CSV文件的完整路径
   * @return 成功导入的记录数量
   * @throws IOException 如果文件读取或数据插入失败会抛出IO异常
   */
  public static int importMovies(String csvFilePath) throws IOException {
    logger.info("开始导入电影数据: {}", csvFilePath);

    int totalCount = 0;      // 总共导入的记录数
    int batchCount = 0;      // 批次计数器，记录执行了多少次批量插入
    List<Put> puts = new ArrayList<>();  // 存储批量插入数据的列表

    // 使用try-with-resources自动关闭文件资源
    try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
      // 一次性读取所有行（对于小文件这样做比较简单）
      List<String[]> records = reader.readAll();

      // 跳过标题行（第一行通常是列名），从第二行开始处理数据
      for (int i = 1; i < records.size(); i++) {
        String[] record = records.get(i);  // 获取当前行的数据

        // 检查数据完整性：确保至少有3列（movieId, title, genres）
        if (record.length >= 3) {
          // 提取并清理数据（去掉前后空格）
          String movieId = record[0].trim();  // 电影ID，作为HBase的行键
          String title = record[1].trim();    // 电影标题
          String genres = record[2].trim();   // 电影类型（可能包含多个类型，用|分隔）

          // 创建Put对象，指定行键（使用电影ID作为唯一标识）
          Put put = new Put(Bytes.toBytes(movieId));
          // 添加列数据：列族:列名 = 值
          put.addColumn(Bytes.toBytes(MOVIES_CF_INFO), Bytes.toBytes("title"), Bytes.toBytes(title));
          put.addColumn(Bytes.toBytes(MOVIES_CF_INFO), Bytes.toBytes("genres"), Bytes.toBytes(genres));

          // 将Put对象添加到批量插入列表中
          puts.add(put);
          totalCount++;  // 增加总计数

          // 当达到批次大小时，执行批量插入
          if (puts.size() >= BATCH_SIZE) {
            HBaseUtil.putDataBatch(MOVIES_TABLE, puts);  // 批量插入到HBase
            batchCount++;  // 增加批次计数
            logger.info("已导入 {} 批电影数据，共 {} 条记录", batchCount, totalCount);
            puts.clear();  // 清空列表，准备下一批数据
          }
        }
      }

      // 处理剩余的数据（最后一批可能不足BATCH_SIZE）
      if (!puts.isEmpty()) {
        HBaseUtil.putDataBatch(MOVIES_TABLE, puts);
        batchCount++;
      }

      logger.info("电影数据导入完成，共导入 {} 条记录，分 {} 批次", totalCount, batchCount);

    } catch (CsvException e) {
      // CSV文件格式错误的处理
      logger.error("CSV文件解析错误", e);
      throw new IOException("CSV文件解析错误", e);
    }

    return totalCount;  // 返回导入的记录数
  }

  /**
   * 导入评分数据 - 就像"把用户评分记录录入到数据库"
   * 
   * CSV文件格式（ratings.csv）：
   * userId,movieId,rating,timestamp
   * 1,1,4.0,964982703
   * 1,3,4.0,964981247
   * 
   * 行键设计说明：
   * 使用 "userId_movieId" 作为行键，比如 "1_1"
   * 这样设计的好处：
   * 1. 保证唯一性：每个用户对每部电影只能有一条评分记录
   * 2. 便于查询：可以快速查找某用户对某电影的评分
   * 3. 支持范围查询：可以查询某用户的所有评分（以userId开头的行）
   *
   * @param csvFilePath CSV文件的完整路径
   * @return 成功导入的记录数量
   * @throws IOException 如果文件读取或数据插入失败会抛出IO异常
   */
  public static int importRatings(String csvFilePath) throws IOException {
    logger.info("开始导入评分数据: {}", csvFilePath);

    int totalCount = 0;      // 总共导入的记录数
    int batchCount = 0;      // 批次计数器
    List<Put> puts = new ArrayList<>();  // 存储批量插入数据的列表

    try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
      List<String[]> records = reader.readAll();

      // 跳过标题行，从第二行开始处理数据
      for (int i = 1; i < records.size(); i++) {
        String[] record = records.get(i);

        // 检查数据完整性：确保至少有4列（userId, movieId, rating, timestamp）
        if (record.length >= 4) {
          // 提取并清理数据
          String userId = record[0].trim();      // 用户ID
          String movieId = record[1].trim();     // 电影ID
          String rating = record[2].trim();      // 评分（通常是1.0-5.0）
          String timestamp = record[3].trim();   // 时间戳（Unix时间戳）

          // 构造行键：userId_movieId，确保每个用户对每部电影只有一条评分记录
          String rowKey = userId + "_" + movieId;

          // 创建Put对象
          Put put = new Put(Bytes.toBytes(rowKey));
          // 添加各列数据
          put.addColumn(Bytes.toBytes(RATINGS_CF_INFO), Bytes.toBytes("userId"), Bytes.toBytes(userId));
          put.addColumn(Bytes.toBytes(RATINGS_CF_INFO), Bytes.toBytes("movieId"), Bytes.toBytes(movieId));
          put.addColumn(Bytes.toBytes(RATINGS_CF_INFO), Bytes.toBytes("rating"), Bytes.toBytes(rating));
          put.addColumn(Bytes.toBytes(RATINGS_CF_INFO), Bytes.toBytes("timestamp"), Bytes.toBytes(timestamp));

          puts.add(put);
          totalCount++;

          // 批量插入处理
          if (puts.size() >= BATCH_SIZE) {
            HBaseUtil.putDataBatch(RATINGS_TABLE, puts);
            batchCount++;
            logger.info("已导入 {} 批评分数据，共 {} 条记录", batchCount, totalCount);
            puts.clear();
          }
        }
      }

      // 处理剩余数据
      if (!puts.isEmpty()) {
        HBaseUtil.putDataBatch(RATINGS_TABLE, puts);
        batchCount++;
      }

      logger.info("评分数据导入完成，共导入 {} 条记录，分 {} 批次", totalCount, batchCount);

    } catch (CsvException e) {
      logger.error("CSV文件解析错误", e);
      throw new IOException("CSV文件解析错误", e);
    }

    return totalCount;
  }

  /**
   * 主程序入口 - 整个数据导入流程的"指挥中心"
   * 
   * 完整的导入流程：
   * 1. 初始化表结构（创建movies和ratings表）
   * 2. 导入电影数据（从movies.csv）
   * 3. 导入评分数据（从ratings.csv）
   * 4. 统计导入结果
   * 5. 验证数据完整性（确保没有数据丢失）
   * 6. 清理资源（关闭HBase连接）
   * 
   * 错误处理：
   * - 如果任何步骤失败，程序会记录错误并退出
   * - 使用finally块确保资源被正确释放
   * 
   * 使用方法：
   * 1. 确保HBase服务正在运行
   * 2. 将MovieLens数据集放在项目根目录的ml-latest-small文件夹中
   * 3. 运行这个main方法
   *
   * @param args 命令行参数（当前未使用）
   */
  public static void main(String[] args) {
    try {
      logger.info("====== HBase 电影评分数据导入程序 ======");

      // 第1步：初始化表结构
      // 这一步很重要，必须先创建表才能插入数据
      initTables();

      // 第2步：导入电影数据
      // 指定CSV文件路径（相对于项目根目录）
      String moviesFile = "ml-latest-small/movies.csv";
      int movieCount = importMovies(moviesFile);

      // 第3步：导入评分数据
      String ratingsFile = "ml-latest-small/ratings.csv";
      int ratingCount = importRatings(ratingsFile);

      // 第4步：输出导入统计信息
      logger.info("====== 数据导入统计 ======");
      logger.info("电影数量: {}", movieCount);
      logger.info("评分记录数量: {}", ratingCount);

      // 第5步：验证数据完整性
      // 通过统计HBase中的实际行数来验证是否有数据丢失
      logger.info("====== 验证数据完整性 ======");
      long actualMovieCount = HBaseUtil.countRows(MOVIES_TABLE);
      long actualRatingCount = HBaseUtil.countRows(RATINGS_TABLE);

      logger.info("实际存储电影数量: {}", actualMovieCount);
      logger.info("实际存储评分数量: {}", actualRatingCount);

      // 比较预期数量和实际数量
      if (actualMovieCount == movieCount && actualRatingCount == ratingCount) {
        logger.info("✓ 数据导入验证通过，无数据丢失");
      } else {
        logger.warn("✗ 数据导入验证失败，可能存在数据丢失");
        logger.warn("电影数据: 预期={}, 实际={}", movieCount, actualMovieCount);
        logger.warn("评分数据: 预期={}, 实际={}", ratingCount, actualRatingCount);
      }

    } catch (IOException e) {
      // 捕获所有IO异常（文件读取失败、HBase连接失败等）
      logger.error("数据导入失败", e);
      System.exit(1);  // 异常退出，返回错误码1
    } finally {
      // 无论成功还是失败，都要关闭HBase连接，释放资源
      HBaseUtil.close();
    }
  }
}
