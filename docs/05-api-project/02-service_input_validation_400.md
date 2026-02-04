# 입력 검증 (400) — DTO(@Valid) + Service(정규화)

이 문서는 Users CRUD API에서 **입력 검증 책임을 계층별로 분리**하여 적용하는 기준을 정의한다.
입력 검증은 두 단계로 나뉘며, 각 단계는 명확히 다른 책임을 가진다.

* **1차 검증 (요청 값 판단)**: Request DTO + `@Valid`
* **2차 검증 (의미 해석)**: Service

본 단계(02)에서는 **요청 값 자체의 오류에 대한 400 처리만을 고정**한다.
대상 없음(404), 중복(409), 인증/인가(401/403)는 이후 단계에서 분리하여 다룬다.

---

## 0. 준비

### 0-1) Validation 의존성 확인

Spring Boot 3 환경에서 Bean Validation을 사용하기 위해 다음 의존성이 필요하다.

`build.gradle`

```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

---

### 0-2) GlobalExceptionHandler 확인

본 프로젝트는 이미 다음 예외를 **400 (INVALID_REQUEST)** 로 통일 처리한다.

* `HttpMessageNotReadableException`

  * 요청 바디 누락
  * JSON 파싱 실패
* `MethodArgumentNotValidException`

  * `@Valid` 검증 실패

---

## 1. Request DTO에 요청 값 판단 규칙 고정

요청 값 판단은 **요청 데이터 자체가 올바른지 여부만**을 다룬다.
비즈니스 의미는 해석하지 않는다.

판단 대상은 다음으로 제한한다.

* 필수 여부 (null / blank)
* 길이 범위
* 간단한 형식 (이메일 등)

---

### 1-1) 회원가입 요청 DTO

파일: `user/dto/request/UserCreateRequest.java`

#### 파일 역할

* 회원가입 요청에 필요한 입력 값을 정의한다.
* 요청 값에 대한 필수 조건, 길이 제한, 형식 판단 규칙을 선언한다.
* 선택 값(email)에 대해 입력 표준화를 수행한다.

#### 적용 규칙

* `username`, `password`, `nickname`

  * 필수
  * 길이 제한
* `email`

  * 선택 값
  * 형식만 판단
* 선택 필드(`email`)는 DTO setter 단계에서 `trim` 후 빈 문자열을 `null`로 정규화한다
  → **빈 문자열과 미입력을 동일한 의미(null)로 일관되게 취급하기 위함**

```java
package com.koreanit.spring.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {

    @NotBlank(message = "username은 필수입니다")
    @Size(min = 4, max = 20, message = "username은 4~20자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "username에는 공백을 포함할 수 없습니다")
    private String username;

    @NotBlank(message = "password는 필수입니다")
    @Size(min = 4, max = 50, message = "password는 4~50자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "password에는 공백을 포함할 수 없습니다")
    private String password;

    @NotBlank(message = "nickname은 필수입니다")
    @Size(min = 2, max = 20, message = "nickname은 2~20자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "nickname에는 공백을 포함할 수 없습니다")
    private String nickname;

    @Email(message = "email 형식이 올바르지 않습니다")
    private String email;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        if (email == null) {
            this.email = null;
            return;
        }
        String v = email.trim();
        this.email = v.isEmpty() ? null : v;
    }
}
```

---

### 1-2) 로그인 요청 DTO

파일: `user/dto/request/UserLoginRequest.java`

#### 파일 역할

* 로그인 요청에 필요한 사용자 식별 정보와 인증 정보를 정의한다.
* 최소한의 필수 조건과 길이 제한만을 판단한다.
* 인증 성공 여부 판단은 Service 또는 Security 계층에서 수행한다.

```java
package com.koreanit.spring.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserLoginRequest {

    @NotBlank(message = "username은 필수입니다")
    @Size(min = 4, max = 20, message = "username은 4~20자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "username에는 공백을 포함할 수 없습니다")
    private String username;

    @NotBlank(message = "password는 필수입니다")
    @Size(min = 4, max = 50, message = "password는 4~50자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "password에는 공백을 포함할 수 없습니다")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 1-3) 비밀번호 변경 요청 DTO

파일: `user/dto/request/UserPasswordChangeRequest.java`

#### 파일 역할

* 비밀번호 변경 요청에 필요한 입력 값만을 정의한다.
* 비밀번호에 대한 필수 조건과 길이 제한을 고정한다.
* 비즈니스 규칙(암호화 등)은 포함하지 않는다.

```java
package com.koreanit.spring.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserPasswordChangeRequest {

    @NotBlank(message = "password는 필수입니다")
    @Size(min = 4, max = 50, message = "password는 4~50자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "password에는 공백을 포함할 수 없습니다")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

---

### 1-4) 닉네임 변경 요청 DTO

파일: `user/dto/request/UserNicknameChangeRequest.java`

#### 파일 역할

* 비밀번호 변경 요청 데이터를 전달한다.
* 실제 암호화 및 저장은 Service 계층에서 수행된다.

```java
package com.koreanit.spring.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserNicknameChangeRequest {

    @NotBlank(message = "nickname은 필수입니다")
    @Size(min = 2, max = 20, message = "nickname은 2~20자여야 합니다")
    @Pattern(regexp = "^[^\\s]+$", message = "nickname에는 공백을 포함할 수 없습니다")
    private String nickname;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
```

---

## 2. Controller에서 @Valid 적용

파일: `user/UserController.java`

#### 파일 역할

* HTTP 요청을 받아 Request DTO로 매핑한다.
* 요청 값 판단을 `@Valid`를 통해 Spring에 위임한다.
* Service 호출 결과를 공통 응답 형식(ApiResponse)으로 반환한다.

Controller는 요청 값 판단 실패 시 직접 처리하지 않으며,
예외는 `GlobalExceptionHandler`로 전달된다.

```java
import jakarta.validation.Valid;

@PostMapping
public ApiResponse<Long> create(@Valid @RequestBody UserCreateRequest req) {
    return ApiResponse.ok(userService.create(req));
}

@PutMapping("/{id}/password")
public ApiResponse<Void> changePassword(
        @PathVariable Long id,
        @Valid @RequestBody UserPasswordChangeRequest req
) {
    userService.changePassword(id, req);
    return ApiResponse.ok();
}
```

---

## 3. Service는 정규화 및 정상 흐름 로직만 담당

파일: `user/UserService.java`

#### 파일 역할

* 검증이 완료된 요청 값을 기준으로 비즈니스 흐름을 실행한다.
* 입력 값은 변형하지 않으며, 보안 규칙 적용 및 저장·조회 흐름을 담당한다.
* 요청 값 자체의 적합성 판단은 수행하지 않는다.

Service는 다음을 전제로 동작한다.

> Controller를 통과한 요청 값은
> 이미 요청 값 판단이 완료된 상태이다.

```java
public Long create(UserCreateRequest req) {
    String username = req.getUsername().trim().toLowerCase();
    String nickname = req.getNickname().trim().toLowerCase();
    String email = req.getEmail().trim().toLowerCase();
    String hash = passwordEncoder.encode(req.getPassword());

    return userRepository.save(username, hash, nickname, email);
}
```

```java
public void changeNickname(Long id, UserNicknameChangeRequest req){
  String nickname = req.getNickname().trim().toLowerCase();  
  ...
}
```

---

## 4. REST 테스트 (400 실패 케이스)

파일: `400.http`

#### 파일 역할

* 요청 값 판단 실패 케이스를 명시적으로 검증한다.
* DTO + Validation 규칙이 정상적으로 동작하는지 확인한다.
* 400 응답 및 에러 코드 통일 여부를 점검한다.

```http
@baseUrl = http://localhost:8080

### username 누락 (400)
POST {{baseUrl}}/api/users
Content-Type: application/json

{
  "password": "1234",
  "nickname": "테스트",
  "email": "test@test.com"
}


### username 길이 오류 (400)
POST {{baseUrl}}/api/users
Content-Type: application/json

{
  "username": "ab",
  "password": "1234",
  "nickname": "테스트",
  "email": "test@test.com"
}

### password 너무 짧음 (400)
PUT {{baseUrl}}/api/users/10060/password
Content-Type: application/json

{
  "password": "12"
}
```

---

## 체크리스트

* 요청 값 판단(필수/길이/형식)은 **Request DTO + `@Valid`** 에서 처리한다
* 요청 값 판단 실패는 Spring 예외로 발생하며,
  `GlobalExceptionHandler` 가 **400 + INVALID_REQUEST** 로 통일한다
* Service는 요청 값 판단을 수행하지 않고,
  **정규화 및 정상 흐름 로직만 담당한다

---


# 실습 — 이메일 변경 API (입력 검증 400)

이 실습은 **입력 검증 책임 분리(400)** 기준을 이메일 변경 API에 적용하는 것이다.
본 실습에서는 **요청 값 오류(400)** 까지만 다루며,
대상 없음(404), 중복(409), 인증/인가(401/403)는 처리하지 않는다.

---

## 실습 목표

* Request DTO + `@Valid`로 **요청 값 판단 규칙을 고정**한다
* Service는 **정규화(trim / lowercase)** 와 정상 흐름만 담당한다
* 요청 값 판단 실패는 **GlobalExceptionHandler에서 400으로 통일**된다

---

## 1. Request DTO 수정

파일: `user/dto/request/UserEmailChangeRequest.java`

### 역할

* 이메일 변경 요청 값을 전달한다
* 이메일 **형식만 판단**한다
* 빈 문자열과 미입력을 동일하게 `null`로 정규화한다

```java
package com.koreanit.spring.dto.request;

import jakarta.validation.constraints.Email;

public class UserEmailChangeRequest {

    @Email(message = "email 형식이 올바르지 않습니다")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null) {
            this.email = null;
            return;
        }
        String v = email.trim();
        this.email = v.isEmpty() ? null : v;
    }
}
```

---

## 2. Controller 엔드포인트에 @Valid 추가

파일: `user/UserController.java`

### 역할

* `@Valid`로 요청 값 판단을 Spring에 위임한다

```java
@PutMapping("/{id}/email")
public ApiResponse<Void> changeEmail(
        @PathVariable Long id,
        @Valid @RequestBody UserEmailChangeRequest req
) {
    userService.changeEmail(id, req);
    return ApiResponse.ok();
}
```

---

## 3. Service 메서드 수정

파일: `user/UserService.java`

### 역할

* 이메일을 정규화하여 저장 흐름을 호출한다

```java
public void changeEmail(Long id, UserEmailChangeRequest req) {
    String email = req.getEmail();
    String normalized = (email == null) ? null : email.toLowerCase();

    userRepository.updateEmail(id, normalized);
}
```


## 4. API 테스트 추가 (VSCode REST Client)
`400.http`
```
### email 형식 오류 (400)
PUT {{baseUrl}}/api/users/1/email
Content-Type: application/json

{
  "email": "not-an-email"
}
```

---

## 실습 체크리스트

* 이메일 형식 오류 시 400 응답이 내려오는가
* 빈 문자열 입력이 400이 아닌 정상 처리되는가
* Controller에서 검증 실패를 직접 처리하지 않는가
* Service에서 요청 값 판단 로직을 넣지 않았는가

---

## 다음 단계

→ [**대상 없음 처리 (404) – Service (ApiException 적용)**](03-not_found_handling_404.md)

