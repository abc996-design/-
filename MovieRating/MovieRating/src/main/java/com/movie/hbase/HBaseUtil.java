package com.movie.hbase;

// 导入HBase相关的类库，这些都是操作HBase数据库必需的
import org.apache.hadoop.conf.Configuration;  // 配置类，用来读取HBase的配置信息
import org.apache.hadoop.hbase.*;  // HBase的核心类，包含表名、列族等基本概念
import org.apache.hadoop.hbase.client.*;  // HBase客户端操作类，用来连接和操作HBase
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;  // 过滤器，只获取每行的第一个键，用于快速统计行数
import org.apache.hadoop.hbase.util.Bytes;  // 字节转换工具，HBase内部都是用字节存储数据
import org.slf4j.Logger;  // 日志记录器接口
import org.slf4j.LoggerFactory;  // 日志工厂，用来创建日志记录器

import java.io.IOException;  // IO异常类，文件读写出错时会抛出
import java.util.ArrayList;  // 动态数组，用来存储查询结果
import java.util.List;  // 列表接口

/**
 * HBase工具类 - 就像一个"万能工具箱"，把所有常用的HBase操作都封装在这里
 * 
 * 为什么要写这个工具类？
 * 1. HBase的原生API比较复杂，每次操作都要写很多重复代码
 * 2. 连接管理很重要，不能每次操作都创建新连接（太浪费资源）
 * 3. 把常用操作封装起来，其他地方调用就很简单了
 * 
 * 这个类使用了"单例模式"：
 * - 整个程序只有一个HBase连接实例
 * - 所有操作都共享这个连接，提高效率
 * - 线程安全，多个线程同时使用也不会出问题
 *
 * @author Movie Rating System
 * @version 1.0
 */
public class HBaseUtil {

  // 日志记录器，用来记录程序运行过程中的信息（成功、失败、警告等）
  private static final Logger logger = LoggerFactory.getLogger(HBaseUtil.class);
  
  // HBase连接对象，volatile关键字保证多线程环境下的可见性
  // 简单理解：当一个线程修改了这个变量，其他线程能立即看到修改后的值
  private static volatile Connection connection;

  // 私有构造函数，防止外部直接new这个类的实例
  // 因为我们要用单例模式，只能有一个实例
  private HBaseUtil() {
  }

  // 静态代码块，在类第一次被加载时执行（只执行一次）
  // 用来初始化HBase连接
  static {
    initConnection();
  }

  /**
   * 初始化HBase连接 - 就像"拨号上网"，建立与HBase数据库的连接
   * 
   * 这个方法做了什么？
   * 1. 创建HBase配置对象，告诉程序HBase服务器在哪里
   * 2. 读取hbase-site.xml配置文件，获取连接参数（服务器地址、端口等）
   * 3. 建立实际的网络连接
   * 4. 如果连接失败，程序就停止运行（因为没有数据库就没法工作）
   */
  private static void initConnection() {
    try {
      // 创建HBase配置对象，这就像填写"上网设置"
      Configuration conf = HBaseConfiguration.create();
      
      // 加载配置文件，告诉程序HBase服务器的具体信息
      // hbase-site.xml文件里包含了HBase集群的地址、端口等重要信息
      conf.addResource("hbase-site.xml");
      
      // 真正建立连接，就像拨号连接到互联网
      connection = ConnectionFactory.createConnection(conf);
      
      // 记录成功信息，✓表示成功
      logger.info("✓ HBase连接初始化成功");
    } catch (IOException e) {
      // 如果连接失败，记录错误信息，✗表示失败
      logger.error("✗ HBase连接初始化失败", e);
      
      // 抛出运行时异常，让程序停止运行
      // 因为没有数据库连接，程序就无法正常工作
      throw new RuntimeException("HBase连接失败", e);
    }
  }

