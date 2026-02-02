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

> 상태 코드는 전세계 "표준"이고,
> 바디 포맷은 우리가 사용하는 "프로젝트 규칙"이다.

---

## 4. 공통 응답 DTO 설계 (개념)

공통 포맷을 코드로 고정하려면
DTO(응답 객체)를 만든다.

예:

* ApiResponse<T>

  * success
  * message
  * data

---

## 5. Controller는 공통 포맷만 반환한다

Controller는
자유로운 응답을 만들지 않는다.

* 성공이면 `ApiResponse`로 감싼다
* 실패는 Service 단계에서 의미있는 예외로 던지고 공통 예외 처리기에 맡긴다

이 원칙이 지켜지면
API 수가 늘어나도 응답 품질이 유지된다.

---

## 실습 목표

* 모든 API가 동일한 응답 구조를 반환하도록 만든다
* Controller에서 직접 객체나 값을 return 하지 않고, 공통 응답 객체를 사용한다

---

## 실습 과제

### 1단계: 공통 응답 클래스 생성

다음 필드를 가지는 공통 응답 클래스를 설계한다.

* success (boolean)
* message (String)
* data (Generic)

조건:

* 모든 정상 응답은 해당 클래스를 사용한다
* Controller에서 Map 또는 직접 JSON 문자열을 생성하지 않는다

예시 코드:

```java
package com.koreanit.spring.common.response;

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String code;   // 실패 식별자

    private ApiResponse(boolean success, String message, T data, String code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getCode() { return code; }

    /* ---------- 성공 ---------- */

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "OK", null, null);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /* ---------- 실패 ---------- */

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, message, null, code);
    }
}
```


### 제네릭 타입 파라미터

`ApiResponse<T>`에서 `<T>`는 **응답 data의 타입을 고정하지 않고 상황에 따라 바꾸기 위한 장치**다.

예를 들어 API마다 `data`에 들어가는 타입이 다르다.

```java
ApiResponse<String>
ApiResponse<Integer>
ApiResponse<UserDto>
ApiResponse<List<UserDto>>
```

공통 응답의 "형태"는 유지하면서도,
각 API의 "결과 데이터" 타입을 정확히 담을 수 있다.

---
### 2단계: HelloController 응답 공통 모듈 적용

기존코드
```java
@GetMapping("/hello/users")
public List<UserResponse> users(@RequestParam(defaultValue = "1000") int limit) {
    return UserMapper.toResponseList(helloService.users(limit));
}

@GetMapping("/hello/users/{id}")
public UserResponse user(@PathVariable Long id) {
    return UserMapper.toResponse(helloService.user(id));
}

@GetMapping("/hello/posts")
public List<PostResponse> posts(@RequestParam(defaultValue = "1000") int limit) {
    return PostMapper.toResponseList(helloService.posts(limit));
}

@GetMapping("/hello/posts/{id}")
public PostResponse post(@PathVariable Long id) {
    return PostMapper.toResponse(helloService.post(id));
}
```
변경코드
```java
@GetMapping("/hello/users")
public ApiResponse<List<UserResponse>> users(@RequestParam(defaultValue = "1000") int limit) {
    return ApiResponse.ok(UserMapper.toResponseList(helloService.users(limit)));
}

@GetMapping("/hello/users/{id}")
public ApiResponse<UserResponse> user(@PathVariable Long id) {
    return ApiResponse.ok(UserMapper.toResponse(helloService.user(id)));
}

@GetMapping("/hello/posts")
public ApiResponse<List<PostResponse>> posts(@RequestParam(defaultValue = "1000") int limit) {
    return ApiResponse.ok(PostMapper.toResponseList(helloService.posts(limit)));
}

@GetMapping("/hello/posts/{id}")
public ApiResponse<PostResponse> post(@PathVariable Long id) {
    return ApiResponse.ok(PostMapper.toResponse(helloService.post(id)));
}
```

변경후 응답:
```
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": 10098,
      "username": "admin",
      "email": "admin@test.com",
      "nickname": "관리자(admin)",
      "createdAt": "2026-02-01T03:32:50"
    }
  ],
  "code": null
}
```

---

## 다음 단계

→ [**에러 코드와 공통 예외**](02-error_codes_and_exceptions.md)
