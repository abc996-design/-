package com.movie.web.service;

// 导入HBase相关的工具类
import com.movie.hbase.HBaseUtil;  // 自定义的HBase工具类，封装了常用的HBase操作
import com.movie.hbase.MovieRatingImporter;  // 数据导入类，定义了表名和列族常量
// 导入Web层的实体类
import com.movie.web.entity.Movie;  // 电影实体类，用来封装电影信息
import com.movie.web.entity.Rating;  // 评分实体类，用来封装评分信息
// 导入HBase原生API
import org.apache.hadoop.hbase.Cell;  // HBase单元格类，代表一个数据单元
import org.apache.hadoop.hbase.CellUtil;  // HBase单元格工具类，提供操作Cell的便捷方法
import org.apache.hadoop.hbase.TableName;  // HBase表名类
import org.apache.hadoop.hbase.client.*;  // HBase客户端API，包含Connection、Table、Scan等
import org.apache.hadoop.hbase.filter.CompareFilter;  // HBase比较过滤器
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;  // HBase单列值过滤器，用来按列值筛选数据
import org.apache.hadoop.hbase.filter.PrefixFilter;  // 按rowkey前缀过滤
import org.apache.hadoop.hbase.filter.RowFilter;  // 按rowkey正则过滤
import org.apache.hadoop.hbase.filter.RegexStringComparator;  // 正则比较器
import org.apache.hadoop.hbase.util.Bytes;  // HBase字节转换工具类
// 导入日志相关的类库
import org.slf4j.Logger;  // 日志记录器接口
import org.slf4j.LoggerFactory;  // 日志工厂
// 导入Spring框架注解
import org.springframework.stereotype.Service;  // Spring服务层注解，标记这是一个业务逻辑组件

// 导入Java标准库
import java.io.IOException;  // IO异常类
import java.util.ArrayList;  // 动态数组
import java.util.HashMap;  // 哈希映射
import java.util.List;  // 列表接口
import java.util.Map;  // 映射接口

/**
 * 电影评分查询服务 - 就像一个"数据服务员"，专门负责从HBase中查询和处理电影评分数据
 *
 * 这个服务类的主要职责：
 * 1. 提供各种电影和评分数据的查询功能
 * 2. 封装复杂的HBase操作，为Web层提供简单易用的接口
 * 3. 处理业务逻辑，如数据验证、格式转换、统计计算等
 * 4. 统一异常处理和日志记录
 *
 * 为什么需要服务层？
 * - 分离关注点：将数据访问逻辑与Web控制器分离
 * - 代码复用：多个控制器可以共享同一个服务
 * - 业务封装：将复杂的业务逻辑封装在服务层
 * - 事务管理：统一管理数据库事务和连接
 *
 * 服务层的设计模式：
 * 1. 单一职责：每个方法只负责一个具体的业务功能
 * 2. 依赖注入：通过Spring容器管理对象生命周期
 * 3. 异常转换：将底层异常转换为业务异常
 * 4. 日志记录：记录关键操作和异常信息
 *
 * 查询功能分类：
 * 1. 单条查询：根据ID查询特定电影或用户评分
 * 2. 列表查询：查询某个条件下的多条记录
 * 3. 统计查询：计算各种统计指标
 * 4. 搜索查询：根据关键词模糊搜索
 * 5. 范围查询：根据评分范围筛选电影
 *
 * @author Movie Rating System
 * @version 1.0
 */
@Service  // Spring服务层注解，告诉Spring这是一个业务逻辑组件，需要被容器管理
public class MovieRatingService {
  // 日志记录器，用来记录服务层的操作日志
  private static final Logger logger = LoggerFactory.getLogger(MovieRatingService.class);

