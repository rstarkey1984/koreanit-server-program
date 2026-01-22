# 에러 코드와 공통 예외

이 장에서는
서버에서 발생하는 실패 상황을 **에러 코드와 예외(Exception)** 로 어떻게 표현할지 기준을 정한다.

앞 장에서 공통 응답 포맷을 확정했다면,
이제는 **실패를 어떤 규칙으로 구분하고 전달할지**를 합의해야 한다.


---

## 1. 메시지로만 에러를 전달하면 생기는 문제

실패 응답을
문자열 메시지로만 전달하면 다음 문제가 생긴다.

* 메시지가 바뀌면 클라이언트 로직이 깨진다
* 언어/표현 변경에 취약하다
* 에러 유형을 코드로 분기하기 어렵다

예:

```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다"
}
```

> 사람에게는 메시지가 중요하지만,
> **프로그램에게는 식별자가 필요하다.**

---

## 2. 에러 코드의 역할

에러 코드는
**실패 유형을 식별하기 위한 고정된 값**이다.

에러 코드의 특징:

* 의미가 바뀌지 않는다
* 문자열 메시지와 분리된다
* 클라이언트 분기 처리에 사용된다

예:

```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다",
  "code": "USER_NOT_FOUND"
}
```

---

## 3. 에러 코드 설계 기본 원칙

에러 코드는
읽기 쉬우면서도 의미가 명확해야 한다.

권장 규칙:

* 대문자 + 언더스코어 사용
* 동사보다 상태 중심
* 너무 세분화하지 않는다

예:

* `INVALID_REQUEST`
* `USER_NOT_FOUND`
* `DUPLICATE_RESOURCE`
* `INTERNAL_ERROR`

---

## 4. 왜 공통 예외가 필요한가

에러 코드만으로는
서버 내부 흐름을 표현하기 어렵다.

그래서 서버에서는
**의미를 담은 예외 객체**를 사용한다.

공통 예외의 역할:

* 실패 원인을 코드로 표현
* 메시지를 함께 전달
* 상위 계층으로 안전하게 전달

---

## 5. 예외를 사용하는 기본 흐름

권장 흐름은 다음과 같다.

```text
Repository / Service
  ↓ (문제 발생)
의미 있는 예외 발생
  ↓
공통 예외 처리기에서 캐치
  ↓
에러 코드 + 메시지로 응답 변환
```

이 구조를 사용하면
Controller는 에러 처리 로직에서 완전히 벗어난다.

---

## 6. 예외와 HTTP 상태 코드의 관계

에러 코드와
HTTP 상태 코드는 다른 역할을 가진다.

* HTTP 상태 코드: 통신 표준
* 에러 코드: 프로젝트 내부 규칙

예:

| 상황    | HTTP 상태 | 에러 코드              |
| ----- | ------- | ------------------ |
| 요청 오류 | 400     | INVALID_REQUEST    |
| 자원 없음 | 404     | USER_NOT_FOUND     |
| 중복 요청 | 409     | DUPLICATE_RESOURCE |
| 서버 오류 | 500     | INTERNAL_ERROR     |


---

## 실습 목표

* 에러 코드를 enum으로 정의한다
* 공통 예외 클래스를 만들어 실패를 코드로 표현한다
* Service 계층에서 예외를 발생시키고 Controller는 이를 직접 처리하지 않는다

---

## 실습 준비

* 앞 장에서 만든 `ApiResponse` 클래스 존재
* 기본적인 Controller / Service 구조가 준비되어 있음

---

## 실습 과제

### 1단계: 에러 코드 enum 정의

> enum이란? 서로 관련 있는 상수들을 하나의 타입으로 묶은 것

예시 코드:

```java
package com.koreanit.spring.error;

public enum ErrorCode {
    INVALID_REQUEST,
    USER_NOT_FOUND,
    DUPLICATE_RESOURCE,
    INTERNAL_ERROR
}
```

조건:

* 문자열 에러 코드를 직접 사용하지 않는다
* 모든 실패 유형은 enum으로 표현한다

---

### 2단계: 공통 예외 클래스 생성

에러 코드와 메시지를 함께 담는 공통 예외 클래스를 만든다.

예시 코드:

```java
package com.koreanit.spring.error;

/**
 * 애플리케이션 전용 예외 클래스
 *
 * - 비즈니스 로직 처리 중 발생한 오류를 표현한다
 * - ErrorCode를 함께 담아 GlobalExceptionHandler로 전달한다
 * - RuntimeException을 상속하여 트랜잭션 롤백 및 전파가 가능하다
 */
public class ApiException extends RuntimeException {

    /** 에러의 종류를 나타내는 코드 */
    private final ErrorCode errorCode;

    /**
     * ApiException 생성자
     *
     * @param errorCode 에러 분류용 코드(enum)
     * @param message   클라이언트에게 전달할 에러 메시지
     */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);          // RuntimeException의 메시지 설정
        this.errorCode = errorCode;
    }

    /**
     * 에러 코드 반환
     *
     * GlobalExceptionHandler에서
     * HTTP 상태 코드 매핑에 사용된다
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

---

### 3단계: Service 계층에서 예외 사용

Service에서 실패 상황이 발생하면 공통 예외를 던진다.

예시 코드:

```java
public String findUser(Integer id) {
  if (id == 10) {
    throw new ApiException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다");
  }

  return "user";
}
```


### 4단계: Controller 계층에서 호출
```java
@GetMapping("/api/user/{id}")
public ApiResponse<String> getUser(@PathVariable Integer id) {
  return ApiResponse.ok(userService.findUser(id));
}
```

> @PathVariable은 URL 경로에 포함된 값을 Controller 메서드 파라미터로 받는다.

---

## 체크 포인트

* 에러 코드가 enum으로 관리되는가?
* 메시지와 실패 유형을 분리했는가?
* Controller에 try-catch 로직이 없는가?

---

## 이 장의 핵심 정리

* 메시지와 에러 코드는 역할이 다르다
* 에러 코드는 실패 유형의 식별자다
* 공통 예외를 사용하면 흐름이 단순해진다
* Controller는 예외를 처리하지 않는다

---

## 다음 단계

→ [**Global Exception Handler**](03-global_exception_handler.md)
