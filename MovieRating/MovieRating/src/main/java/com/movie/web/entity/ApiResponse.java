package com.movie.web.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 */
@Data /*生成getter、setter、toString、equals和hashCode方法*/
@NoArgsConstructor  /*生成一个无参构造函数*/
@AllArgsConstructor  /*生成一个包含所有字段的构造函数*/
public class ApiResponse<T> {
  private int code;  /*整型，表示响应状态码*/
  private String message;  /*字符串，表示响应消息*/
  private T data;   /*泛型字段，用于携带响应数据*/

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(200, "success", data);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(200, message, data);
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(500, message, null);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