  /**
   * 根据电影ID查询电影信息 - 就像"在电影数据库中查找一部特定的电影"
   *
   * 查询流程：
   * 1. 参数验证：检查电影ID是否有效
   * 2. HBase查询：根据行键（电影ID）精确查询
   * 3. 数据解析：将HBase的Result转换为Movie对象
   * 4. 结果返回：返回电影信息或null（如果不存在）
   *
   * 业务规则：
   * - 电影ID不能为空或空字符串
   * - 如果电影不存在，返回null而不是抛出异常
   * - 确保返回的电影信息完整（标题和类型都不为空）
   *
   * 性能特点：
   * - 基于行键查询，性能很高（O(1)时间复杂度）
   * - 单次网络往返，延迟低
   * - 适合高频调用的场景
   *
   * @param movieId 电影ID，作为HBase表的行键
   * @return Movie对象，包含电影的基本信息；如果不存在返回null
   * @throws IllegalArgumentException 如果电影ID为空或无效
   * @throws IOException 如果HBase查询失败
   */
  public Movie getMovieById(String movieId) throws IOException {
    // 参数验证：确保电影ID不为空
    if (movieId == null || movieId.trim().isEmpty()) {
      throw new IllegalArgumentException("电影ID不能为空");
    }

    // 使用HBaseUtil工具类从HBase的MOVIES_TABLE表中根据行键（movieId）获取数据
    // 这是一个精确查询，直接根据行键定位到具体的行
    Result result = HBaseUtil.getData(MovieRatingImporter.MOVIES_TABLE, movieId);

    // 检查查询结果是否为空
    if (result.isEmpty()) {
      logger.debug("未找到电影ID: {}", movieId);
      return null;  // 电影不存在，返回null
    }

    // 从HBase的Result对象中提取title和genres列的值
    // 这些列属于MOVIES_CF_INFO列族，需要指定列族名和列名
    String title = Bytes.toString(result.getValue(
            Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),  // 列族名
            Bytes.toBytes("title")));  // 列名
    String genres = Bytes.toString(result.getValue(
            Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
            Bytes.toBytes("genres")));

