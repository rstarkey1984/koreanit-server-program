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

## 5. 공통 예외 / 에러 코드 정의

### ErrorCode (에러 코드 + HTTP 상태 코드 매핑)

```java
package com.koreanit.spring.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final @NonNull HttpStatusCode status;

    ErrorCode(@NonNull HttpStatus status) {
        this.status = status;
    }

    public @NonNull HttpStatusCode getStatus() {
        return status;
    }
}
```

> `@NonNull`은 이 값이 **null이 아님을 명시적으로 선언**하는 애너테이션이다

---

### ApiException (에러 코드 + 메시지를 담는 공통 예외)

```java
package com.koreanit.spring.error;

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

```java
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Integer checkConnection() {
        try {
            return userRepository.selectOne();
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(
                ErrorCode.USER_NOT_FOUND,
                "존재하지 않는 사용자입니다"
            );
        }
    }
}
```

* Service는 어떤 예외를 어떤 에러 코드로 바꿀지만 결정한다
* HTTP 응답 변환 책임은 Service에 없다

---

## 7. Controller는 호출만 담당한다

```java
@GetMapping("/db-check")
public ApiResponse<Integer> dbCheck() {
    return ApiResponse.ok(userService.checkConnection());
}
```

* Controller에는 try-catch가 없다
* 정상 흐름만 호출한다

---

## 8. enum 상수는 값이 아니라 객체다

`ErrorCode.INTERNAL_ERROR`처럼 보이는 enum 상수는
단순한 문자열이나 숫자가 아니라 **이미 생성된 객체 인스턴스**다.

```java
public enum Status {
    READY,
    RUNNING,
    DONE
}
```

### enum이 실제로 생성되는 방식

```java
public final class Status extends Enum<Status> {

    public static final Status READY   = new Status("READY", 0);
    public static final Status RUNNING = new Status("RUNNING", 1);
    public static final Status DONE    = new Status("DONE", 2);

    private Status(String name, int ordinal) {
        super(name, ordinal);
    }
}
```

* enum 상수는 객체다
* 애플리케이션 시작 시 한 번만 생성된다
* 각 상수는 JVM 전체에서 하나만 존재한다

### 왜 `==` 비교가 가능한가

```java
Status a = Status.READY;
Status b = Status.READY;

a == b  // true
```

* 동일한 객체 참조를 가리키기 때문이다

### 이 구조가 에러 코드에 적합한 이유

* 문자열보다 타입 안전하다
* 의미 + 데이터(HttpStatus)를 함께 가질 수 있다
* IDE 자동완성, 컴파일 타임 검증이 가능하다

> **에러 코드는 값이 아니라 의미를 가진 객체로 관리한다**

---

## 이 장의 핵심 정리

* 메시지와 에러 코드는 역할이 다르다
* 에러 코드는 객체로 생성된 실패 유형 식별자다
* 공통 예외를 사용하면 흐름이 단순해진다
* Controller는 예외를 처리하지 않는다

---

## 다음 단계

→ [**Global Exception Handler**](03-global_exception_handler.md)
