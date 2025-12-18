package com.movie.web.controller;

// 导入Web层的实体类
import com.movie.web.entity.ApiResponse;  // 统一API响应格式类，封装返回结果
import com.movie.web.entity.Movie;  // 电影实体类
import com.movie.web.entity.Rating;  // 评分实体类
// 导入业务逻辑层服务
import com.movie.web.service.MovieRatingService;  // 电影评分业务服务类
// 导入日志相关类
import org.slf4j.Logger;  // 日志记录器接口
import org.slf4j.LoggerFactory;  // 日志工厂
// 导入Spring Web相关注解和类
import org.springframework.beans.factory.annotation.Autowired;  // 自动装配注解
import org.springframework.web.bind.annotation.*;  // Spring Web注解集合

// 导入Java标准库
import java.util.List;  // 列表接口
import java.util.Map;  // 映射接口

/**
 * 电影评分查询REST API控制器 - 就像一个"服务台接待员"，专门处理客户端的HTTP请求
 *
 * 控制器的主要职责：
 * 1. 接收HTTP请求：处理来自前端或其他客户端的API调用
 * 2. 参数验证和转换：将HTTP参数转换为Java对象
 * 3. 调用业务逻辑：委托给Service层处理具体的业务逻辑
 * 4. 响应格式化：将处理结果封装成统一的JSON响应格式
 * 5. 异常处理：捕获并转换异常为用户友好的错误信息
 *
 * RESTful API设计原则：
 * 1. 资源导向：URL表示资源，HTTP方法表示操作
 * 2. 无状态：每个请求都包含完整的信息，服务器不保存客户端状态
 * 3. 统一接口：使用标准的HTTP方法（GET、POST、PUT、DELETE）
 * 4. 分层系统：客户端不需要知道服务器内部实现细节
 *
 * API路径设计规范：
 * - /api/movies/{id}：操作特定电影资源
 * - /api/ratings/user/{userId}：获取用户相关的评分资源
 * - /api/ratings/movie/{movieId}：获取电影相关的评分资源
 * - /api/movies/search：搜索电影资源
 * - /api/statistics：获取统计信息
 *
 * HTTP状态码使用：
 * - 200：请求成功
 * - 400：客户端请求错误（参数无效等）
 * - 404：资源未找到
 * - 500：服务器内部错误
 *
 * 响应格式统一：
 * - 成功：{"success": true, "message": "操作成功", "data": {...}}
 * - 失败：{"success": false, "code": 404, "message": "错误信息"}
 *
 * @author Movie Rating System
 * @version 1.0
 */
@RestController   // Spring REST控制器注解，自动将返回值转换为JSON格式，相当于@Controller + @ResponseBody
@RequestMapping("/api")  // 基础路径映射，所有接口都以/api开头
public class MovieRatingController {
  // 日志记录器，用来记录API调用和错误信息
  private static final Logger logger = LoggerFactory.getLogger(MovieRatingController.class);

  // 自动注入业务逻辑层服务，Spring会自动创建MovieRatingService实例并注入
  @Autowired   // 依赖注入注解，告诉Spring自动装配MovieRatingService bean
  private MovieRatingService movieRatingService;

