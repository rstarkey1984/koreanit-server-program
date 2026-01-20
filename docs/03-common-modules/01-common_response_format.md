# 공통 응답 포맷

이 장에서는
API 응답을 **하나의 공통 포맷으로 통일**하는 기준을 만든다.

응답 포맷이 통일되면
프론트엔드/클라이언트는 일관된 방식으로 성공/실패를 처리할 수 있고,
서버는 에러 처리와 로깅을 표준화할 수 있다.


---

## 1. 왜 응답 포맷을 통일해야 하는가

API를 여러 개 만들기 시작하면
응답 형식이 쉽게 흔들린다.

예:

* 어떤 API는 `{ "data": ... }`
* 어떤 API는 `{ "result": ... }`
* 어떤 API는 성공/실패 필드가 없다

이 상태가 되면
클라이언트는 API마다 다른 처리를 해야 한다.

> 응답 포맷 통일은
> 서버-클라이언트 계약(contract)을 만드는 일이다.

---

## 2. 공통 응답의 최소 기준

권장 최소 필드:

### 성공 응답

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

### 실패 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "code": "ERROR_CODE"
}
```

설계 포인트:

* `success`: 성공/실패를 한 번에 판단
* `message`: 사용자/개발자에게 설명
* `data`: 성공 결과(없으면 null 가능)
* `code`: 실패 시 클라이언트 분기 처리용

---

## 3. HTTP 상태 코드와의 관계

응답 바디의 `success`와 별개로,
HTTP 상태 코드도 함께 일관되게 사용해야 한다.

예:

* 200 OK: 정상 응답
* 201 Created: 생성 성공
* 400 Bad Request: 요청값 오류
* 404 Not Found: 자원 없음
* 500 Internal Server Error: 서버 내부 오류

> 상태 코드는 "표준"이고,
> 바디 포맷은 "프로젝트 규칙"이다.

---

## 4. 공통 응답 DTO 설계 (개념)

공통 포맷을 코드로 고정하려면
DTO(응답 객체)를 만든다.

예:

* ApiResponse<T>

  * success
  * message
  * data

* ErrorResponse

  * success
  * message
  * code

> 실제 클래스 구현은
> 다음 장의 예외/핸들러와 함께 완성하는 것이 효율적이다.

---

## 5. Controller는 공통 포맷만 반환한다

Controller는
자유로운 응답을 만들지 않는다.

* 성공이면 `ApiResponse`로 감싼다
* 실패는 예외로 던지고 공통 처리기에 맡긴다

이 원칙이 지켜지면
API 수가 늘어나도 응답 품질이 유지된다.

---

## 실습 목표

* 모든 API가 동일한 응답 구조를 반환하도록 만든다
* Controller에서 직접 JSON을 생성하지 않고 공통 응답 객체를 사용한다

---

## 실습 준비

* Spring Boot 프로젝트가 실행 가능한 상태
* 테스트용 Controller 1개 이상 존재

예:

* `/api/health`
* `/api/test`

---

## 실습 과제

### 1단계: 공통 응답 클래스 생성

다음 필드를 가지는 공통 응답 클래스를 설계한다.

* success (boolean)
* message (String)
* data (Generic 또는 Object)

조건:

* 모든 정상 응답은 해당 클래스를 사용한다
* Controller에서 Map 또는 직접 JSON 문자열을 생성하지 않는다

예시 코드:

```java
package com.example.api.common;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
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
}
```

---

### 2단계: Controller 응답 통일

기존 Controller 코드를 수정하여
정상 응답 시 항상 공통 응답 객체를 반환하도록 변경한다.

요구사항:

* HTTP 상태 코드는 기존 의미에 맞게 유지한다
* 응답 바디 구조는 모든 API에서 동일해야 한다

예시 코드:

```java
package com.example.api.controller;

import com.example.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Void> health() {
        return ApiResponse.ok(null);
    }
}
```

---

### 3단계: 실패 응답 설계 (개념)

아직 예외 처리는 구현하지 않는다.

다음 질문에 답해본다.

* 실패 응답에는 어떤 필드가 더 필요할까?
* 클라이언트는 어떤 값을 기준으로 분기 처리할까?

---

## 체크 포인트

* 모든 API 응답 구조가 동일한가?
* Controller마다 응답 포맷이 달라지지 않는가?
* HTTP 상태 코드와 응답 바디의 역할을 구분했는가?

---

## 다음 단계

→ [**에러 코드와 공통 예외**](02-error_codes_and_exceptions.md)
