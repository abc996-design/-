package com.movie.web.entity;

// Lombok注解导入 - 用于自动生成常用方法，减少样板代码
import lombok.AllArgsConstructor;  // 生成包含所有字段的构造函数
import lombok.Data;               // 生成getter、setter、toString、equals、hashCode方法
import lombok.NoArgsConstructor;   // 生成无参构造函数

/**
 * 电影实体类 - 电影数据的Java对象表示
 * 
 * 这个类是整个电影评分系统的核心数据模型之一，代表了一部电影的基本信息。
 * 它承担着以下重要职责：
 * 
 * 1. 数据载体：封装电影的基本属性（ID、标题、类型）
 * 2. 数据传输：在各个层之间传递电影信息
 * 3. JSON序列化：支持与前端的数据交换
 * 4. HBase映射：与HBase中的电影表数据对应
 * 
 * 设计特点：
 * - 使用Lombok减少样板代码，提高开发效率
 * - 字段设计简洁明了，符合电影数据的基本需求
 * - 支持JSON序列化，便于REST API数据传输
 * - 与HBase存储结构保持一致
 * 
 * 数据来源：
 * - MovieLens数据集中的movies.csv文件
 * - HBase中的movies表
 * - 用户通过API提交的电影信息
 * 
 * 使用场景：
 * - 电影信息查询和展示
 * - 电影数据的增删改操作
 * - 电影推荐系统的基础数据
 * - 统计分析中的电影维度数据
 */
@Data  // 自动生成getter、setter、toString、equals、hashCode方法
       // 这大大减少了样板代码，让类更加简洁
@NoArgsConstructor   // 生成无参构造函数，用于：
                     // 1. Spring框架的Bean实例化
                     // 2. JSON反序列化
                     // 3. JPA等ORM框架的实体创建
@AllArgsConstructor  // 生成包含所有字段的构造函数，用于：
                     // 1. 快速创建完整的Movie对象
                     // 2. 测试数据的构造
                     // 3. 数据转换和映射
public class Movie {
  
  /**
   * 电影ID - 电影的唯一标识符
   * 
   * 这是电影在系统中的主键，具有以下特点：
   * - 全局唯一性：每部电影都有唯一的ID
   * - 数据关联：用于与评分数据建立关联关系
   * - 查询索引：作为HBase的RowKey，查询效率极高
   * - 数据来源：来自MovieLens数据集的movieId字段
   * 
   * 使用场景：
   * - 根据ID查询电影详情
   * - 电影评分数据的关联查询
   * - 电影推荐算法中的电影标识
   * - 缓存系统中的键值
   * 
   * 注意事项：
   * - 使用String类型以保持与原始数据的一致性
   * - 在HBase中作为RowKey使用，影响数据分布
   * - 不应该暴露给最终用户，仅用于系统内部
   */
  private String movieId;
  
  /**
   * 电影标题 - 电影的名称和上映年份
   * 
   * 这是电影最重要的展示信息，包含以下内容：
   * - 电影名称：电影的正式标题
   * - 上映年份：通常以(YYYY)格式附在标题后
   * - 多语言支持：主要以英文为主，部分包含原语言标题
   * 
   * 数据格式示例：
   * - "Toy Story (1995)"
   * - "Jumanji (1995)"
   * - "Grumpier Old Men (1995)"
   * 
   * 使用场景：
   * - 电影列表的展示
   * - 搜索功能的匹配字段
   * - 用户界面的主要显示内容
   * - 电影推荐结果的展示
   * 
   * 注意事项：
   * - 可能包含特殊字符，需要考虑编码问题
   * - 长度不固定，UI设计时需要考虑自适应
   * - 搜索时可能需要模糊匹配功能
   */
  private String title;
  
  /**
   * 电影类型 - 电影的分类标签
   * 
   * 这个字段描述了电影所属的类型或风格，具有以下特点：
   * - 多类型支持：一部电影可以属于多个类型
   * - 分隔符：多个类型用"|"符号分隔
   * - 标准化：使用预定义的类型词汇
   * 
   * 常见类型包括：
   * - Action（动作）
   * - Adventure（冒险）
   * - Animation（动画）
   * - Children（儿童）
   * - Comedy（喜剧）
   * - Crime（犯罪）
   * - Documentary（纪录片）
   * - Drama（剧情）
   * - Fantasy（奇幻）
   * - Horror（恐怖）
   * - Musical（音乐剧）
   * - Mystery（悬疑）
   * - Romance（爱情）
   * - Sci-Fi（科幻）
   * - Thriller（惊悚）
   * - War（战争）
   * - Western（西部）
   * 
   * 数据格式示例：
   * - "Adventure|Animation|Children|Comedy|Fantasy"
   * - "Comedy|Romance"
   * - "Action|Crime|Thriller"
   * 
   * 使用场景：
   * - 电影分类和筛选
   * - 推荐算法的特征维度
   * - 用户偏好分析
   * - 统计报表中的分类依据
   * 
   * 注意事项：
   * - 需要解析"|"分隔的多个类型
   * - 类型名称需要国际化支持
   * - 可能需要类型映射和标准化处理
   */
  private String genres;
}
