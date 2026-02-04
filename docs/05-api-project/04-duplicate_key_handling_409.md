# 중복 제약 해석 (409) – ErrorCode + ApiException

이 문서는 Users CRUD에서 **UNIQUE 제약 위반(중복)** 을
**ErrorCode + ApiException 구조**로 해석하여 **409 Conflict** 로 처리하는 기준을 정의한다.

이전 단계까지 다음 실패 유형을 계층별로 고정했다.

* 요청 바디 누락 / JSON 파싱 실패 → 400 (`GlobalExceptionHandler`)
* 요청 값 판단 실패(`@Valid`) → 400 (`GlobalExceptionHandler`)
* 대상 없음 → 404 (`ErrorCode.NOT_FOUND_RESOURCE`)

본 단계에서는 **요청과 형식은 정상이나, 이미 존재하는 값으로 인해 실패하는 경우**를 다룬다.

---

## 이 단계의 핵심 메시지

> DB 기술 예외(`DuplicateKeyException`)를 Service에서 **업무 의미(409)** 로 변환한다.

* 중복은 409 (Conflict)
* 어떤 값이 중복인지 판단하는 책임은 **Service**
* HTTP 상태 변환과 응답 포맷은 **GlobalExceptionHandler**

---

## 1. ErrorCode 정의 확인 (409)

파일: `common/error/ErrorCode.java`

#### 파일 역할

* API 전반에서 사용할 표준 에러 코드를 정의한다.
* 각 에러 코드는 HTTP 상태와 1:1로 매핑된다.

```java
DUPLICATE_RESOURCE(HttpStatus.CONFLICT), // 409
```

---

## 2. GlobalExceptionHandler 처리 흐름

`ApiException`은 이미 전역 예외 처리기에 의해 공통 응답 형식으로 변환된다.
본 단계에서는 **추가적인 예외 매핑을 정의하지 않는다.**

```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
    ...
}
```

---

## 3. Service에서 DuplicateKeyException 해석

Repository는 여전히 **DB 접근만 담당**한다.
중복 여부 판단과 의미 해석은 **Service의 책임**이다.

---

### 3-1. 중복 메시지 변환 로직

파일: `user/UserService.java`

#### 적용 목적

* DB/드라이버별 메시지 차이를 Service 내부로 한정한다.
* 어떤 컬럼이 중복되었는지를 사용자 메시지로 변환한다.

```java
private String toDuplicateMessage(DuplicateKeyException e) {
    String m = (e.getMessage() == null) ? "" : e.getMessage();

    // MySQL 기준: "Duplicate entry ... for key '...'"
    // DB/드라이버에 따라 메시지 포맷은 달라질 수 있으므로
    // key 이름 기반으로만 판단한다.
    if (m.contains("for key") && (m.contains("users.username") || m.contains("'username'") || m.contains("username"))) {
        return "이미 존재하는 username입니다";
    }
    if (m.contains("for key") && (m.contains("users.email") || m.contains("'email'") || m.contains("email"))) {
        return "이미 존재하는 email입니다";
    }

    return "이미 존재하는 값입니다";
}
```

---

### 3-2. 회원가입(create) 중복 처리

파일: `user/UserService.java`

#### 처리 기준

* 형식 검증은 02단계에서 완료된 상태를 전제로 한다.
* Repository 호출 중 발생한 `DuplicateKeyException`을 잡아
  `ApiException(ErrorCode.DUPLICATE_RESOURCE, ...)`로 변환한다.

```java
// 정상 흐름: 회원가입 → PK 반환
public Long create(UserCreateRequest req) {
  String username = req.getUsername().trim().toLowerCase();
  String nickname = req.getNickname().trim().toLowerCase();
  String email = req.getEmail().trim().toLowerCase();
  String hash = passwordEncoder.encode(req.getPassword());

  try {
    return userRepository.save(username, hash, nickname, email);
  } catch (DuplicateKeyException e) {
    throw new ApiException(
        ErrorCode.DUPLICATE_RESOURCE,
        toDuplicateMessage(e));
  }
}
```

> 메시지 파싱 방식은 DB/드라이버 설정에 따라 달라질 수 있다.
> 본 단계의 목적은 **중복 실패를 409로 해석하는 구조를 고정**하는 것이다.

---

## 4. 테스트

파일: `409.http`

#### 파일 역할

* UNIQUE 제약 위반 시 409 응답이 반환되는지 확인한다.
* 중복 필드(username/email)에 따라 메시지가 달라지는지 검증한다.

```http
@baseUrl = http://localhost:8080

### Step1 - username 중복 (409)
POST {{baseUrl}}/api/users
Content-Type: application/json

{
  "username": "test1",
  "password": "1234",
  "nickname": "테스트",
  "email": "t1_new@test.com"
}

### Step2 - email 중복 (409)
POST {{baseUrl}}/api/users
Content-Type: application/json

{
  "username": "user1",
  "password": "1234",
  "nickname": "테스트",
  "email": "test1@test.com"
}
```

---

## 체크리스트

* UNIQUE 제약 위반은 409로 처리한다.
* `DuplicateKeyException`은 Service에서 해석한다.
* 실패 유형은 `ErrorCode`로 구분한다.
* 예외 표현은 `ApiException`으로 통일한다.

---

## 다음 단계

→ [**인증 (401) — Session + Spring Security**](05-session_authentication_401.md)
