# Global Exception Handler

이 장에서는
서버 전역에서 발생하는 예외를 **한 곳에서 일관되게 처리하는 구조**를 설계한다.

앞 장에서
에러 코드와 공통 예외의 기준을 정했다면,
이제는 **그 예외들을 실제로 어디에서, 어떻게 응답으로 변환할지**를 결정한다.

---

## 1. 예외는 어디에서 처리되어야 하는가

예외는
발생 지점에서 처리되지 않는다.

다음과 같은 구조는 지양해야 한다.

```text
Controller에서 try-catch
Service에서 try-catch
Repository에서 try-catch
```

이 방식의 문제:

* 중복 코드 증가
* 응답 형식 불일치
* 예외가 삼켜져서 원인 추적이 어려움

> 예외는 발생 지점이 아니라
> **응답으로 변환되는 지점**에서 처리한다.

---

## 2. Global Exception Handler의 역할

Global Exception Handler는
서버 전역에서 발생한 예외를 가로채
**정해진 규칙에 따라 HTTP 응답으로 변환**한다.

역할 정리:

* 예외 유형 판별
* HTTP 상태 코드 결정
* 공통 에러 응답 생성
* 로그 기록(필요 시)

---

## 3. 예외 처리 흐름 한눈에 보기

```text
Controller / Service / Repository
  ↓ (예외 발생)
Global Exception Handler
  ↓
공통 에러 응답 생성
  ↓
클라이언트 응답
```

이 구조가 만들어지면
각 계층의 책임이 명확해진다.

---

## 4. Controller가 예외를 처리하지 않아야 하는 이유

Controller의 역할은
HTTP 요청을 받고 응답을 반환하는 것이다.

Controller가 예외를 처리하기 시작하면:

* 비즈니스 흐름이 섞이고
* 공통 규칙이 무너지고
* 유지보수가 어려워진다

그래서 Controller는
**예외를 던지기만 한다.**

---

## 5. 공통 예외 → 응답 변환 기준

Global Exception Handler는
다음 정보를 기준으로 응답을 만든다.

* 예외에 포함된 에러 코드
* 예외에 포함된 메시지
* 예외 유형

이를 바탕으로:

* HTTP 상태 코드 설정
* 공통 에러 응답 바디 생성

> 이 규칙은 프로젝트 전반에 동일하게 적용된다.

---

## 6. 지금 단계에서 하지 않는 것

이 장에서는 다음을 다루지 않는다.

* 프레임워크 애노테이션 상세 설명
* 예외 상속 구조 심화 설계

지금 단계의 목표는
**"예외 처리는 여기서 한다"라는 위치 감각**과,
성공/실패 응답 구조를 **하나로 통합하는 완성 단계**다.

---

## 이 장의 핵심 정리

* 예외 처리는 전역에서 한 번만 한다
* Global Exception Handler는 응답 변환 지점이다
* Controller는 예외를 던지고 책임에서 벗어난다
* 일관된 에러 응답은 구조에서 나온다

---

## 실습 목표

* 성공/실패 응답을 `ApiResponse` 한 가지 구조로 통일한다
* `ApiException`을 전역에서 받아 HTTP 응답으로 변환한다
* Controller에 try-catch 없이도 실패 응답이 동일한 포맷으로 내려가게 만든다

---

## 실습 준비

* 01장에서 만든 `ApiResponse<T>` 클래스
* 02장에서 만든 `ErrorCode`, `ApiException`
* 테스트용 Controller + Service 하나

---

## 실습 과제

### 1단계: ApiResponse를 실패까지 포함하도록 확장

성공/실패 모두 같은 DTO를 사용한다.

요구사항:

* 성공 응답: `success=true`, `message`, `data`
* 실패 응답: `success=false`, `message`, `code`

예시 코드:

```java
package com.example.api.common;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String code; // 실패 시 사용

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

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }
}
```

---

### 2단계: 전역 예외 핸들러 클래스 생성

`@RestControllerAdvice`를 사용해 예외를 잡고,
`ApiResponse.fail()`로 변환해 반환한다.

예시 코드:

```java
package com.example.api.error;

import com.example.api.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        HttpStatus status = mapStatus(e.getErrorCode());

        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), "서버 오류"));
    }

    private HttpStatus mapStatus(ErrorCode code) {
        switch (code) {
            case INVALID_REQUEST:
                return HttpStatus.BAD_REQUEST;
            case USER_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case DUPLICATE_RESOURCE:
                return HttpStatus.CONFLICT;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
```

포인트:

* Controller는 예외를 처리하지 않는다
* 상태 코드는 `ErrorCode`에 의해 결정된다

---

### 3단계: 동작 확인 (성공/실패 응답 비교)

예시 Controller:

```java
package com.example.api.controller;

import com.example.api.common.ApiResponse;
import com.example.api.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}")
    public ApiResponse<String> getUser(@PathVariable Long id) {
        String name = userService.findUser(id);
        return ApiResponse.ok(name);
    }
}
```

테스트:

* `/api/users/1` 호출 → 404 + 실패 응답(통일된 포맷)
* 성공 케이스를 만들고 싶다면 `UserService.findUser()`에 임시 성공 분기 추가

---

## 체크 포인트

* 성공/실패 모두 `ApiResponse` 한 가지 구조로 내려오는가?
* Controller 코드에 try-catch가 없는가?
* `ApiException`은 전역 핸들러에서만 응답으로 변환되는가?
* HTTP 상태 코드가 에러 코드와 일관되게 매핑되는가?

---

## 다음 단계

→ [**요청 로깅 전략**](/docs/03-common-modules/04-request_logging.md)