  /**
   * 根据电影ID查询电影信息 - 就像"根据电影票上的编号查找电影详情"
   *
   * API设计说明：
   * - HTTP方法：GET（查询操作）
   * - URL模式：/api/movies/{movieId}
   * - 路径参数：movieId（电影ID）
   * - 返回格式：ApiResponse<Movie>
   *
   * 业务流程：
   * 1. 接收HTTP GET请求
   * 2. 从URL路径中提取movieId参数
   * 3. 调用业务服务查询电影信息
   * 4. 根据查询结果返回相应的响应
   *
   * 响应场景：
   * - 成功找到：返回200状态码和电影信息
   * - 电影不存在：返回404状态码和错误信息
   * - 参数无效：返回400状态码和参数错误信息
   * - 系统异常：返回500状态码和系统错误信息
   *
   * 使用示例：
   * GET /api/movies/1
   * GET /api/movies/toy_story
   *
   * @param movieId 电影ID，从URL路径中获取
   * @return ApiResponse<Movie> 包含电影信息的统一响应格式
   */
  @GetMapping("/movies/{movieId}")  // GET请求映射，{movieId}是路径变量
  public ApiResponse<Movie> getMovieById(@PathVariable String movieId) {
    try {
      // 记录API调用日志，便于监控和调试
      logger.info("查询电影ID: {}", movieId);

      // 调用业务服务层查询电影信息
      Movie movie = movieRatingService.getMovieById(movieId);

      // 检查查询结果，如果电影不存在则返回404错误
      if (movie == null) {
        return ApiResponse.error(404, "未找到电影ID: " + movieId);
      }

      // 查询成功，返回电影信息
      return ApiResponse.success(movie);
    } catch (IllegalArgumentException e) {
      // 捕获参数验证异常，返回400错误
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      // 捕获其他所有异常，返回500错误
      logger.error("查询电影失败", e);
      return ApiResponse.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 根据用户ID查询该用户的所有评分记录 - 就像"查看某个用户的观影历史和评分记录"
   *
   * API设计说明：
   * - HTTP方法：GET（查询操作）
   * - URL模式：/api/ratings/user/{userId}
   * - 路径参数：userId（用户ID）
   * - 返回格式：ApiResponse<List<Rating>>
   *
   * 业务场景：
   * - 用户个人中心：显示用户的观影历史
   * - 推荐系统：基于用户历史评分进行推荐
   * - 数据分析：分析用户的观影偏好
   *
   * 性能考虑：
   * - 对于活跃用户，评分记录可能很多，建议前端分页显示
   * - 可以考虑添加缓存来提高响应速度
   * - 可以按时间倒序排列，优先显示最新评分
   *
   * 使用示例：
   * GET /api/ratings/user/123
   * GET /api/ratings/user/john_doe
   *
   * @param userId 用户ID，从URL路径中获取
   * @return ApiResponse<List<Rating>> 包含用户评分列表的统一响应格式
   */
  @GetMapping("/ratings/user/{userId}")
  public ApiResponse<List<Rating>> getRatingsByUserId(@PathVariable String userId) {
    try {
      logger.info("查询用户ID: {} 的评分记录", userId);

      // 调用业务服务查询用户的所有评分记录
      List<Rating> ratings = movieRatingService.getRatingsByUserId(userId);

      // 检查是否找到评分记录
      if (ratings.isEmpty()) {
        return ApiResponse.error(404, "用户 " + userId + " 暂无评分记录");
      }

      // 返回成功结果，包含评分数量信息
      return ApiResponse.success("查询到 " + ratings.size() + " 条评分记录", ratings);
    } catch (IllegalArgumentException e) {
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      logger.error("查询评分记录失败", e);
      return ApiResponse.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 根据电影ID查询该电影的所有评分记录 - 就像"查看某部电影的所有观众评价"
   *
   * API设计说明：
   * - HTTP方法：GET（查询操作）
   * - URL模式：/api/ratings/movie/{movieId}
   * - 路径参数：movieId（电影ID）
   * - 返回格式：ApiResponse<List<Rating>>
   *
   * 业务场景：
   * - 电影详情页：显示用户对该电影的评分和评论
   * - 评分统计：计算电影的平均分和评分分布
   * - 社交功能：让用户看到其他人对电影的评价
   *
   * 数据处理建议：
   * - 前端可以根据评分进行排序（高分优先或低分优先）
   * - 可以按评分时间排序（最新评分优先）
   * - 可以过滤显示有评论的评分记录
   *
   * 使用示例：
   * GET /api/ratings/movie/1
   *
   * @param movieId 电影ID，从URL路径中获取
   * @return ApiResponse<List<Rating>> 包含电影评分列表的统一响应格式
   */
  @GetMapping("/ratings/movie/{movieId}")
  public ApiResponse<List<Rating>> getRatingsByMovieId(@PathVariable String movieId) {
    try {
      logger.info("查询电影ID: {} 的评分记录", movieId);

      // 调用业务服务查询电影的所有评分记录
      List<Rating> ratings = movieRatingService.getRatingsByMovieId(movieId);

      // 检查是否找到评分记录
      if (ratings.isEmpty()) {
        return ApiResponse.error(404, "电影 " + movieId + " 暂无评分记录");
      }

      // 返回成功结果，包含评分数量信息
      return ApiResponse.success("查询到 " + ratings.size() + " 条评分记录", ratings);
    } catch (IllegalArgumentException e) {
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      logger.error("查询评分记录失败", e);
      return ApiResponse.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 按评分范围筛选电影 - 就像"找出所有高分电影"或"筛选中等评分的电影"
   *
   * API设计说明：
   * - HTTP方法：GET（查询操作）
   * - URL模式：/api/movies/rating-range?min=3.0&max=5.0
   * - 查询参数：min（最小评分，默认0.0），max（最大评分，默认5.0）
   * - 返回格式：ApiResponse<List<Map<String, Object>>>
   *
   * 参数说明：
   * - min：最小评分（包含），可选参数，默认0.0
   * - max：最大评分（包含），可选参数，默认5.0
   * - 评分范围：[min, max]，闭区间
   *
   * 返回数据结构：
   * - movieId：电影ID
   * - title：电影标题
   * - genres：电影类型
   * - avgRating：平均评分（保留2位小数）
   * - ratingCount：评分数量
   *
   * 业务场景：
   * - 电影推荐：推荐高分电影给用户
   * - 分类浏览：按评分等级浏览电影
   * - 数据分析：分析不同评分区间的电影分布
   *
   * 使用示例：
   * GET /api/movies/rating-range?min=4.0&max=5.0  // 查询高分电影
   * GET /api/movies/rating-range?min=3.0&max=4.0  // 查询中等评分电影
   * GET /api/movies/rating-range                  // 查询所有电影（使用默认范围）
   *
   * @param min 最小评分，可选参数，默认0.0
   * @param max 最大评分，可选参数，默认5.0
   * @return ApiResponse<List<Map<String, Object>>> 包含电影信息和统计数据的响应
   */
  @GetMapping("/movies/rating-range")
  public ApiResponse<List<Map<String, Object>>> getMoviesByRatingRange(
          @RequestParam(required = false, defaultValue = "0.0") Double min,  // 可选参数，有默认值
          @RequestParam(required = false, defaultValue = "5.0") Double max) {
    try {
      logger.info("查询评分范围: {} - {}", min, max);

      // 调用业务服务按评分范围筛选电影
      List<Map<String, Object>> movies = movieRatingService.getMoviesByRatingRange(min, max);

      // 检查是否找到符合条件的电影
      if (movies.isEmpty()) {
        return ApiResponse.error(404, "未找到评分范围 " + min + "-" + max + " 的电影");
      }

      // 返回成功结果，包含电影数量信息
      return ApiResponse.success("查询到 " + movies.size() + " 部电影", movies);
    } catch (IllegalArgumentException e) {
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      logger.error("查询电影失败", e);
      return ApiResponse.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 按电影名模糊搜索并返回每部电影的评分列表与统计
   *
   * API：GET /api/ratings/search-by-title?keyword=toy
   * 返回：每个匹配电影包含 movieId/title/genres/ratingCount/avgRating/ratings[]
   */
  @GetMapping("/ratings/search-by-title")
  public ApiResponse<List<Map<String, Object>>> searchRatingsByTitle(@RequestParam String keyword) {
    try {
      logger.info("按标题模糊查询评分，关键词: {}", keyword);

      List<Map<String, Object>> list = movieRatingService.searchRatingsByMovieTitle(keyword);
      if (list.isEmpty()) {
        return ApiResponse.error(404, "未找到包含 '" + keyword + "' 的电影");
      }
      return ApiResponse.success("查询到 " + list.size() + " 部电影及评分", list);
    } catch (IllegalArgumentException e) {
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      logger.error("按标题查询评分失败", e);
      return ApiResponse.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 根据电影标题精确查询电影 - 直接返回唯一结果
   *
   * API设计说明：
   * - HTTP方法：GET
   * - URL模式：/api/movies/by-title?title=Toy%20Story%20(1995)
   * - 查询参数：title（电影完整标题，必填）
   * - 返回格式：ApiResponse<Movie>
   *
   * 匹配规则：
   * - 标题规范化（合并空白、忽略大小写）后做精确匹配
   *
   * @param title 电影完整标题
   * @return ApiResponse<Movie>
   */
  @GetMapping("/movies/by-title")
  public ApiResponse<Movie> getMovieByTitle(@RequestParam String title) {
    try {
      logger.info("按标题精确查询电影: {}", title);

      Movie movie = movieRatingService.getMovieByExactTitle(title);
      if (movie == null) {
        return ApiResponse.error(404, "未找到电影标题: " + title);
      }
      return ApiResponse.success(movie);
    } catch (IllegalArgumentException e) {
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      logger.error("按标题查询电影失败", e);
      return ApiResponse.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 搜索电影（按标题模糊匹配） - 就像"在电影库中按名字搜索电影"
   *
   * API设计说明：
   * - HTTP方法：GET（查询操作）
   * - URL模式：/api/movies/search?keyword=toy
   * - 查询参数：keyword（搜索关键词，必需参数）
   * - 返回格式：ApiResponse<List<Movie>>
   *
   * 搜索特性：
   * - 模糊匹配：支持部分关键词搜索
   * - 不区分大小写：搜索"toy"可以找到"Toy Story"
   * - 包含匹配：关键词可以出现在标题的任何位置
   *
   * 搜索优化建议：
   * - 前端可以实现搜索建议功能
   * - 可以按相关度排序搜索结果
   * - 可以限制返回结果数量，避免返回过多数据
   * - 可以添加搜索历史记录功能
   *
   * 业务场景：
   * - 用户主动搜索：用户输入电影名称进行搜索
   * - 自动完成：输入过程中提供搜索建议
   * - 相关推荐：基于搜索关键词推荐相关电影
   *
   * 使用示例：
   * GET /api/movies/search?keyword=toy        // 搜索包含"toy"的电影
   * GET /api/movies/search?keyword=star wars  // 搜索包含"star wars"的电影
   * GET /api/movies/search?keyword=action     // 搜索包含"action"的电影
   *
   * @param keyword 搜索关键词，必需参数
   * @return ApiResponse<List<Movie>> 包含匹配电影列表的统一响应格式
   */
  @GetMapping("/movies/search")
  public ApiResponse<List<Movie>> searchMovies(@RequestParam String keyword) {
    try {
      logger.info("搜索电影关键词: {}", keyword);

      // 调用业务服务进行电影标题搜索
      List<Movie> movies = movieRatingService.searchMoviesByTitle(keyword);

      // 检查是否找到匹配的电影
      if (movies.isEmpty()) {
        return ApiResponse.error(404, "未找到包含 '" + keyword + "' 的电影");
      }

      // 返回成功结果，包含找到的电影数量信息
      return ApiResponse.success("查询到 " + movies.size() + " 部电影", movies);
    } catch (IllegalArgumentException e) {
      logger.warn("参数错误: {}", e.getMessage());
      return ApiResponse.error(400, e.getMessage());
    } catch (Exception e) {
      logger.error("搜索电影失败", e);
      return ApiResponse.error("搜索失败: " + e.getMessage());
    }
  }

  /**
   * 获取系统统计信息 - 就像"查看电影数据库的整体运营情况"
   *
   * API设计说明：
   * - HTTP方法：GET（查询操作）
   * - URL模式：/api/statistics
   * - 无参数：不需要任何参数
   * - 返回格式：ApiResponse<Map<String, Object>>
   *
   * 统计指标：
   * - movieCount：电影总数
   * - ratingCount：评分总数
   * - avgRatingsPerMovie：平均每部电影的评分数
   *
   * 业务用途：
   * - 系统监控：了解数据库的数据规模
   * - 运营分析：分析用户活跃度和内容丰富度
   * - 管理后台：为管理员提供系统概览
   * - 数据报表：生成系统运营报告
   *
   * 扩展可能：
   * - 可以添加更多统计维度（如用户数、热门电影等）
   * - 可以支持时间范围统计（如本月新增评分数）
   * - 可以添加缓存来提高响应速度
   *
   * 使用示例：
   * GET /api/statistics
   *
   * 响应示例：
   * {
   *   "success": true,
   *   "data": {
   *     "movieCount": 1000,
   *     "ratingCount": 5000,
   *     "avgRatingsPerMovie": 5.0
   *   }
   * }
   *
   * @return ApiResponse<Map<String, Object>> 包含统计信息的统一响应格式
   */
  @GetMapping("/statistics")
  public ApiResponse<Map<String, Object>> getStatistics() {
    try {
      logger.info("获取统计信息");

      // 调用业务服务获取系统统计信息
      Map<String, Object> stats = movieRatingService.getStatistics();

      // 返回统计结果
      return ApiResponse.success(stats);
    } catch (Exception e) {
      // 统计信息获取失败，记录错误并返回500错误
      logger.error("获取统计信息失败", e);
      return ApiResponse.error("获取统计信息失败: " + e.getMessage());
    }
  }
}
