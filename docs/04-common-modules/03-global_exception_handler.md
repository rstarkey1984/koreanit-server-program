# Global Exception Handler

이 장에서는
Spring Boot 서버에서 발생하는 모든 예외를 **한 곳에서 일관된 규칙으로 처리하는 구조**를 완성한다.

앞 장에서
에러 코드(`ErrorCode`)와 공통 예외(`ApiException`)를 설계했다면,
이제는 **그 예외들이 실제 HTTP 응답으로 어떻게 변환되는지**를 결정해야 한다.

이 장의 목표는 단순히 코드를 작성하는 것이 아니라,
**왜 전역 예외 처리가 필요한지**,
**예외는 언제 어디서 발생하는지**,
**각 계층은 무엇을 책임져야 하는지**를 구조적으로 이해하는 것이다.

---

## 1. 예외는 어디에서 처리되어야 하는가

많은 초보 코드에서 예외 처리는 다음처럼 흩어져 있다.

```text
Controller에서 try-catch
Service에서 try-catch
Repository에서 try-catch
```

이 구조는 처음에는 안전해 보이지만,
프로젝트가 커질수록 심각한 문제를 만든다.

문제점:

* 같은 예외 처리 코드가 여러 곳에 반복된다
* 응답 포맷이 상황마다 달라진다
* 예외가 중간에서 처리되어 원인을 추적하기 어려워진다

그래서 서버 구조에서는 다음 원칙을 따른다.

> **예외는 발생한 위치와 상관없이 위로 전달되고, HTTP 응답이 결정되는 Global Exception Handler에서 한 번만 처리된다.**

---

## 2. Global Exception Handler의 역할

Global Exception Handler는
서버 전역에서 발생한 예외를 가로채서
**정해진 규칙에 따라 HTTP 응답으로 변환하는 전용 계층**이다.

이 계층의 책임은 명확하다.

* 어떤 예외인지 판별한다
* 어떤 HTTP 상태 코드를 내려줄지 결정한다
* 공통 실패 응답(`ApiResponse`)을 생성한다
* 필요한 수준의 로그를 남긴다

즉,
Global Exception Handler는
**예외 처리 로직의 종착지**다.

---

## 3. 예외 처리 흐름 전체 그림

Spring Boot 서버에서 예외는 다음 흐름으로 처리된다.

```text
HTTP 요청
  ↓
@RequestBody 파싱
  ↓
@Valid 검증
  ↓ (실패 시 예외 발생)
Controller 메서드 실행 안 됨
  ↓
Global Exception Handler
  ↓
ApiResponse 실패 응답 생성
  ↓
클라이언트 응답
```

이 흐름을 이해하면
왜 Controller에 try-catch를 두면 안 되는지 자연스럽게 알 수 있다.

---

## 4. Validation 예외는 Controller 이전 단계에서 발생한다

다음 Controller 코드를 보자.

```java
@PostMapping
public void create(@Valid @RequestBody User user) {
    userService.register(user);
}
```

많은 사람이 이 메서드 안에서 검증이 일어난다고 생각하지만,
실제로는 그렇지 않다.

> **@Valid 검증은 Controller 메서드가 실행되기 전에 수행된다**

즉, 검증에 실패하면:

* Controller 메서드는 실행되지 않는다
* Service / Repository는 호출되지 않는다
* Spring이 예외를 바로 던진다

이때 발생하는 대표적인 예외는 다음과 같다.

* `MethodArgumentNotValidException`

  * @Valid 객체 검증 실패
* `HttpMessageNotReadableException`

  * 요청 바디가 없음
  * JSON 형식 오류
  * 타입 불일치

이 예외들은
비즈니스 로직의 문제가 아니라
**요청 형식과 값이 올바르지 않을 때 발생하는 프레임워크 예외**다.

---

## 5. Controller는 왜 예외를 처리하지 않는가

Controller의 책임은 단순하다.

* HTTP 요청을 받는다
* Service를 호출한다
* 응답을 반환한다

Controller가 예외를 직접 처리하기 시작하면:

