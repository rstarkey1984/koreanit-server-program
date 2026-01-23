package com.koreanit.spring.error;

import com.koreanit.spring.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {

    HttpStatus status = switch (e.getErrorCode()) {
      case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
      case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case DUPLICATE_RESOURCE -> HttpStatus.CONFLICT;
      case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };

    return ResponseEntity
        .status(status)
        .body(ApiResponse.fail(
            e.getErrorCode().name(),
            e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), "서버 오류"));
  }
  
}