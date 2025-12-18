package com.movie.hbase;

// HBase核心类导入 - 用于操作HBase数据
import org.apache.hadoop.hbase.Cell;           // HBase中的单元格，存储一个具体的数据值
import org.apache.hadoop.hbase.CellUtil;       // 单元格工具类，用于提取Cell中的各种信息
import org.apache.hadoop.hbase.client.Put;     // Put操作类，用于向HBase插入数据
import org.apache.hadoop.hbase.client.Result;  // 查询结果类，包含一行的所有数据
import org.apache.hadoop.hbase.util.Bytes;     // 字节转换工具类，HBase内部都是字节存储

// 日志相关导入
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Java基础类导入
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HBase 表操作测试类 - 你的HBase学习实验室！
 * 
 * 这个类就像一个"HBase操作大全"，演示了HBase的所有基本操作。
 * 通过运行这个测试，你可以：
 * 1. 学习HBase的基本概念和操作流程
 * 2. 验证HBase环境是否正常工作
 * 3. 理解HBase数据模型（表、行、列族、列、单元格）
 * 4. 掌握HBase的CRUD操作
 * 
 * 测试覆盖的操作：
 * - CREATE: 创建表 (testCreateTable)
 * - PUT: 插入数据，支持单条和批量 (testPutData)
 * - GET: 根据RowKey查询单行数据 (testGetData)
 * - SCAN: 扫描表中的多行数据 (testScanData)
 * - DELETE: 删除单个列 (testDeleteColumn)
 * - DELETEALL: 删除整行数据 (testDeleteRow)
 * - DISABLE: 禁用表 (testDisableTable)
 * - DROP: 删除表 (testDropTable)
 * 
 * 设计思路：
 * 1. 按照HBase操作的生命周期顺序进行测试
 * 2. 每个测试方法独立，便于单独调试
 * 3. 详细的日志输出，方便观察操作结果
 * 4. 异常处理完善，确保资源正确释放
 */
public class HBaseTableTest {
  // 日志记录器 - 用于输出测试过程和结果
  private static final Logger logger = LoggerFactory.getLogger(HBaseTableTest.class);

  // 测试用的表名 - 使用独立的测试表，避免影响生产数据
  private static final String TEST_TABLE = "test_movies";
  
  // 列族名 - HBase中列的分组，这里用"info"存储电影基本信息
  // 列族是HBase表结构设计的重要概念，同一列族的数据存储在一起
  private static final String CF = "info";

  /**
   * 主方法 - HBase操作测试的总指挥
   * 
   * 按照HBase表的完整生命周期进行测试：
   * 创建 → 插入 → 查询 → 扫描 → 删除列 → 删除行 → 禁用 → 删除表
   * 
   * 这个顺序很重要：
   * 1. 先创建表，才能进行数据操作
   * 2. 先插入数据，才能测试查询功能
   * 3. 测试各种删除操作，了解数据清理方式
   * 4. 最后删除表，清理测试环境
   */
  public static void main(String[] args) {
    try {
      logger.info("========== HBase 表操作测试 ==========\n");

      // 1. 测试创建表 - HBase操作的第一步
      testCreateTable();

      // 2. 测试插入数据 (put) - 学习单条和批量插入
      testPutData();

      // 3. 测试查询数据 (get) - 根据RowKey精确查询
      testGetData();

      // 4. 测试扫描数据 (scan) - 批量读取多行数据
      testScanData();

      // 5. 测试删除单个列 (delete) - 精确删除某个字段
      testDeleteColumn();

      // 6. 测试删除整行 (deleteall) - 删除一行的所有数据
      testDeleteRow();

      // 7. 测试禁用表 (disable) - 表管理操作
      testDisableTable();

      // 8. 测试删除表 (drop) - 清理测试环境
      testDropTable();

      logger.info("\n========== 所有测试完成 ==========");

    } catch (IOException e) {
      // HBase操作可能出现的异常：网络问题、权限问题、表不存在等
      logger.error("测试失败", e);
      System.exit(1);
    } finally {
      // 无论测试成功还是失败，都要关闭HBase连接，释放资源
      // 这是良好的编程习惯，避免连接泄露
      HBaseUtil.close();
    }
  }

