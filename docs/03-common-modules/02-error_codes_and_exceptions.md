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

## 7. 지금 단계에서 하지 않는 것

이 장에서는 다음을 다루지 않는다.

* 예외 클래스 상속 구조 설계
* 체크 예외 vs 언체크 예외 논쟁
* 에러 코드 전체 목록 정의

지금 단계의 목표는
**에러 처리의 방향과 기준을 잡는 것**이다.

---

## 이 장의 핵심 정리

* 메시지와 에러 코드는 역할이 다르다
* 에러 코드는 실패 유형의 식별자다
* 공통 예외를 사용하면 흐름이 단순해진다
* Controller는 예외를 처리하지 않는다

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

에러 코드를 하나의 enum으로 관리한다.

예시 코드:

```java
package com.example.api.error;

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
package com.example.api.error;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

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
package com.example.api.service;

import com.example.api.error.ApiException;
import com.example.api.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public String findUser(Long id) {
        if (id == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "잘못된 요청입니다");
        }

        throw new ApiException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다");
    }
}
```

---

## 체크 포인트

* 에러 코드가 enum으로 관리되는가?
* 메시지와 실패 유형을 분리했는가?
* Controller에 try-catch 로직이 없는가?

---

## 다음 단계

→ [**Global Exception Handler**](03-global_exception_handler.md)
