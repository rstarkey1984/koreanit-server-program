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
* `NOT_FOUND_RESOURCE`
* `DUPLICATE_RESOURCE`
* `INTERNAL_ERROR`

---

## 4. 왜 공통 예외가 필요한가

에러 코드는 **실패 유형의 식별자**지만,
서버 내부에서는 “어디서/왜 실패했는지”를 **흐름으로 전달**해야 한다.

만약 예외 없이 “실패 결과를 리턴”으로 처리하면,

* Service가 실패 여부를 매번 if로 확인해야 하고
* Controller도 실패 분기 로직을 갖게 되며
* 계층 간 책임 분리가 무너진다

그래서 서버는 실패를 **예외(Exception)** 로 표현하고,
에러 코드와 메시지를 함께 담은 **공통 예외(ApiException)** 를 표준으로 사용한다.

### 4-1. 공통 예외를 쓰면 무엇이 좋아지나

공통 예외를 쓰면 아래가 한 번에 정리된다.

* 실패 유형: `ErrorCode` (고정 식별자)
* 실패 메시지: `Exception message` (클라이언트 응답용 문장)
* HTTP 상태 코드: `ErrorCode → HttpStatus 매핑`
* 처리 위치: `GlobalExceptionHandler` 한 곳으로 통일

즉,

> Service는 “실패를 발견하면 던지고(throw)”,
> Controller는 “정상 흐름만 호출한다.”
> 실패 응답 변환은 Global Handler가 담당한다.

---

## 5. 에러 코드 / 공통 예외 정의

## ErrorCode (에러 코드 + HTTP 상태 코드 매핑)

```java
package com.koreanit.spring.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST),   // 400
    NOT_FOUND_RESOURCE(HttpStatus.NOT_FOUND),  // 404
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),   // 409
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),      // 401 
    FORBIDDEN(HttpStatus.FORBIDDEN),            // 403 
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR); // 500

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

---

## enum 상수는 값이 아니라 객체다

`ErrorCode.INVALID_REQUEST` 처럼 보이는 enum 상수는 코드에서는 이름으로 표현되지만, 실행 시점에는 JVM이 생성한 단일 객체 인스턴스를 참조한다.

* 애플리케이션 시작 시 한 번만 생성된다
* 각 상수 객체는 JVM 전체에서 하나만 존재한다

### 왜 `==` 비교가 가능한가

```java
ErrorCode a = ErrorCode.INVALID_REQUEST;
ErrorCode b = ErrorCode.INVALID_REQUEST;

// true
boolean same = (a == b);
```

동일한 enum 상수는
항상 **같은 객체 참조**를 가리키기 때문이다.

> **에러 코드는 값이 아니라 의미를 가진 객체로 관리한다**

## 상태 코드 예시 정리

| ErrorCode            |                HTTP 상태 코드 | 언제 쓰나 (예시)   | 대표 상황 예시                                            |
| -------------------- | ------------------------: | ------------ | --------------------------------------------------- |
| `INVALID_REQUEST`    |           400 Bad Request | 요청 형식/값이 잘못됨 | 필수 파라미터 누락, 타입 불일치, 허용 범위 밖 값, JSON 파싱 실패           |
| `NOT_FOUND_RESOURCE` |             404 Not Found | 대상 리소스가 없음   | `GET /users/999` 조회 결과 없음, `DELETE /posts/10` 대상 없음 |
| `DUPLICATE_RESOURCE` |              409 Conflict | 유니크/중복 충돌    | 회원가입 시 `username` 중복, 이미 존재하는 값으로 변경 시도             |
| `INTERNAL_ERROR`     | 500 Internal Server Error | 서버 내부 처리 실패  | 예상하지 못한 예외, 외부 연동 실패, 로직 버그 등(일반적으로 공통 처리)          |


---

## ApiException (에러 코드 + 메시지를 담는 공통 예외)

```java
package com.koreanit.spring.common.error;

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

* 메시지는 **클라이언트 응답용 문장**이다
* 로그 상세 정보는 로깅 단계에서 별도로 남긴다

---

## 6. Service에서 의미 있는 실패를 예외로 표현

Repository가 던진 **데이터 접근 예외**를
Service 계층에서 **비즈니스 의미의 예외**로 변환한다.

```java
public User getUser(Long id) {
    try {
        return userRepository.findById(id);
    } catch (EmptyResultDataAccessException e) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 사용자입니다. id=" + id
        );
    }
}
```

```java
public PostRow getPost(Long id) {
    try {
        return postRepository.findById(id);
    } catch (EmptyResultDataAccessException e) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 게시글입니다. id=" + id
        );
    }
}
```

* Service는 **어떤 실패를 어떤 에러 코드로 변환할지만 결정**한다
* HTTP 응답 변환 책임은 Service에 없다

---

## 7. Controller는 호출만 담당한다

* Controller에는 try-catch가 없다
* 정상 흐름만 호출한다

---

## 이 장의 핵심 정리

* 메시지와 에러 코드는 역할이 다르다
* 에러 코드는 객체로 생성된 실패 유형 식별자다
* 공통 예외를 사용하면 흐름이 단순해진다
* Controller는 예외를 처리하지 않는다

---

## 다음 단계

→ [**Global Exception Handler**](03-global_exception_handler.md)