  /**
   * 测试1: 创建表 - HBase表结构设计的实践
   * 
   * HBase表的特点：
   * 1. 表名全局唯一
   * 2. 必须定义至少一个列族
   * 3. 列族在创建表时确定，后续很难修改
   * 4. 同一列族的数据物理存储在一起，查询效率高
   * 
   * 创建表的步骤：
   * 1. 检查表是否已存在（避免重复创建）
   * 2. 如果存在则先删除（清理旧数据）
   * 3. 创建新表并指定列族
   * 4. 验证创建结果
   */
  private static void testCreateTable() throws IOException {
    logger.info(">>> 测试1: 创建表");

    // 防御性编程：如果表已存在，先删除
    // 这样确保每次测试都是从干净的环境开始
    if (HBaseUtil.tableExists(TEST_TABLE)) {
      logger.info("表已存在，先删除...");
      HBaseUtil.dropTable(TEST_TABLE);
    }

    // 创建新表，指定表名和列族
    // 列族"info"用于存储电影的基本信息（标题、类型等）
    HBaseUtil.createTable(TEST_TABLE, CF);

    // 验证表是否创建成功 - 重要的测试步骤
    boolean exists = HBaseUtil.tableExists(TEST_TABLE);
    logger.info("表创建{}：{}", exists ? "成功" : "失败", TEST_TABLE);
    logger.info("");
  }

  /**
   * 测试2: 插入数据 (put) - HBase数据写入的两种方式
   * 
   * HBase的Put操作特点：
   * 1. 基于RowKey进行数据定位
   * 2. 支持单条插入和批量插入
   * 3. 相同RowKey的数据会覆盖（更新）
   * 4. 插入时需要指定：表名、RowKey、列族、列名、值
   * 
   * 两种插入方式的对比：
   * - 单条插入：简单直接，适合少量数据
   * - 批量插入：效率更高，适合大量数据导入
   * 
   * 数据设计说明：
   * - RowKey使用电影ID，便于后续查询
   * - 列族"info"存储电影基本信息
   * - 列"title"存储电影标题
   * - 列"genres"存储电影类型（用|分隔多个类型）
   */
  private static void testPutData() throws IOException {
    logger.info(">>> 测试2: 插入数据 (put)");

    // 方式1：单条插入 - 适合实时数据更新
    logger.info("方式1: 单条插入");
    // 为电影ID=1插入标题信息
    HBaseUtil.putData(TEST_TABLE, "1", CF, "title", "Toy Story (1995)");
    // 为电影ID=1插入类型信息
    HBaseUtil.putData(TEST_TABLE, "1", CF, "genres", "Adventure|Animation|Children|Comedy|Fantasy");
    logger.info("插入电影ID=1");

    // 方式2：批量插入 - 适合数据导入场景
    logger.info("方式2: 批量插入");
    List<Put> puts = new ArrayList<>();

    // 电影2的数据准备
    Put put2 = new Put(Bytes.toBytes("2"));  // RowKey = "2"
    put2.addColumn(Bytes.toBytes(CF), Bytes.toBytes("title"), Bytes.toBytes("Jumanji (1995)"));
    put2.addColumn(Bytes.toBytes(CF), Bytes.toBytes("genres"), Bytes.toBytes("Adventure|Children|Fantasy"));
    puts.add(put2);

    // 电影3的数据准备
    Put put3 = new Put(Bytes.toBytes("3"));  // RowKey = "3"
    put3.addColumn(Bytes.toBytes(CF), Bytes.toBytes("title"), Bytes.toBytes("Grumpier Old Men (1995)"));
    put3.addColumn(Bytes.toBytes(CF), Bytes.toBytes("genres"), Bytes.toBytes("Comedy|Romance"));
    puts.add(put3);

    // 电影4的数据准备
    Put put4 = new Put(Bytes.toBytes("4"));  // RowKey = "4"
    put4.addColumn(Bytes.toBytes(CF), Bytes.toBytes("title"), Bytes.toBytes("Waiting to Exhale (1995)"));
    put4.addColumn(Bytes.toBytes(CF), Bytes.toBytes("genres"), Bytes.toBytes("Comedy|Drama|Romance"));
    puts.add(put4);

    // 执行批量插入 - 一次性提交所有Put操作
    HBaseUtil.putDataBatch(TEST_TABLE, puts);
    logger.info("批量插入 {} 条记录", puts.size());

    // 统计总记录数 - 验证插入结果
    long count = HBaseUtil.countRows(TEST_TABLE);
    logger.info("当前表中共有 {} 条记录", count);
    logger.info("");
  }