    // 创建并返回Movie对象
    logger.debug("查询到电影: ID={}, 标题={}, 类型={}", movieId, title, genres);
    return new Movie(movieId, title, genres);
  }

  /**
   * 根据用户ID查询该用户的所有评分记录 - 就像"查看某个用户的观影历史和评分"
   *
   * 查询策略：
   * 1. 使用HBase的Scan操作遍历评分表
   * 2. 通过SingleColumnValueFilter过滤出指定用户的记录
   * 3. 解析每条记录并转换为Rating对象
   * 4. 返回该用户的所有评分列表
   *
   * 过滤器工作原理：
   * - SingleColumnValueFilter：根据列值进行过滤
   * - 只返回userId列等于指定值的行
   * - setFilterIfMissing(true)：如果列不存在，则过滤掉该行
   *
   * 性能考虑：
   * - 需要扫描整个评分表，性能相对较低
   * - 适合用户评分数量不太多的场景
   * - 可以考虑添加缓存来提高性能
   *
   * @param userId 用户ID
   * @return 该用户的所有评分记录列表
   * @throws IllegalArgumentException 如果用户ID为空或无效
   * @throws IOException 如果HBase查询失败
   */
  public List<Rating> getRatingsByUserId(String userId) throws IOException {
    // 参数验证
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("用户ID不能为空");
    }

    List<Rating> ratings = new ArrayList<>();
    // 获取HBase连接和评分表的操作句柄
    Connection connection = HBaseUtil.getConnection();
    Table table = connection.getTable(TableName.valueOf(MovieRatingImporter.RATINGS_TABLE));

    try {
      // 创建扫描操作，使用rowkey前缀过滤（rowkey格式：userId_movieId）
      Scan scan = new Scan();
      PrefixFilter prefixFilter = new PrefixFilter(Bytes.toBytes(userId + "_"));
      scan.setFilter(prefixFilter);

      ResultScanner scanner = table.getScanner(scan);
      for (Result result : scanner) {
        Rating rating = parseRatingFromResult(result);
        if (rating != null) ratings.add(rating);
      }
      scanner.close();

      // 兜底：如果RowKey前缀过滤未命中，则回退到按列值过滤
      if (ratings.isEmpty()) {
        Scan fallbackScan = new Scan();
        SingleColumnValueFilter filter = new SingleColumnValueFilter(
                Bytes.toBytes(MovieRatingImporter.RATINGS_CF_INFO),
                Bytes.toBytes("userId"),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes(userId));
        filter.setFilterIfMissing(true);
        fallbackScan.setFilter(filter);

        ResultScanner fbScanner = table.getScanner(fallbackScan);
        for (Result result : fbScanner) {
          Rating rating = parseRatingFromResult(result);
          if (rating != null) ratings.add(rating);
        }
        fbScanner.close();
      }

      logger.info("查询到用户 {} 的 {} 条评分记录", userId, ratings.size());
    } finally {
      table.close();  // 关闭表连接，释放资源
    }

    return ratings;
  }

  /**
   * 根据电影ID查询所有评分记录 - 就像"查看某部电影的所有观众评分"
   *
   * 查询逻辑：
   * 1. 扫描整个评分表
   * 2. 使用过滤器筛选出指定电影的评分
   * 3. 收集所有匹配的评分记录
   * 4. 返回该电影的评分列表
   *
   * 应用场景：
   * - 显示电影详情页的用户评分
   * - 计算电影的平均分和评分分布
   * - 分析用户对电影的评价趋势
   *
   * 注意事项：
   * - 热门电影可能有大量评分，查询时间较长
   * - 建议在前端分页显示，避免一次性加载过多数据
   * - 可以考虑按时间排序，优先显示最新评分
   *
   * @param movieId 电影ID
   * @return 该电影的所有评分记录列表
   * @throws IllegalArgumentException 如果电影ID为空或无效
   * @throws IOException 如果HBase查询失败
   */
  public List<Rating> getRatingsByMovieId(String movieId) throws IOException {
    if (movieId == null || movieId.trim().isEmpty()) {
      throw new IllegalArgumentException("电影ID不能为空");
    }

    List<Rating> ratings = new ArrayList<>();
    Connection connection = HBaseUtil.getConnection();
    Table table = connection.getTable(TableName.valueOf(MovieRatingImporter.RATINGS_TABLE));

    try {
      // 1) 优先用RowKey正则过滤，兼容 userId_movieId 和 userId_movieId_timestamp
      Scan scan = new Scan();
      String regex = ".*_" + java.util.regex.Pattern.quote(movieId) + "(?:_.*)?$";
      RowFilter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL, new RegexStringComparator(regex));
      scan.setFilter(rowFilter);

      ResultScanner scanner = table.getScanner(scan);
      for (Result result : scanner) {
        Rating rating = parseRatingFromResult(result);
        if (rating != null) ratings.add(rating);
      }
      scanner.close();

      // 2) 兜底：如果RowKey正则过滤未命中，则回退到按列值过滤
      if (ratings.isEmpty()) {
        Scan fallbackScan = new Scan();
        SingleColumnValueFilter filter = new SingleColumnValueFilter(
                Bytes.toBytes(MovieRatingImporter.RATINGS_CF_INFO),
                Bytes.toBytes("movieId"),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes(movieId));
        filter.setFilterIfMissing(true);
        fallbackScan.setFilter(filter);

        ResultScanner fbScanner = table.getScanner(fallbackScan);
        for (Result result : fbScanner) {
          Rating rating = parseRatingFromResult(result);
          if (rating != null) ratings.add(rating);
        }
        fbScanner.close();
      }

      // 3) 最后兜底：全表扫描，解析rowKey第二段是否等于movieId
      if (ratings.isEmpty()) {
        Scan fullScan = new Scan();
        ResultScanner all = table.getScanner(fullScan);
        for (Result result : all) {
          String rk = Bytes.toString(result.getRow());
          if (rk != null) {
            String[] parts = rk.split("_");
            if (parts.length >= 2 && movieId.equals(parts[1])) {
              Rating rating = parseRatingFromResult(result);
              if (rating != null) ratings.add(rating);
            }
          }
        }
        all.close();
      }

      logger.info("查询到电影 {} 的 {} 条评分记录", movieId, ratings.size());
    } finally {
      table.close();
    }

    return ratings;
  }

  /**
   * 按评分范围筛选电影 - 就像"找出所有高分电影"或"筛选中等评分的电影"
   *
   * 复杂查询流程：
   * 1. 扫描所有评分记录
   * 2. 按电影ID分组收集评分数据
   * 3. 计算每部电影的平均评分
   * 4. 筛选出符合评分范围的电影
   * 5. 查询电影详细信息并组装结果
   *
   * 数据处理逻辑：
   * - 使用Map按电影ID分组：movieId -> List<评分值>
   * - 计算平均分：sum(评分) / count(评分)
   * - 过滤评分范围：minRating <= 平均分 <= maxRating
   * - 补充电影信息：调用getMovieById获取标题和类型
   *
   * 返回数据结构：
   * - movieId：电影ID
   * - title：电影标题
   * - genres：电影类型
   * - avgRating：平均评分（保留2位小数）
   * - ratingCount：评分数量
   *
   * 性能优化建议：
   * - 对于大数据集，考虑使用MapReduce或Spark进行计算
   * - 可以预计算平均分并存储，避免实时计算
   * - 添加缓存机制，减少重复查询
   *
   * @param minRating 最小评分（包含）
   * @param maxRating 最大评分（包含）
   * @return 符合评分范围的电影列表，包含电影信息和统计数据
   * @throws IllegalArgumentException 如果评分范围无效
   * @throws IOException 如果HBase查询失败
   */
  public List<Map<String, Object>> getMoviesByRatingRange(Double minRating, Double maxRating)
          throws IOException {
    // 参数验证
    if (minRating == null || maxRating == null) {
      throw new IllegalArgumentException("评分范围不能为空");
    }
    if (minRating > maxRating) {
      throw new IllegalArgumentException("最小评分不能大于最大评分");
    }

    List<Map<String, Object>> results = new ArrayList<>();
    // 用来按电影ID分组收集评分数据：movieId -> List<评分值>
    Map<String, List<Double>> movieRatingsMap = new HashMap<>();

    Connection connection = HBaseUtil.getConnection();
    Table table = connection.getTable(TableName.valueOf(MovieRatingImporter.RATINGS_TABLE));

    try {
      // 扫描整个评分表
      Scan scan = new Scan();
      ResultScanner scanner = table.getScanner(scan);

      // 遍历所有评分记录，按电影ID分组
      for (Result result : scanner) {
        Rating rating = parseRatingFromResult(result);
        if (rating != null) {
          try {
            // 将评分字符串转换为数值
            double ratingValue = Double.parseDouble(rating.getRating());
            // 检查评分是否在指定范围内
            if (ratingValue >= minRating && ratingValue <= maxRating) {
              // 按电影ID分组，如果该电影ID不存在则创建新的列表
              movieRatingsMap.computeIfAbsent(rating.getMovieId(), k -> new ArrayList<>())
                      .add(ratingValue);
            }
          } catch (NumberFormatException e) {
            // 如果评分格式不正确，记录警告但继续处理其他记录
            logger.warn("评分格式错误: {}", rating.getRating());
          }
        }
      }
      scanner.close();
    } finally {
      table.close();
    }

    // 计算每部电影的平均分并返回结果
    for (Map.Entry<String, List<Double>> entry : movieRatingsMap.entrySet()) {
      String movieId = entry.getKey();
      List<Double> ratingsList = entry.getValue();

      // 计算平均评分
      double sum = 0;
      for (Double r : ratingsList) {
        sum += r;
      }
      double avgRating = sum / ratingsList.size();

      // 获取电影详细信息
      Movie movie = getMovieById(movieId);

      // 组装返回数据
      Map<String, Object> movieData = new HashMap<>();
      movieData.put("movieId", movieId);
      movieData.put("title", movie != null ? movie.getTitle() : "Unknown");
      movieData.put("genres", movie != null ? movie.getGenres() : "Unknown");
      movieData.put("avgRating", String.format("%.2f", avgRating));  // 保留2位小数
      movieData.put("ratingCount", ratingsList.size());

      results.add(movieData);
    }

    logger.info("查询到评分范围 {}-{} 的 {} 部电影", minRating, maxRating, results.size());
    return results;
  }

  /**
   * 根据电影标题搜索电影 - 就像"在电影库中按名字找电影"
   *
   * 搜索策略：
   * 1. 扫描整个电影表（因为HBase不支持二级索引）
   * 2. 对每部电影的标题进行模糊匹配
   * 3. 收集所有匹配的电影记录
   * 4. 返回搜索结果列表
   *
   * 匹配规则：
   * - 不区分大小写的包含匹配
   * - 支持部分关键词搜索
   * - 例如：搜索"toy"可以找到"Toy Story"
   *
   * 性能说明：
   * - 需要全表扫描，性能相对较低
   * - 适合小到中等规模的数据集
   * - 对于大数据集，建议使用Elasticsearch等搜索引擎
   *
   * 优化建议：
   * - 可以添加缓存，缓存热门搜索结果
   * - 可以建立倒排索引来提高搜索性能
   * - 可以限制返回结果数量，避免返回过多数据
   *
   * @param title 搜索关键词（电影标题的一部分）
   * @return 匹配的电影列表
   * @throws IllegalArgumentException 如果搜索关键词为空
   * @throws IOException 如果HBase查询失败
   */
  public List<Movie> searchMoviesByTitle(String title) throws IOException {
    // 参数验证
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("搜索关键词不能为空");
    }

    List<Movie> movies = new ArrayList<>();
    Connection connection = HBaseUtil.getConnection();
    Table table = connection.getTable(TableName.valueOf(MovieRatingImporter.MOVIES_TABLE));

    try {
      // 扫描整个电影表
      Scan scan = new Scan();
      //整一个结果对象
      ResultScanner scanner = table.getScanner(scan);

      // 将搜索关键词转换为小写，实现不区分大小写的搜索
      String searchTitle = title.toLowerCase();

      for (Result result : scanner) {
        // 解析电影记录
        Movie movie = parseMovieFromResult(result);
        if (movie != null && movie.getTitle() != null) {
          // 检查电影标题是否包含搜索关键词（不区分大小写）
          if (movie.getTitle().toLowerCase().contains(searchTitle)) {
            movies.add(movie);
          }
        }
      }
      scanner.close();

      logger.info("搜索关键词 '{}' 找到 {} 部电影", title, movies.size());
    } finally {
      table.close();
    }

    return movies;
  }

  /**
   * 根据电影标题精确查询电影 - 就像"按电影名直接定位"
   *
   * 查询策略：
   * 1. 扫描整个电影表（HBase无二级索引）
   * 2. 对每部电影的标题做规范化后与输入标题做精确匹配
   * 3. 找到第一个精确匹配则返回；如果没有则返回null
   *
   * 规范化规则：
   * - 去除前后空格
   * - 合并连续空白为单个空格
   * - 不区分大小写
   *
   * @param title 电影标题（完整名称，可包含年份）
   * @return 精确匹配的Movie；未找到返回null
   * @throws IllegalArgumentException 如果标题为空
   * @throws IOException 如果HBase查询失败
   */
  public Movie getMovieByExactTitle(String title) throws IOException {
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("电影名不能为空");
    }

    Connection connection = HBaseUtil.getConnection();
    Table table = connection.getTable(TableName.valueOf(MovieRatingImporter.MOVIES_TABLE));

    try {
      Scan scan = new Scan();
      ResultScanner scanner = table.getScanner(scan);

      String target = normalizeTitle(title);

      for (Result result : scanner) {
        Movie movie = parseMovieFromResult(result);
        if (movie != null && movie.getTitle() != null) {
          if (normalizeTitle(movie.getTitle()).equals(target)) {
            scanner.close();
            logger.info("精确匹配到电影: {}", movie.getTitle());
            return movie;
          }
        }
      }
      scanner.close();

      logger.info("未找到精确匹配的电影标题: {}", title);
      return null;
    } finally {
      table.close();
    }
  }

  // 标题规范化（去掉多余空白并忽略大小写）
  private String normalizeTitle(String s) {
    return s == null ? "" : s.replaceAll("\\s+", " ").trim().toLowerCase();
  }

  /**
   * 获取数据统计信息 - 就像"查看电影数据库的整体情况"
   *
   * 统计内容：
   * 1. 电影总数：数据库中有多少部电影
   * 2. 评分总数：总共有多少条评分记录
   * 3. 平均每部电影的评分数：评分密度指标
   *
   * 统计用途：
   * - 系统监控：了解数据库的数据量
   * - 业务分析：分析用户活跃度和电影热度
   * - 容量规划：为系统扩容提供数据支持
   * - 数据质量：检查数据的完整性和一致性
   *
   * 计算逻辑：
   * - 使用HBase的行数统计功能
   * - 计算平均值时要处理除零情况
   * - 返回格式化的统计结果
   *
   * @return 包含各种统计指标的Map
   * @throws IOException 如果HBase查询失败
   */
  public Map<String, Object> getStatistics() throws IOException {
    Map<String, Object> stats = new HashMap<>();

    // 统计电影表的行数
    long movieCount = HBaseUtil.countRows(MovieRatingImporter.MOVIES_TABLE);
    // 统计评分表的行数
    long ratingCount = HBaseUtil.countRows(MovieRatingImporter.RATINGS_TABLE);

    // 组装统计结果
    stats.put("movieCount", movieCount);
    stats.put("ratingCount", ratingCount);
    // 计算平均每部电影的评分数，避免除零错误
    stats.put("avgRatingsPerMovie", movieCount > 0 ? (double) ratingCount / movieCount : 0);

    logger.info("统计信息: 电影数={}, 评分数={}", movieCount, ratingCount);
    return stats;
  }

  /**
   * 模糊按电影标题搜索，并返回每部电影的评分列表与统计 - 就像"先找到电影，再一并带上它的评分"
   *
   * 返回数据结构（每个元素）：
   * - movieId：电影ID
   * - title：电影标题
   * - genres：类型
   * - ratingCount：评分数量
   * - avgRating：平均评分（保留2位小数）
   * - ratings：评分列表（Rating对象集合）
   *
   * @param keyword 标题关键词（模糊匹配）
   * @return 包含电影及其评分的列表
   * @throws IllegalArgumentException 如果关键词为空
   * @throws IOException HBase访问异常
   */
  public List<Map<String, Object>> searchRatingsByMovieTitle(String keyword) throws IOException {
    if (keyword == null || keyword.trim().isEmpty()) {
      throw new IllegalArgumentException("搜索关键词不能为空");
    }

    List<Map<String, Object>> results = new ArrayList<>();

    // 先模糊搜索电影
    List<Movie> movies = searchMoviesByTitle(keyword);
    for (Movie movie : movies) {
      // 查询该电影的所有评分
      List<Rating> ratings = getRatingsByMovieId(movie.getMovieId());

      // 统计平均分（仅统计有效评分值）
      double sum = 0;
      int valid = 0;
      for (Rating r : ratings) {
        if (r != null && r.getRating() != null) {
          try {
            sum += Double.parseDouble(r.getRating());
            valid++;
          } catch (NumberFormatException ignored) {}
        }
      }
      String avgRating = valid > 0 ? String.format("%.2f", sum / valid) : "N/A";

      Map<String, Object> item = new HashMap<>();
      item.put("movieId", movie.getMovieId());
      item.put("title", movie.getTitle());
      item.put("genres", movie.getGenres());
      item.put("ratingCount", ratings.size());
      item.put("avgRating", avgRating);
      item.put("ratings", ratings);
      results.add(item);
    }

    logger.info("标题关键词 '{}' 找到 {} 部电影及评分", keyword, results.size());
    return results;
  }

  /**
   * 解析HBase查询结果为Rating对象 - 就像"把数据库记录转换为Java对象"
   *
   * 解析流程：
   * 1. 检查Result对象是否有效
   * 2. 提取行键（rowKey）
   * 3. 从指定列族和列中提取各个字段值
   * 4. 将字节数组转换为字符串
   * 5. 创建并返回Rating对象
   *
   * 错误处理：
   * - 如果Result为空或null，返回null
   * - 如果解析过程中出现异常，记录错误并返回null
   * - 确保程序的健壮性，不因单条记录错误而崩溃
   *
   * 数据映射：
   * - rowKey：HBase的行键，通常是userId_movieId格式
   * - userId：用户ID，存储在RATINGS_CF_INFO:userId列
   * - movieId：电影ID，存储在RATINGS_CF_INFO:movieId列
   * - rating：评分值，存储在RATINGS_CF_INFO:rating列
   * - timestamp：时间戳，存储在RATINGS_CF_INFO:timestamp列
   *
   * @param result HBase查询结果对象
   * @return Rating对象，如果解析失败返回null
   */
  private Rating parseRatingFromResult(Result result) {
    // 检查输入参数的有效性
    if (result == null || result.isEmpty()) {
      return null;
    }

    try {
      // 提取行键
      String rowKey = Bytes.toString(result.getRow());

      // 先按预期列族和列名读取
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

      // 如果存在任何空值，遍历cells进行兜底解析（防止列族/列名不一致导致的null）
      if (userId == null || movieId == null || rating == null || timestamp == null) {
        try {
          java.util.List<org.apache.hadoop.hbase.Cell> cells = result.listCells();
          if (cells != null) {
            for (org.apache.hadoop.hbase.Cell cell : cells) {
              String cf = Bytes.toString(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength());
              String q = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
              String v = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
              // 只解析目标列族，或在未知列族时也尝试读取（以防创建表时列族名不同）
              if (cf != null) {
                if ("userId".equals(q) && userId == null) userId = v;
                else if ("movieId".equals(q) && movieId == null) movieId = v;
                else if (("rating".equals(q) || "score".equalsIgnoreCase(q)) && rating == null) rating = v;
                else if (("timestamp".equals(q) || "time".equalsIgnoreCase(q)) && timestamp == null) timestamp = v;
              }
            }
          }
        } catch (Exception ignored) {}

        // 仍为空则从rowKey推断userId和movieId（格式通常为userId_movieId或userId_movieId_timestamp）
        if (rowKey != null) {
          String[] parts = rowKey.split("_");
          if (parts.length >= 2) {
            if (userId == null) userId = parts[0];
            if (movieId == null) movieId = parts[1];
            if (parts.length >= 3 && (timestamp == null || timestamp.isEmpty())) {
              timestamp = parts[2];
            }
          }
        }
      }

      // 创建并返回Rating对象
      return new Rating(rowKey, userId, movieId, rating, timestamp);
    } catch (Exception e) {
      // 如果解析过程中出现任何异常，记录错误并返回null
      logger.error("解析评分记录失败", e);
      return null;
    }
  }

  /**
   * 解析HBase查询结果为Movie对象 - 就像"把数据库记录转换为电影对象"
   *
   * 解析流程：
   * 1. 验证Result对象的有效性
   * 2. 提取行键作为电影ID
   * 3. 从MOVIES_CF_INFO列族中提取电影信息
   * 4. 创建Movie对象并返回
   *
   * 数据结构：
   * - movieId：电影ID，来自HBase的行键
   * - title：电影标题，存储在MOVIES_CF_INFO:title列
   * - genres：电影类型，存储在MOVIES_CF_INFO:genres列
   *
   * 异常处理：
   * - 采用防御性编程，确保不会因为单条记录问题影响整体功能
   * - 记录详细的错误日志，便于问题排查
   * - 返回null而不是抛出异常，让调用方决定如何处理
   *
   * @param result HBase查询结果对象
   * @return Movie对象，如果解析失败返回null
   */
  private Movie parseMovieFromResult(Result result) {
    if (result == null || result.isEmpty()) {
      return null;
    }

    try {
      // 提取电影ID（行键）
      String movieId = Bytes.toString(result.getRow());

      // 提取电影信息
      String title = Bytes.toString(result.getValue(
              Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
              Bytes.toBytes("title")));
      String genres = Bytes.toString(result.getValue(
              Bytes.toBytes(MovieRatingImporter.MOVIES_CF_INFO),
              Bytes.toBytes("genres")));

      return new Movie(movieId, title, genres);
    } catch (Exception e) {
      logger.error("解析电影记录失败", e);
      return null;
    }
  }
}
