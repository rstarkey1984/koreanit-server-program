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


---

## 4. Validation 예외는 Controller 이전 단계에서 발생한다

다음 Controller 코드를 보자.

```java
@PostMapping
public void create(@Valid @RequestBody User user) {
    userService.register(user);
}
```

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

## 5. 이 문서의 전역 예외 처리 대상 정리

이 프로젝트에서
Global Exception Handler가 책임지는 예외는 다음과 같다.

| 구분                | 예외 타입                             | 발생 계층                | 의미 / 처리 의도                              | HTTP 상태                   |
| ----------------- | --------------------------------- | -------------------- | --------------------------------------- | ------------------------- |
| **비즈니스 예외**       | `ApiException`                    | Service / Repository | 개발자가 의도적으로 던진 도메인·비즈니스 오류               | `ErrorCode`에 정의된 상태       |
| **중복 제약 위반**      | `DuplicateKeyException`           | Repository (DB)      | UNIQUE 제약 조건 위반 (예: username, email 중복) | 409 CONFLICT              |
| **요청 바디 오류**      | `HttpMessageNotReadableException` | Controller 이전        | 요청 바디 없음 / JSON 파싱 오류 / 타입 불일치          | 400 BAD REQUEST           |
| **Validation 실패** | `MethodArgumentNotValidException` | Controller 이전        | `@Valid` 검증 실패                          | 400 BAD REQUEST           |
| **예상하지 못한 예외**    | `Exception`                       | 전 구간                 | 매핑되지 않은 모든 런타임 예외 (버그/누락)               | 500 INTERNAL SERVER ERROR |



---

## 6. GlobalExceptionHandler 구현

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

## 8. @RestControllerAdvice 구현의 의미

이 구현의 핵심은 **예외의 발생 위치(Controller/Service/Repository)와 상관없이**
에러가 발생했을때 응답 변환은 항상 여기서 일어나도록 하는 것이다.

### 8-1. `@RestControllerAdvice`가 커버하는 범위

```java
@RestControllerAdvice
public class GlobalExceptionHandler { ... }
```

`@RestControllerAdvice`는 다음 의미를 가진다.

* **전역 적용 범위**: 기본적으로 *DispatcherServlet이 처리하는 모든 Controller 요청*에 대해 적용된다.

  * `@RestController`, `@Controller`, `@Service`, `@Repository` 모두 포함된다.
  * 즉, DispatcherServlet 아래에서 던져진 예외는 모두 커버한다.

* **처리 가능한 예외의 출처**

  * Controller 메서드 내부에서 `throw`된 예외
  * `@RequestBody` 변환/파싱 단계에서 발생한 예외
  * `@Valid` 검증 단계에서 발생한 예외
  * Controller에서 호출한 Service/Repository에서 위로 전파된 예외

* **커버하지 않는 영역(주의)**

  * DispatcherServlet까지 오지 않는 요청 흐름(예: 별도 서블릿, 별도 필터 체인에서 응답을 이미 작성한 경우)
  * Spring Security의 일부 예외처럼, Security Filter 단계에서 이미 처리(EntryPoint/AccessDeniedHandler)되어
    MVC 예외 처리까지 도달하지 않는 케이스

정리하면,

> `@RestControllerAdvice`는 **MVC(DispatcherServlet) 요청 처리 파이프라인 전체**에서
> 최종적으로 발생/전파된 예외를 받아 **JSON 응답 규칙으로 변환**하는 전역 핸들러다.

---

### 8-2. `@ExceptionHandler`의 의미

```java
@ExceptionHandler(DuplicateKeyException.class)
```

이 선언의 의미는 다음과 같다.

> Controller 처리 중 `DuplicateKeyException`이 발생하면
> 이 메서드가 해당 예외를 대신 처리한다.

Spring은 내부적으로 다음 흐름으로 동작한다.

```text
Controller 실행 중 예외 발생
  ↓
일치하는 @ExceptionHandler 탐색
  ↓
해당 메서드 호출
  ↓
반환값을 HTTP 응답으로 변환
```

따라서 각 `@ExceptionHandler` 메서드는
**특정 예외 타입 → HTTP 응답 규칙**을 매핑하는 역할을 한다.

---

### 8-3. 비즈니스 예외 (`ApiException`) 처리

```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e)
```

이 메서드는 **개발자가 의도적으로 던진 예외**를 처리한다.

특징:

* `ErrorCode`를 통해 HTTP 상태 코드를 결정한다
* 에러 코드를 기준으로 warn 로그를 1줄 남긴다
* 실패 응답은 항상 `ApiResponse.fail()`로 통일한다

즉,

> 비즈니스 규칙 위반은
> **예외를 던지고 → 전역에서 응답으로 변환**한다

라는 설계 원칙을 구현한 부분이다.

---

### 8-4. Spring 프레임워크 예외 처리

다음 예외들은 모두 **Controller 이전 단계**에서 발생할 수 있다.

* `HttpMessageNotReadableException`

  * 요청 바디 없음 / JSON 형식 오류 / 타입 불일치

* `MethodArgumentNotValidException`

  * `@Valid` 검증 실패

이 예외들의 공통점은:

* Service 비즈니스 로직과 무관한 “입력/제약” 문제인 경우가 많다
* Controller에서 try-catch로 일일이 처리하면 코드가 복잡해진다

따라서 이 예외들도 Global Exception Handler에서 **명시적으로 매핑**해
응답 구조를 통일한다.

---

### 8-5. 처리되지 않은 예외 (`Exception`)는 최후의 안전망

```java
@ExceptionHandler(Exception.class)
```

이 핸들러는 **예상하지 못한 예외**에 대한 마지막 방어선이다.

* 누락된 매핑
* 로직 버그
* 외부 연동 실패

이 경우에는:

* HTTP 500 응답
* 스택 트레이스를 포함한 error 로그 출력
* 클라이언트에는 내부 정보 노출 없이 일반 메시지만 반환

> 이 핸들러가 호출된다면
> **버그이거나 설계 누락**으로 판단해야 한다.

---

## 이 장의 핵심 정리

* 예외 처리는 전역에서 한 번만 한다
* Global Exception Handler는 응답 변환 지점이다
* Validation 예외는 Controller 이전 단계에서 발생한다
* 모든 실패 응답은 ApiResponse로 통일된다

---

## 다음 단계

→ [**요청 로깅 전략 (Filter 기반)**](04-request-logging-strategy.md)
