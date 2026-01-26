# Service 회원가입 로직 구현

이 문서에서는 `UserService.signup()`을 **실제 비즈니스 로직**으로 완성한다.

핵심 목표:

* Service가 **검증 + 흐름 제어 + 예외 변환**을 담당
* Repository는 **데이터 접근만** 수행
* 실패는 `ApiException`으로 던지고, 응답 변환은 `GlobalExceptionHandler`가 수행

---

## 1. 이미 준비된 구조

* `ApiResponse<T>` 공통 응답 포맷
* `ErrorCode`, `ApiException`
* `GlobalExceptionHandler` (`@RestControllerAdvice`)
* Repository 메서드 뼈대

  * `countByUsername(String username)`
  * `insertUser(String username, String password, String nickname, String email)`

---

## 2. Service가 책임지는 것

### Service에서 처리

* 필수값 검증(요청값 오류)
* 중복 체크(비즈니스 규칙)
* 저장 흐름 통제(Repository 호출 순서)
* 실패를 `ApiException`으로 통일

### Service에서 처리하지 않음

* HTTP 응답 생성(ApiResponse)
* Service는 HTTP 상태 코드를 직접 다루지 않는다. 대신 실패의 “의미”만 ErrorCode로 표현한다. HTTP 상태 코드는 전역 예외 처리기에서 매핑된다.
* SQL 작성

---

## 3. 회원가입 규칙

* username: 필수, 공백 불가
* password: 필수, 공백 불가
* nickname: 필수, 공백 불가
* email: 선택

### 검증 실패 처리

* `ApiException(ErrorCode.INVALID_REQUEST, "요청값 오류")`

### 중복 처리

* `ApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 아이디입니다")`

---

## 4. UserService.signup() 구현

아래 코드는 **Service 메서드만** 기준으로 제공한다.

```java
public UserSignupResponse signup(UserSignupRequest req) {

    // 1) 필수값 검증
    if (req == null) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "요청 바디가 없습니다");
    }

    if (isBlank(req.username)) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "username은 필수입니다");
    }

    if (isBlank(req.password)) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "password는 필수입니다");
    }

    if (isBlank(req.nickname)) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "nickname은 필수입니다");
    }

    // 2) 중복 체크
    int duplicated = userRepository.countByUsername(req.username);
    if (duplicated > 0) {
        throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 아이디입니다");
    }

    // 3) 저장
    long userId = userRepository.insertUser(
        req.username,
        req.password,
        req.nickname,
        req.email
    );

    // 4) 응답 DTO 생성
    return new UserSignupResponse(
        userId,
        req.username,
        req.nickname
    );
}

private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
}
```

* 검증 실패/중복 실패 모두 `ApiException`으로 통일

* Controller에 try-catch 없음

* 실패 응답 변환은 `GlobalExceptionHandler`가 수행

---

## 5. 동작 흐름

### 성공

* Controller → Service → Repository 저장
* Service가 `UserSignupResponse` 반환
* Controller가 `ApiResponse.ok("회원가입 완료", UserSignupResponse)` 반환

### 실패

* Service에서 `ApiException` 발생
* `GlobalExceptionHandler`가 잡아서

  * HTTP 상태 코드 결정(`ErrorCode.getStatus()`)
  * `ApiResponse.fail(code, message)`로 변환

---

## 체크 포인트

* Service에서만 `ApiException`을 생성하는가
* Controller에 try-catch가 없는가
* 중복 체크가 저장보다 먼저 실행되는가
* 실패 응답의 `code`가 `ErrorCode`와 일치하는가

---

## 다음 단계

[**04. VS Code REST Client 사용법**](04-vscode_rest_client.md)