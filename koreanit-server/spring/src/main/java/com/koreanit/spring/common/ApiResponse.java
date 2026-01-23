package com.koreanit.spring.common;

public class ApiResponse<T> {

  public boolean success;
  public String message;
  public T data;
  public String code; // 실패 시 사용

  private ApiResponse(boolean success, String message, T data, String code) {
    this.success = success;
    this.message = message;
    this.data = data;
    this.code = code;
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "OK", data, null);
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, message, data, null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(false, message, null, code);
  }
}