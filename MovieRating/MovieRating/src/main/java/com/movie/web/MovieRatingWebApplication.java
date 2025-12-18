package com.movie.web;

// Spring Boot核心注解导入
import org.springframework.boot.SpringApplication;  // Spring Boot应用启动器
import org.springframework.boot.autoconfigure.SpringBootApplication;  // 自动配置注解
import org.springframework.context.annotation.ComponentScan;  // 组件扫描注解

/**
 * 电影评分系统Web应用主启动类 - Spring Boot应用的入口点
 * 
 * 这个类是整个电影评分系统的启动入口，负责初始化和配置Spring Boot应用。
 * 它承担着以下重要职责：
 * 
 * 1. 应用启动：作为Spring Boot应用的main方法入口
 * 2. 自动配置：启用Spring Boot的自动配置机制
 * 3. 组件扫描：定义Spring组件的扫描范围
 * 4. 容器初始化：初始化Spring IoC容器和相关Bean
 * 
 * 系统架构：
 * - Web层：处理HTTP请求和响应（Controller）
 * - 服务层：处理业务逻辑（Service）
 * - 数据层：处理HBase数据访问（HBase相关类）
 * - 实体层：定义数据模型（Entity）
 * 
 * 技术栈：
 * - Spring Boot：应用框架和自动配置
 * - Spring MVC：Web层框架
 * - HBase：分布式NoSQL数据库
 * - Lombok：减少样板代码
 * - Maven：项目构建和依赖管理
 * 
 * 部署特点：
 * - 内嵌Tomcat：无需外部应用服务器
 * - 独立运行：可以直接通过java -jar启动
 * - 配置外化：支持application.properties配置
 * - 健康检查：内置监控和管理端点
 * 
 * 扫描包结构：
 * - com.movie.web：Web层相关组件（Controller、Service、Entity）
 * - com.movie.hbase：HBase数据访问层组件
 * 
 * 启动流程：
 * 1. 执行main方法
 * 2. SpringApplication.run()启动应用
 * 3. 自动配置生效
 * 4. 组件扫描和Bean注册
 * 5. 内嵌Tomcat启动
 * 6. 应用就绪，开始接收请求
 * 
 * 配置说明：
 * - 默认端口：8080（可通过server.port配置修改）
 * - 上下文路径：/（可通过server.servlet.context-path配置）
 * - 日志级别：INFO（可通过logging.level配置）
 * - HBase连接：需要配置HBase相关参数
 */
@SpringBootApplication  // Spring Boot主配置注解，等价于以下三个注解的组合：
                        // @Configuration：标识这是一个配置类
                        // @EnableAutoConfiguration：启用自动配置机制
                        // @ComponentScan：启用组件扫描（默认扫描当前包及子包）
@ComponentScan(basePackages = {"com.movie.web", "com.movie.hbase"})  
// 自定义组件扫描范围，明确指定要扫描的包：
// - com.movie.web：包含Web层的所有组件
//   - controller：REST API控制器
//   - service：业务逻辑服务
//   - entity：数据传输对象
// - com.movie.hbase：包含HBase数据访问层组件
//   - 数据访问对象和工具类
//   - HBase连接和操作相关组件
// 
// 为什么需要自定义扫描范围？
// 1. 明确性：清楚地定义哪些包需要被Spring管理
// 2. 性能：避免扫描不必要的包，提高启动速度
// 3. 隔离性：防止意外扫描到测试代码或其他无关代码
// 4. 可维护性：便于理解项目的包结构和组件分布
public class MovieRatingWebApplication {

  /**
   * 应用程序主入口方法 - Spring Boot应用的启动点
   * 
   * 这个方法是整个电影评分系统的启动入口，当执行java命令时会首先调用这个方法：
   * - JVM入口：符合Java应用程序的标准入口点规范
   * - 启动触发：通过SpringApplication.run()启动Spring Boot应用
   * - 参数传递：将命令行参数传递给Spring Boot框架
   * - 异常处理：Spring Boot会处理启动过程中的各种异常
   * 
   * 启动过程详解：
   * 1. JVM加载并执行main方法
   * 2. SpringApplication.run()方法被调用
   * 3. Spring Boot开始自动配置过程
   * 4. 扫描指定包下的组件并注册为Bean
   * 5. 初始化内嵌的Tomcat服务器
   * 6. 启动Web服务，开始监听HTTP请求
   * 7. 应用启动完成，输出启动日志
   * 
   * 启动参数说明：
   * - args：命令行参数数组，可以包含：
   *   - 配置参数：--server.port=8081
   *   - 环境参数：--spring.profiles.active=dev
   *   - 自定义参数：--custom.property=value
   * 
   * 常见启动方式：
   * 1. IDE直接运行：在开发环境中直接运行main方法
   * 2. Maven启动：mvn spring-boot:run
   * 3. JAR包启动：java -jar movie-rating.jar
   * 4. Docker启动：在容器中运行JAR包
   * 
   * 启动成功标志：
   * - 控制台输出"Started MovieRatingWebApplication"
   * - 显示启动耗时和监听端口
   * - 可以通过浏览器访问应用接口
   * 
   * 故障排查：
   * - 端口冲突：修改server.port配置
   * - 依赖问题：检查Maven依赖和版本兼容性
   * - 配置错误：检查application.properties配置
   * - HBase连接：确保HBase服务可用且配置正确
   * 
   * 性能监控：
   * - 启动时间：关注应用启动耗时
   * - 内存使用：监控JVM内存占用
   * - 线程数量：观察线程池的创建和使用
   * - 连接池：监控数据库连接池状态
   * 
   * 最佳实践：
   * - 合理配置JVM参数，如堆内存大小
   * - 使用配置文件管理不同环境的配置
   * - 添加健康检查端点便于监控
   * - 配置日志输出便于问题排查
   * 
   * @param args 命令行参数数组，用于传递启动参数和配置信息
   *             这些参数会被Spring Boot框架解析和使用
   *             支持标准的Spring Boot配置参数格式
   */
  public static void main(String[] args) {
    // SpringApplication.run()是Spring Boot的核心启动方法
    // 参数说明：
    // - MovieRatingWebApplication.class：主配置类，包含@SpringBootApplication注解
    // - args：命令行参数，会被Spring Boot解析为配置属性
    // 
    // 这个方法会：
    // 1. 创建Spring应用上下文（ApplicationContext）
    // 2. 注册配置类和自动配置
    // 3. 扫描并注册组件Bean
    // 4. 启动内嵌Web服务器（默认Tomcat）
    // 5. 初始化所有单例Bean
    // 6. 触发应用启动完成事件
    // 
    // 返回值：ConfigurableApplicationContext
    // - 可以用于获取Bean、关闭应用等操作
    // - 在这里我们不需要保存返回值，因为应用会持续运行
    SpringApplication.run(MovieRatingWebApplication.class, args);
    
    // 注意：这里没有其他代码，因为SpringApplication.run()会阻塞主线程
    // 直到应用被关闭（如收到SIGTERM信号或调用context.close()）
    // 
    // 如果需要在启动后执行特定逻辑，可以：
    // 1. 实现CommandLineRunner接口
    // 2. 实现ApplicationRunner接口
    // 3. 监听ApplicationReadyEvent事件
    // 4. 使用@PostConstruct注解的方法
  }
}