* 비즈니스 흐름과 HTTP 처리 로직이 섞이고
* 공통 규칙을 유지하기 어려워지며
* 유지보수 비용이 급격히 증가한다

그래서 Controller는
**예외를 잡지 않고 그대로 던진다.**

예외를 어떻게 응답으로 바꿀지는
Global Exception Handler의 책임이다.

---

## 6. 전역 예외 처리 대상 정리

이 프로젝트에서
Global Exception Handler가 책임지는 예외는 다음과 같다.

| 구분            | 예외                              | 발생 주체  |
| ------------- | ------------------------------- | ------ |
| 비즈니스 예외       | ApiException                    | 개발자    |
| 요청 바디 오류      | HttpMessageNotReadableException | Spring |
| Validation 실패 | MethodArgumentNotValidException | Spring |
| 처리되지 않은 예외    | Exception                       | 시스템    |

> 이 장에서 말하는 **Validation 포함**이란,
> Spring이 자동으로 발생시키는 검증 예외까지
> 모두 동일한 실패 응답 구조로 통일한다는 의미다.

---

## 7. GlobalExceptionHandler 구현

아래 코드는
이 장에서 사용할 최종 GlobalExceptionHandler 구현이다.

```java
package com.koreanit.spring.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.koreanit.spring.common.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private String origin(Throwable e) {
    StackTraceElement[] trace = e.getStackTrace();
    if (trace == null || trace.length == 0) return "unknown";

    // 우리 코드 위치를 우선으로 찍는다
    for (StackTraceElement el : trace) {
      if (el.getClassName().startsWith("com.koreanit.")) {
        return el.getClassName() + ":" + el.getLineNumber();
      }
    }

    // fallback: 첫 프레임
    StackTraceElement top = trace[0];
    return top.getClassName() + ":" + top.getLineNumber();
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
    ErrorCode code = e.getErrorCode();

    log.warn("[API_ERROR] code={} message=\"{}\" origin={}",
        code.name(), e.getMessage(), origin(e));

    return ResponseEntity
        .status(code.getStatus())
        .body(ApiResponse.fail(code.name(), e.getMessage()));
  }

  // 핵심: 중복 제약 위반은 409로 매핑하고 warn 1줄만 찍는다
  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
    log.warn("[DUPLICATE_KEY] message=\"{}\" origin={}",
        "중복된 값으로 인해 저장할 수 없습니다", origin(e));

    return ResponseEntity
        .status(ErrorCode.DUPLICATE_RESOURCE.getStatus())
        .body(ApiResponse.fail(
            ErrorCode.DUPLICATE_RESOURCE.name(),
            "이미 존재하는 사용자입니다"
        ));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleBodyMissing(HttpMessageNotReadableException e) {
    log.warn("[INVALID_BODY] origin={}", origin(e));

    return ResponseEntity
        .status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(ApiResponse.fail(
            ErrorCode.INVALID_REQUEST.name(),
            "요청 바디가 비어 있거나 형식이 올바르지 않습니다"
        ));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
    FieldError error = e.getBindingResult().getFieldError();

    String message = (error != null && error.getDefaultMessage() != null && !error.getDefaultMessage().isBlank())
        ? error.getDefaultMessage()
        : "요청 값이 올바르지 않습니다";

    log.warn("[VALIDATION_FAIL] message=\"{}\" origin={}", message, origin(e));

    return ResponseEntity
        .status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    // 5xx만 스택트레이스 출력
    log.error("[INTERNAL_ERROR] origin={}", origin(e), e);

    return ResponseEntity
        .status(ErrorCode.INTERNAL_ERROR.getStatus())
        .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), "서버 오류"));
  }
}
```

---

## 이 장의 핵심 정리

* 예외 처리는 전역에서 한 번만 한다
* Global Exception Handler는 응답 변환 지점이다
* Controller는 예외를 처리하지 않고 던진다
* Validation 예외는 Controller 이전 단계에서 발생한다
* 모든 실패 응답은 ApiResponse로 통일된다

---

## 다음 단계

→ [**요청 로깅 전략 (Filter 기반)**](04-request-logging-strategy.md)