  /**
   * 测试3: 查询数据 (get) - HBase精确查询的使用
   * 
   * HBase的Get操作特点：
   * 1. 基于RowKey进行精确查询（类似主键查询）
   * 2. 查询速度非常快，时间复杂度O(1)
   * 3. 返回指定RowKey的所有列数据
   * 4. 如果RowKey不存在，返回空结果
   * 
   * 查询结果Result对象包含：
   * - RowKey：行键
   * - Cell列表：该行的所有列数据
   * - 每个Cell包含：列族、列名、值、时间戳等信息
   * 
   * 使用场景：
   * - 根据用户ID查询用户信息
   * - 根据订单ID查询订单详情
   * - 根据商品ID查询商品信息
   */
  private static void testGetData() throws IOException {
    logger.info(">>> 测试3: 查询数据 (get)");

    // 查询电影ID=1 - 测试正常查询
    Result result = HBaseUtil.getData(TEST_TABLE, "1");
    logger.info("查询电影ID=1:");
    printResult(result);

    // 查询电影ID=2 - 测试批量插入的数据
    result = HBaseUtil.getData(TEST_TABLE, "2");
    logger.info("查询电影ID=2:");
    printResult(result);

    // 查询不存在的电影 - 测试异常情况处理
    result = HBaseUtil.getData(TEST_TABLE, "999");
    logger.info("查询不存在的电影ID=999:");
    if (result.isEmpty()) {
      logger.info("  未找到记录");
    } else {
      printResult(result);
    }
    logger.info("");
  }

  /**
   * 测试4: 扫描数据 (scan) - HBase批量查询的使用
   * 
   * HBase的Scan操作特点：
   * 1. 可以扫描表中的多行数据
   * 2. 支持范围查询（startRow到stopRow）
   * 3. 支持过滤器，可以按条件筛选数据
   * 4. 支持限制返回行数，避免内存溢出
   * 
   * Scan vs Get的区别：
   * - Get：查询单行，基于RowKey精确匹配
   * - Scan：查询多行，可以设置范围和条件
   * 
   * 使用场景：
   * - 分页查询数据
   * - 统计分析（如查询某个时间段的数据）
   * - 数据导出
   * - 全表扫描（小心使用，大表会很慢）
   * 
   * 注意事项：
   * - 大表扫描要设置限制，避免内存问题
   * - 合理使用过滤器，减少网络传输
   * - 考虑使用协处理器进行服务端计算
   */
  private static void testScanData() throws IOException {
    logger.info(">>> 测试4: 扫描数据 (scan)");

    // 扫描所有数据，限制最多返回100条
    // 这是一个安全的做法，避免大表导致内存溢出
    logger.info("扫描所有数据:");
    List<Result> results = HBaseUtil.scanTableWithLimit(TEST_TABLE, 100);
    
    int index = 1;
    for (Result result : results) {
      logger.info("--- 记录 {} ---", index++);
      printResult(result);
    }

    logger.info("共扫描到 {} 条记录", results.size());
    logger.info("");
  }

  /**
   * 测试5: 删除单个列 (delete) - HBase精确删除操作
   * 
   * HBase的列删除特点：
   * 1. 可以精确删除某一行的某一列
   * 2. 删除操作是逻辑删除，数据标记为删除状态
   * 3. 真正的物理删除在Major Compaction时进行
   * 4. 删除后该列不会在查询结果中出现
   * 
   * 删除的粒度：
   * - 删除整个表：dropTable
   * - 删除整行：deleteData(table, rowKey)
   * - 删除单列：deleteColumn(table, rowKey, family, qualifier)
   * - 删除列族：需要修改表结构
   * 
   * 使用场景：
   * - 用户取消某个设置（删除配置列）
   * - 商品下架某个属性（删除属性列）
   * - 数据清理和隐私保护
   * 
   * 注意事项：
   * - 删除操作不可逆，要谨慎使用
   * - 考虑使用软删除（标记字段）代替物理删除
   * - 删除大量数据时考虑性能影响
   */
  private static void testDeleteColumn() throws IOException {
    logger.info(">>> 测试5: 删除单个列 (delete)");

    // 删除前查询 - 展示完整数据
    logger.info("删除前，电影ID=1的数据:");
    Result before = HBaseUtil.getData(TEST_TABLE, "1");
    printResult(before);

    // 删除genres列 - 演示精确删除
    // 这里删除电影ID=1的类型信息，但保留标题信息
    HBaseUtil.deleteColumn(TEST_TABLE, "1", CF, "genres");

    // 删除后查询 - 验证删除效果
    logger.info("删除genres列后，电影ID=1的数据:");
    Result after = HBaseUtil.getData(TEST_TABLE, "1");
    printResult(after);

    // 恢复数据 - 为后续测试准备
    HBaseUtil.putData(TEST_TABLE, "1", CF, "genres", "Adventure|Animation|Children|Comedy|Fantasy");
    logger.info("已恢复genres列");
    logger.info("");
  }