  /**
   * 获取HBase连接实例 - 就像"获取电话线"，给其他方法提供数据库连接
   * 
   * 为什么要这样设计？
   * 1. 单例模式：整个程序只用一个连接，节省资源
   * 2. 懒加载：只有真正需要时才创建连接
   * 3. 线程安全：多个线程同时调用也不会出问题
   * 4. 自动重连：如果连接断了，会自动重新连接
   *
   * @return HBase连接对象，其他方法用这个连接来操作数据库
   */
  public static Connection getConnection() {
    // 双重检查锁定模式，确保线程安全且性能最优
    // 第一次检查：如果连接不存在或已关闭，才进入同步块
    if (connection == null || connection.isClosed()) {
      // synchronized确保同一时间只有一个线程能执行这段代码
      synchronized (HBaseUtil.class) {
        // 第二次检查：防止多个线程同时进入同步块时重复创建连接
        if (connection == null || connection.isClosed()) {
          // 重新初始化连接
          initConnection();
        }
      }
    }
    return connection;
  }

  /**
   * 创建HBase表 - 就像在数据库里"新建一张表格"
   * 
   * HBase表的基本概念：
   * - 表(Table)：就像Excel的工作表，用来存储数据
   * - 列族(Column Family)：把相关的列归类在一起，比如"基本信息"、"评分信息"
   * - 行键(Row Key)：每行数据的唯一标识，就像身份证号
   * 
   * 这个方法做了什么？
   * 1. 检查表是否已经存在，避免重复创建
   * 2. 创建表的结构定义（表名、列族等）
   * 3. 为每个列族设置参数（比如保存几个版本的数据）
   * 4. 真正在HBase中创建这张表
   *
   * @param tableName      表名，比如"movies"、"ratings"
   * @param columnFamilies 列族名称数组，比如["info", "rating"]
   * @throws IOException 如果创建失败会抛出IO异常
   */
  public static void createTable(String tableName, String... columnFamilies) throws IOException {
    // try-with-resources语法，自动关闭Admin对象，避免资源泄露
    // Admin就像数据库管理员，负责创建、删除表等管理操作
    try (Admin admin = getConnection().getAdmin()) {
      // 将字符串表名转换为HBase的TableName对象
      TableName table = TableName.valueOf(tableName);

      // 先检查表是否已经存在，避免重复创建
      if (admin.tableExists(table)) {
        logger.warn("⚠ 表 [{}] 已存在，跳过创建", tableName);
        return;  // 直接返回，不再执行后面的创建操作
      }

      // 创建表的描述符，就像填写"建表申请单"
      HTableDescriptor tableDescriptor = new HTableDescriptor(table);
      
      // 为每个列族创建描述符并添加到表中
      for (String cf : columnFamilies) {
        // 创建列族描述符
        HColumnDescriptor columnDescriptor = new HColumnDescriptor(cf);
        
        // 设置最大版本数为3，意思是每个单元格最多保存3个历史版本的数据
        // 比如一个电影的评分被修改了，可以保留最近3次的修改记录
        columnDescriptor.setMaxVersions(3);
        
        // 将列族添加到表描述符中
        tableDescriptor.addFamily(columnDescriptor);
      }

      // 真正创建表，就像向数据库提交"建表申请"
      admin.createTable(tableDescriptor);
      
      // 记录成功信息
      logger.info("✓ 表 [{}] 创建成功，列族: [{}]", tableName, String.join(", ", columnFamilies));
    }
  }

  /**
   * 删除HBase表 - 就像"删除一张表格"，彻底清除表和所有数据
   * 
   * 为什么删除表要两步？
   * 1. HBase的安全机制：防止误删重要数据
   * 2. 先禁用表：让表停止服务，不能再读写数据
   * 3. 再删除表：彻底从系统中移除
   * 
   * 注意：删除表会永久丢失所有数据，操作需谨慎！
   *
   * @param tableName 要删除的表名
   * @throws IOException 如果删除失败会抛出IO异常
   */
  public static void dropTable(String tableName) throws IOException {
    // 获取管理员权限，用来执行删除操作
    try (Admin admin = getConnection().getAdmin()) {
      TableName table = TableName.valueOf(tableName);

      // 先检查表是否存在，不存在就没必要删除了
      if (!admin.tableExists(table)) {
        logger.warn("⚠ 表 [{}] 不存在，无需删除", tableName);
        return;
      }

      // 检查表是否还在运行中，如果是就先禁用它
      // 就像关机前要先关闭所有程序一样
      if (!admin.isTableDisabled(table)) {
        admin.disableTable(table);  // 禁用表，停止所有读写操作
        logger.info("✓ 表 [{}] 已禁用", tableName);
      }

      // 真正删除表，这一步不可逆！
      admin.deleteTable(table);
      logger.info("✓ 表 [{}] 已删除", tableName);
    }
  }

