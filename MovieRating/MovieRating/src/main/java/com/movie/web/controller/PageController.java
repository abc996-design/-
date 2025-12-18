package com.movie.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器
 */
@Controller  /*Spring MVC的控制器注解，用于标记这个类作为Web请求处理器*/
public class PageController {     /*@Controller用于返回视图页面，而@RestController用于返回JSON数据*/

  /**
   * 首页
   */
  @GetMapping("/")  /*Spring MVC的HTTP GET请求映射注解*/
  public String index() {
    return "index";
  }  /*对应index.html模板文件，Spring Boot会使用Thymeleaf模板引擎渲染这个页面*/

  /**
   * 电影查询页面
   */
  @GetMapping("/movies")
  public String movies() {
    return "movies";
  }

  /**
   * 评分查询页面
   */
  @GetMapping("/ratings")
  public String ratings() {
    return "ratings";
  }

  /**
   * 统计页面
   */
  @GetMapping("/statistics")
  public String statistics() {
    return "statistics";
  }
}