  /**
   * 测试6: 删除整行 (deleteall) - HBase行级删除操作
   * 
   * HBase的行删除特点：
   * 1. 删除指定RowKey的所有列数据
   * 2. 删除操作是逻辑删除，实际数据标记为删除状态
   * 3. 物理删除在Major Compaction时进行
   * 4. 删除后该行在查询时返回空结果
   * 
   * 行删除 vs 列删除：
   * - 行删除：删除整行的所有数据，影响范围大
   * - 列删除：只删除特定列，其他列数据保留
   * 
   * 使用场景：
   * - 用户注销账户（删除用户所有信息）
   * - 订单取消（删除订单所有数据）
   * - 数据过期清理（删除过期记录）
   * - 测试数据清理
   * 
   * 注意事项：
   * - 行删除影响范围大，操作前要确认
   * - 考虑数据备份和恢复策略
   * - 大批量删除时注意性能影响
   * - 删除操作不可逆，谨慎使用
   */
  private static void testDeleteRow() throws IOException {
    logger.info(">>> 测试6: 删除整行 (deleteall)");

    // 删除前统计 - 记录删除前的数据量
    long beforeCount = HBaseUtil.countRows(TEST_TABLE);
    logger.info("删除前，表中有 {} 条记录", beforeCount);

    // 删除电影ID=4的整行 - 演示行级删除
    // 这会删除该行的所有列（title、genres等）
    HBaseUtil.deleteData(TEST_TABLE, "4");

    // 删除后统计 - 验证删除效果
    long afterCount = HBaseUtil.countRows(TEST_TABLE);
    logger.info("删除电影ID=4后，表中有 {} 条记录", afterCount);

    // 验证删除 - 通过查询确认数据已删除
    Result result = HBaseUtil.getData(TEST_TABLE, "4");
    if (result.isEmpty()) {
      logger.info("✓ 确认电影ID=4已被删除");
    } else {
      logger.warn("✗ 电影ID=4仍然存在");
    }
    logger.info("");
  }

  /**
   * 测试7: 禁用表 (disable) - HBase表状态管理
   * 
   * HBase表的状态：
   * 1. ENABLED：正常状态，可以进行读写操作
   * 2. DISABLED：禁用状态，无法进行读写操作
   * 3. DISABLING：正在禁用中的过渡状态
   * 4. ENABLING：正在启用中的过渡状态
   * 
   * 禁用表的作用：
   * 1. 表结构修改前必须先禁用表
   * 2. 删除表前必须先禁用表
   * 3. 维护操作时暂停表的使用
   * 4. 数据迁移时的安全措施
   * 
   * 禁用表的影响：
   * - 所有读写操作都会失败
   * - 表仍然存在，只是不可用
   * - 可以通过enable重新启用
   * 
   * 使用场景：
   * - 表结构变更（添加/删除列族）
   * - 系统维护
   * - 数据备份
   * - 表删除前的准备工作
   * 
   * 注意事项：
   * - 禁用表会影响应用程序的正常使用
   * - 禁用大表可能需要较长时间
   * - 确保没有正在进行的操作再禁用表
   */
  private static void testDisableTable() throws IOException {
    logger.info(">>> 测试7: 禁用表 (disable)");

    // 注意：禁用表后无法进行读写操作
    // 这里只是演示，实际使用时要谨慎
    // 在我们的HBaseUtil中，dropTable方法会自动处理禁用操作
    logger.info("禁用表操作将在删除表时自动执行");
    logger.info("如需手动禁用，使用 HBase Shell: disable '{}'", TEST_TABLE);
    logger.info("如需手动启用，使用 HBase Shell: enable '{}'", TEST_TABLE);
    logger.info("表状态查询，使用 HBase Shell: is_enabled '{}'", TEST_TABLE);
    logger.info("");
  }