  /**
   * 插入单条数据 - 就像在Excel表格中"填写一个单元格"
   * 
   * HBase数据存储结构：
   * - 行键(rowKey)：这行数据的唯一标识，比如电影ID "1"
   * - 列族(columnFamily)：列的分组，比如 "info"
   * - 列名(column)：具体的字段名，比如 "title"
   * - 值(value)：实际存储的数据，比如 "玩具总动员"
   * 
   * 完整地址就是：表名:行键:列族:列名 = 值
   * 例如：movies:1:info:title = "玩具总动员"
   *
   * @param tableName    表名，数据要存到哪张表
   * @param rowKey       行键，这条数据的唯一标识
   * @param columnFamily 列族，列的分组名称
   * @param column       列名，具体的字段名
   * @param value        值，要存储的实际数据
   * @throws IOException 如果插入失败会抛出IO异常
   */
  public static void putData(String tableName, String rowKey, String columnFamily,
                             String column, String value) throws IOException {
    // 获取表的操作句柄，就像打开一个Excel文件
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 创建Put对象，指定要插入数据的行
      Put put = new Put(Bytes.toBytes(rowKey));
      
      // 添加列数据：指定列族、列名和值
      // Bytes.toBytes()将字符串转换为字节数组，因为HBase内部用字节存储
      put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
      
      // 执行插入操作
      table.put(put);
    }
  }

  /**
   * 批量插入数据 - 就像"批量复制粘贴"，一次性插入很多条数据
   * 
   * 为什么要批量插入？
   * 1. 性能更好：减少网络通信次数
   * 2. 效率更高：HBase可以优化批量操作
   * 3. 资源节省：减少连接建立和释放的开销
   * 
   * 什么时候用批量插入？
   * - 导入大量数据时（比如从CSV文件导入几万条记录）
   * - 需要同时插入多条相关数据时
   *
   * @param tableName 表名，数据要存到哪张表
   * @param puts      Put对象列表，每个Put代表一条要插入的数据
   * @throws IOException 如果插入失败会抛出IO异常
   */
  public static void putDataBatch(String tableName, List<Put> puts) throws IOException {
    // 先检查数据是否为空，避免无意义的操作
    if (puts == null || puts.isEmpty()) {
      logger.warn("⚠ 批量插入数据为空，跳过操作");
      return;
    }

    // 获取表的操作对象
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 一次性提交所有Put操作，HBase会自动优化批量插入
      table.put(puts);
      
      // 记录插入的数据条数，方便监控和调试
      logger.debug("✓ 批量插入 {} 条数据到表 [{}]", puts.size(), tableName);
    }
  }

  /**
   * 获取单条数据 - 就像"查找某个单元格的内容"
   * 
   * 这是最基本的查询操作：
   * 1. 根据行键精确定位到某一行
   * 2. 返回这一行的所有列数据
   * 3. 如果行不存在，返回空的Result对象
   *
   * @param tableName 表名，从哪张表查询
   * @param rowKey    行键，要查询哪一行的数据
   * @return 查询结果，包含这一行的所有列数据
   * @throws IOException 如果查询失败会抛出IO异常
   */
  public static Result getData(String tableName, String rowKey) throws IOException {
    // 获取表的操作句柄
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 创建Get对象，指定要查询的行键
      Get get = new Get(Bytes.toBytes(rowKey));
      
      // 执行查询并返回结果
      return table.get(get);
    }
  }

  /**
   * 扫描表中所有数据 - 就像"浏览整张表格"
   * 
   * 注意事项：
   * 1. 这个方法返回ResultScanner，使用完后必须手动关闭！
   * 2. 扫描大表时要小心，可能会很慢
   * 3. 建议在try-with-resources中使用，确保资源被正确释放
   * 
   * 使用示例：
   * try (ResultScanner scanner = HBaseUtil.scanTable("movies")) {
   *     for (Result result : scanner) {
   *         // 处理每一行数据
   *     }
   * }
   *
   * @param tableName 表名，要扫描哪张表
   * @return 结果扫描器，可以遍历所有行数据
   * @throws IOException 如果扫描失败会抛出IO异常
   */
  public static ResultScanner scanTable(String tableName) throws IOException {
    // 获取表的操作句柄（注意：这里不能用try-with-resources，因为要返回scanner）
    Table table = getConnection().getTable(TableName.valueOf(tableName));
    
    // 创建扫描对象，默认扫描所有行
    Scan scan = new Scan();
    
    // 返回扫描器，调用者负责关闭
    return table.getScanner(scan);
  }

  /**
   * 扫描表中指定行数的数据 - 就像"只看表格的前几行"
   * 
   * 为什么要限制行数？
   * 1. 避免一次性加载太多数据，导致内存不足
   * 2. 提高查询速度，只获取需要的数据
   * 3. 适合分页查询或数据预览
   * 
   * 性能优化：
   * - 设置了缓存大小，减少网络通信次数
   * - 自动管理资源，使用完自动关闭
   *
   * @param tableName 表名，要扫描哪张表
   * @param limit     限制行数，最多返回多少行数据
   * @return 结果列表，包含指定数量的行数据
   * @throws IOException 如果扫描失败会抛出IO异常
   */
  public static List<Result> scanTableWithLimit(String tableName, int limit) throws IOException {
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 创建扫描对象
      Scan scan = new Scan();
      
      // 设置缓存大小，优化网络传输
      // 取limit和100中的较小值，避免缓存过大
      scan.setCaching(Math.min(limit, 100));

      // 创建结果列表，预分配容量提高性能
      List<Result> results = new ArrayList<>(limit);
      
      // 执行扫描
      try (ResultScanner scanner = table.getScanner(scan)) {
        for (Result result : scanner) {
          results.add(result);
          
          // 达到限制数量就停止扫描
          if (results.size() >= limit) {
            break;
          }
        }
      }
      return results;
    }
  }

  /**
   * 删除整行数据 - 就像"删除表格中的一整行"
   * 
   * 这个操作会：
   * 1. 删除指定行的所有列数据
   * 2. 删除所有版本的历史数据
   * 3. 操作不可逆，删除后无法恢复
   * 
   * 使用场景：
   * - 用户注销账号，删除所有相关数据
   * - 清理测试数据
   * - 数据过期清理
   *
   * @param tableName 表名，从哪张表删除数据
   * @param rowKey    行键，要删除哪一行
   * @throws IOException 如果删除失败会抛出IO异常
   */
  public static void deleteData(String tableName, String rowKey) throws IOException {
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 创建Delete对象，指定要删除的行
      Delete delete = new Delete(Bytes.toBytes(rowKey));
      
      // 执行删除操作
      table.delete(delete);
      
      // 记录删除信息，方便审计和调试
      logger.info("✓ 删除行 [{}] 从表 [{}]", rowKey, tableName);
    }
  }

  /**
   * 删除指定列的数据 - 就像"删除表格中的某个单元格"
   * 
   * 精确删除：
   * 1. 只删除指定的列，其他列不受影响
   * 2. 只删除最新版本的数据，历史版本可能还在
   * 3. 比删除整行更精确，风险更小
   * 
   * 使用场景：
   * - 用户修改个人信息，删除旧的某个字段
   * - 纠正错误数据
   * - 隐私保护，删除敏感信息
   *
   * @param tableName    表名，从哪张表删除数据
   * @param rowKey       行键，要删除哪一行的数据
   * @param columnFamily 列族，要删除哪个列族的数据
   * @param column       列名，要删除哪一列的数据
   * @throws IOException 如果删除失败会抛出IO异常
   */
  public static void deleteColumn(String tableName, String rowKey, String columnFamily,
                                  String column) throws IOException {
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 创建Delete对象，指定要删除的行
      Delete delete = new Delete(Bytes.toBytes(rowKey));
      
      // 指定要删除的具体列
      delete.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
      
      // 执行删除操作
      table.delete(delete);
      
      // 记录详细的删除信息
      logger.info("✓ 删除列 [{}:{}] 从表 [{}] 的行 [{}]", columnFamily, column, tableName, rowKey);
    }
  }

  /**
   * 统计表中的行数 - 就像"数一数表格有多少行"
   * 
   * 性能优化技巧：
   * 1. 使用FirstKeyOnlyFilter：只读取每行的第一个键，不读取具体数据
   * 2. 设置缓存：减少网络通信次数
   * 3. 只统计行数，不返回具体数据，速度更快
   * 
   * 注意事项：
   * - 大表统计可能很慢，建议在业务低峰期执行
   * - 返回的是准确的行数，不是估算值
   *
   * @param tableName 表名，要统计哪张表的行数
   * @return 行数，表中总共有多少行数据
   * @throws IOException 如果统计失败会抛出IO异常
   */
  public static long countRows(String tableName) throws IOException {
    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
      // 创建扫描对象
      Scan scan = new Scan();
      
      // 设置过滤器：只获取每行的第一个键，不读取具体数据
      // 这样可以大大提高统计速度
      scan.setFilter(new FirstKeyOnlyFilter());
      
      // 设置缓存大小，优化网络传输
      scan.setCaching(1000);

      long count = 0;  // 计数器
      
      // 执行扫描并计数
      try (ResultScanner scanner = table.getScanner(scan)) {
        for (Result result : scanner) {
          count++;  // 每扫描到一行就加1
        }
      }
      return count;
    }
  }

  /**
   * 检查表是否存在 - 就像"查看某个文件是否存在"
   * 
   * 使用场景：
   * 1. 创建表前先检查，避免重复创建
   * 2. 操作表前先确认，避免操作不存在的表
   * 3. 系统初始化时检查必要的表是否存在
   *
   * @param tableName 表名，要检查哪张表
   * @return true表示存在，false表示不存在
   * @throws IOException 如果检查失败会抛出IO异常
   */
  public static boolean tableExists(String tableName) throws IOException {
    try (Admin admin = getConnection().getAdmin()) {
      // 调用HBase的API检查表是否存在
      return admin.tableExists(TableName.valueOf(tableName));
    }
  }

  /**
   * 关闭HBase连接 - 就像"挂断电话"，释放网络连接
   * 
   * 什么时候需要关闭连接？
   * 1. 程序正常退出时
   * 2. 长时间不使用HBase时
   * 3. 系统资源紧张时
   * 
   * 注意事项：
   * - 一般情况下不需要手动调用，JVM退出时会自动关闭
   * - 关闭后如果再次使用，会自动重新连接
   * - 多线程环境下要小心，确保其他线程不再使用连接
   */
  public static void close() {
    // 检查连接是否存在且未关闭
    if (connection != null && !connection.isClosed()) {
      try {
        // 关闭连接
        connection.close();
        logger.info("✓ HBase连接已关闭");
      } catch (IOException e) {
        // 记录关闭失败的错误，但不抛出异常（因为程序可能正在退出）
        logger.error("✗ 关闭HBase连接失败", e);
      }
    }
  }
  
  /**
   * 为Spark集成提供HBase连接 - 用于Spark作业中连接HBase
   * 
   * @return HBase连接对象
   */
  public static Connection getSparkConnection() {
    return getConnection();
  }
}