  /**
   * 测试8: 删除表 (drop) - HBase表的完全清理
   * 
   * 删除表的过程：
   * 1. 检查表是否存在
   * 2. 禁用表（如果表处于启用状态）
   * 3. 删除表结构和所有数据
   * 4. 清理相关的元数据
   * 
   * 删除表的影响：
   * - 表结构完全消失
   * - 所有数据永久丢失
   * - 相关的索引和协处理器也被删除
   * - 操作不可逆
   * 
   * 删除表 vs 清空表：
   * - 删除表：表结构和数据都删除，表不存在
   * - 清空表：只删除数据，表结构保留
   * 
   * 使用场景：
   * - 测试环境清理
   * - 临时表的清理
   * - 项目结束后的资源清理
   * - 表结构重新设计
   * 
   * 注意事项：
   * - 删除表是危险操作，确保数据已备份
   * - 生产环境要有严格的权限控制
   * - 删除大表可能需要较长时间
   * - 考虑使用表重命名代替删除
   */
  private static void testDropTable() throws IOException {
    logger.info(">>> 测试8: 删除表 (drop)");

    // 删除表 - 这会自动处理禁用表的操作
    // HBaseUtil.dropTable内部会先检查表状态，如果启用则先禁用，然后删除
    HBaseUtil.dropTable(TEST_TABLE);

    // 验证表是否已删除 - 通过检查表存在性确认删除结果
    boolean exists = HBaseUtil.tableExists(TEST_TABLE);
    logger.info("表删除{}：{}", exists ? "失败" : "成功", TEST_TABLE);
    
    if (!exists) {
      logger.info("✓ 测试表已完全清理，测试环境恢复干净状态");
    } else {
      logger.warn("✗ 表删除失败，可能需要手动清理");
    }
    logger.info("");
  }

  /**
   * 打印查询结果 - HBase Result对象的可视化展示
   * 
   * Result对象的结构：
   * - RowKey：行键，用于唯一标识一行数据
   * - Cell列表：该行的所有列数据
   * - 每个Cell包含：列族、列名、值、时间戳等
   * 
   * 数据展示格式：
   * RowKey: [行键值]
   *   [列族]:[列名] = [值]
   *   [列族]:[列名] = [值]
   *   ...
   * 
   * 这种格式的好处：
   * 1. 清晰展示HBase的数据结构
   * 2. 便于理解列族和列的概念
   * 3. 方便调试和数据验证
   * 4. 符合HBase Shell的输出格式
   * 
   * 使用场景：
   * - 调试HBase查询结果
   * - 验证数据插入是否正确
   * - 学习HBase数据模型
   * - 测试和演示
   * 
   * 注意事项：
   * - 大量数据时要控制输出量
   * - 二进制数据可能显示异常
   * - 考虑数据的编码格式
   */
  private static void printResult(Result result) {
    // 检查结果是否为空 - 防止空指针异常
    if (result.isEmpty()) {
      logger.info("  (空结果)");
      return;
    }

    // 提取并显示RowKey - HBase中每行数据的唯一标识
    String rowKey = Bytes.toString(result.getRow());
    logger.info("  RowKey: {}", rowKey);

    // 遍历所有Cell，展示列数据
    // Cell是HBase中数据存储的最小单位
    for (Cell cell : result.listCells()) {
      // 提取列族名 - 列的分组标识
      String family = Bytes.toString(CellUtil.cloneFamily(cell));
      
      // 提取列名（限定符）- 具体的字段名
      String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
      
      // 提取值 - 实际存储的数据
      String value = Bytes.toString(CellUtil.cloneValue(cell));
      
      // 按照HBase标准格式输出：列族:列名 = 值
      logger.info("    {}:{} = {}", family, qualifier, value);
    }
  }
}
