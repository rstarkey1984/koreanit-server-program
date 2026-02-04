# Users 정상 흐름 CRUD + 세션 로그인

이 문서는 **Users 도메인의 정상 흐름 CRUD와 세션 기반 로그인을 하나의 기준 흐름으로 완성**한다.

여기서 말하는 **정상 흐름(normal path)** 이란 다음 조건을 모두 만족하는 경우를 의미한다.

* 요청 데이터가 올바르게 전달된 경우
* 대상 리소스가 존재하는 경우
* 중복, 인증 실패, 권한 오류가 발생하지 않는 경우

즉, 본 문서에서는 **400 / 404 / 409 / 401 / 403에 대한 실패 의미 해석을 의도적으로 제외**한다.
이 문서는 이후 단계에서 실패 흐름을 단계별로 추가하기 위한 **기준선(baseline)** 역할을 한다.

---

## 전제 조건

이 문서는 다음 공통 모듈이 **이미 적용되어 있음**을 전제로 한다.

* 공통 응답 포맷: `ApiResponse`
* 공통 예외 처리: `GlobalExceptionHandler`
* 요청 로깅: `AccessLogFilter`

또한 이전 단계에서 다음 구조적 원칙이 이미 고정되었다.

* Entity / Domain / DTO 계층 분리
* Repository / Service / Controller 책임 분리


---

## 도메인 중심 패키지 구조

```text
com.koreanit.spring
├─ Application.java
├─ common
├─ security
│  ├─ AuthController.java
│  ├─ SecurityConfig.java
│  ├─ MethodSecurityConfig.java
│  ├─ SessionAuthenticationFilter.java
│  ├─ LoginUser.java
│  ├─ SecurityUtils.java
│  ├─ UserRoleRepository.java
│  └─ JdbcUserRoleRepository.java
│
└─ user
   ├─ UserController.java
   ├─ UserService.java
   ├─ UserRepository.java
   ├─ JdbcUserRepository.java
   ├─ UserEntity.java
   ├─ User.java
   ├─ UserMapper.java
   └─ dto
      ├─ request
      │  ├─ UserCreateRequest.java
      │  ├─ UserLoginRequest.java
      │  ├─ UserEmailChangeRequest.java
      │  └─ UserPasswordChangeRequest.java
      └─ response
         └─ UserResponse.java
```

---

## 1. 이번 장의 목표

이 장이 끝나면 서버는 다음 기능을 정상 흐름 기준으로 수행할 수 있다.

* 사용자 생성 (Create)
* 사용자 조회 (Read: 단건 / 목록)
* 사용자 정보 수정 (Update)
* 사용자 삭제 (Delete)
* 세션 기반 로그인 및 로그아웃

본 단계에서 **의도적으로 포함하지 않는 항목**은 다음과 같다.

* 입력 값 판단(@Valid)
* 대상 없음(404) 의미 해석
* 중복 제약(409) 의미 해석
* 인증/인가 차단(401/403)

---

## 2. 객체 역할 고정 (재확인)

| 계층     | 역할               | 사용 위치      |
| ------ | ---------------- | ---------- |
| Entity | DB 테이블 구조 표현     | Repository |
| Domain | Service 내부 표준 타입 | Service    |
| DTO    | 외부 API 계약        | Controller |

변환 책임은 다음과 같이 고정한다.

* Entity → Domain: Service (또는 Mapper)
* Domain → DTO: Controller (또는 Mapper)

---

## 3. PasswordEncoder Bean 등록

### 파일: `common/config/SecurityBeansConfig.java`

#### 파일 역할

* 서버 전역에서 사용할 `PasswordEncoder`를 공통 Bean으로 등록한다.
* 비밀번호 해시 정책(BCrypt)을 단일 지점에서 고정한다.
* 인증/인가 로직과는 무관한 **암호화 정책 선언**만을 담당한다.

```java
package com.koreanit.spring.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## 4. DTO 정의 (외부 API 계약)

DTO는 외부 요청/응답 계약만을 표현하며, 도메인 규칙이나 저장 방식은 포함하지 않는다.

---

### 4-1. 회원가입 요청 DTO

파일: `user/dto/request/UserCreateRequest.java`

#### 파일 역할

* 회원가입 요청 바디(JSON)를 표현한다.
* 외부 입력 필드를 그대로 수용하는 역할만 담당한다.
* Domain 및 Repository 구조와 직접적인 연관을 갖지 않는다.

```java
package com.koreanit.spring.dto.request;

public class UserCreateRequest {

    private String username;
    private String password;
    private String nickname;
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

---

### 4-2. 로그인 요청 DTO

파일: `user/dto/request/UserLoginRequest.java`

#### 파일 역할

* 로그인 요청 시 필요한 사용자 식별 정보와 인증 정보를 전달한다.
* 인증 성공 여부 판단이나 세션 생성은 담당하지 않는다.

```java
package com.koreanit.spring.dto.request;

public class UserLoginRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

---

### 4-3. 비밀번호 변경 요청 DTO

파일: `user/dto/request/UserPasswordChangeRequest.java`

#### 파일 역할

* 비밀번호 변경 요청 데이터를 전달한다.
* 실제 암호화 및 저장은 Service 계층에서 수행된다.

```java
package com.koreanit.spring.dto.request;

public class UserPasswordChangeRequest {

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

### 4-4. 닉네임 변경 요청 DTO

파일: `user/dto/request/UserNicknameChangeRequest.java`

#### 파일 역할

* 비밀번호 변경 요청 데이터를 전달한다.
* 실제 암호화 및 저장은 Service 계층에서 수행된다.

```java
package com.koreanit.spring.dto.request;

public class UserNicknameChangeRequest {
    private String nickname;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
```

---

## 5. Repository 계층 (Entity 반환 고정)

Repository 계층은 **DB 접근 전담 계층**이며, 의미 해석을 수행하지 않는다.

---

### 5-1. Repository 인터페이스

파일: `user/UserRepository.java`

#### 파일 역할

* Users 테이블에 대한 접근 계약을 정의한다.
* 반환 타입은 항상 Entity로 고정한다.
* 조회 결과에 대한 의미 해석(404/409 등)은 수행하지 않는다.

```java
package com.koreanit.spring.repository;

import java.util.List;
import com.koreanit.spring.entity.UserEntity;

public interface UserRepository {

    Long save(String username, String passwordHash, String nickname, String email);

    UserEntity findById(Long id);

    UserEntity findByUsername(String username);

    List<UserEntity> findAll(int limit);

    int updateNickname(Long id, String nickname);

    int updatePassword(Long id, String passwordHash);

    int deleteById(Long id);
}
```

---

### 5-2. JdbcTemplate 구현체

파일: `user/JdbcUserRepository.java`

#### 파일 역할

* SQL 실행과 파라미터 바인딩을 담당한다.
* ResultSet을 Entity로 매핑한다.
* DB 처리 결과에 대한 의미 판단은 수행하지 않는다.

```java
// UserRepository의 JdbcTemplate 기반 구현체
// SQL 실행과 ResultSet → Entity 매핑만 담당한다.
package com.koreanit.spring.repository.impl;

import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.repository.UserRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final RowMapper<UserEntity> userRowMapper = (rs, rowNum) -> {
    UserEntity u = new UserEntity();
    u.setId(rs.getLong("id"));
    u.setUsername(rs.getString("username"));
    u.setEmail(rs.getString("email"));
    u.setPassword(rs.getString("password"));
    u.setNickname(rs.getString("nickname"));

    Timestamp c = rs.getTimestamp("created_at");
    if (c != null) u.setCreatedAt(c.toLocalDateTime());

    Timestamp up = rs.getTimestamp("updated_at");
    if (up != null) u.setUpdatedAt(up.toLocalDateTime());

    return u;
  };

  @Override
  public Long save(String username, String passwordHash, String nickname, String email) {
    String sql = """
        INSERT INTO users (username, password, nickname, email)
        VALUES (?, ?, ?, ?)
        """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, username);
      ps.setString(2, passwordHash);
      ps.setString(3, nickname);
      ps.setString(4, email);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    return (key != null) ? key.longValue() : null;
  }

  @Override
  public UserEntity findById(Long id) {
    String sql = """
        SELECT id, username, email, password, nickname, created_at, updated_at
        FROM users
        WHERE id = ?
        """;
    return jdbcTemplate.queryForObject(sql, userRowMapper, id);
  }

  @Override
  public UserEntity findByUsername(String username) {
    String sql = """
        SELECT id, username, email, password, nickname, created_at, updated_at
        FROM users
        WHERE username = ?
        """;
    return jdbcTemplate.queryForObject(sql, userRowMapper, username);
  }

  @Override
  public List<UserEntity> findAll(int limit) {
    String sql = """
        SELECT id, username, email, password, nickname, created_at, updated_at
        FROM users
        ORDER BY id DESC
        LIMIT ?
        """;
    return jdbcTemplate.query(sql, userRowMapper, limit);
  }

  @Override
  public int updateNickname(Long id, String nickname) {
    String sql = """
        UPDATE users
        SET nickname = ?, updated_at = NOW()
        WHERE id = ?
        """;
    return jdbcTemplate.update(sql, nickname, id);
  }

  @Override
  public int updatePassword(Long id, String passwordHash) {
    String sql = """
        UPDATE users
        SET password = ?, updated_at = NOW()
        WHERE id = ?
        """;
    return jdbcTemplate.update(sql, passwordHash, id);
  }

  @Override
  public int deleteById(Long id) {
    String sql = """
        DELETE FROM users
        WHERE id = ?
        """;
    return jdbcTemplate.update(sql, id);
  }
}
```

---

## 6. Service 계층 (정상 흐름 제어)

파일: `user/UserService.java`

#### 파일 역할

* Users 도메인의 정상 흐름 비즈니스 로직을 수행한다.
* Repository 결과(Entity)를 Domain으로 변환한다.
* 비밀번호 해시 생성 및 비교 같은 도메인 규칙을 적용한다.

```java
package com.koreanit.spring.service;

import com.koreanit.spring.domain.User;
import com.koreanit.spring.dto.request.UserCreateRequest;
import com.koreanit.spring.dto.request.UserPasswordChangeRequest;
import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.mapper.UserMapper;
import com.koreanit.spring.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final int MAX_LIMIT = 1000;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  private int normalizeLimit(int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit 은 1 이상 입력해주세요");
    }
    return Math.min(limit, MAX_LIMIT);
  }

  public Long create(UserCreateRequest req) {
    String hash = passwordEncoder.encode(req.getPassword());

    return userRepository.save(
        req.getUsername(),
        hash,
        req.getNickname(),
        req.getEmail());
  }

  public User get(Long id) {
    UserEntity e = userRepository.findById(id);
    return UserMapper.toDomain(e);
  }

  public List<User> list(int limit) {
    int safeLimit = normalizeLimit(limit);
    return UserMapper.toDomainList(userRepository.findAll(safeLimit));
  }

  public void changeNickname(Long id, UserNicknameChangeRequest req) {
    String nickname = req.getNickname();
    userRepository.updateNickname(id, nickname);
  }

  public void changePassword(Long id, UserPasswordChangeRequest req) {
    String hash = passwordEncoder.encode(req.getPassword());
    userRepository.updatePassword(id, hash);
  }

  public void delete(Long id) {
    userRepository.deleteById(id);
  }

  public Long login(String username, String password) {
    UserEntity e = userRepository.findByUsername(username);

    boolean ok = passwordEncoder.matches(password, e.getPassword());
    if (!ok) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "비밀번호 검증 실패");
    }

    return e.getId();
  }
}
```

---

## 7. Controller 계층 (ApiResponse 고정)

### 7-1. Users CRUD Controller

파일: `user/UserController.java`

#### 파일 역할

* Users 리소스에 대한 CRUD API를 제공한다.
* Service 결과(Domain)를 DTO로 변환하여 반환한다.
* 모든 응답을 `ApiResponse`로 통일한다.

```java
package com.koreanit.spring.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.koreanit.spring.common.response.ApiResponse;
import com.koreanit.spring.dto.request.UserCreateRequest;
import com.koreanit.spring.dto.request.UserPasswordChangeRequest;
import com.koreanit.spring.dto.response.UserResponse;
import com.koreanit.spring.mapper.UserMapper;
import com.koreanit.spring.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody UserCreateRequest req) {
        return ApiResponse.ok(userService.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(UserMapper.toResponse(userService.get(id)));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(@RequestParam(defaultValue = "1000") int limit) {
        return ApiResponse.ok(UserMapper.toResponseList(userService.list(limit)));
    }

    @PutMapping("/{id}/nickname")
    public ApiResponse<Void> changeNickname(@PathVariable Long id, @RequestBody UserNicknameChangeRequest req) {
        userService.changeNickname(id, req);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> changePassword(@PathVariable Long id, @RequestBody UserPasswordChangeRequest req) {
        userService.changePassword(id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }
}
```

---

### 7-2. 인증 Controller (세션 기반)

파일: `security/AuthController.java`

#### 파일 역할

* 세션 기반 로그인 및 로그아웃 API를 제공한다.
* 로그인 성공 시 사용자 식별자를 세션에 저장한다.
* 인증 실패에 대한 의미 해석은 이후 단계에서 추가된다.

```java
package com.koreanit.spring.controller;

import org.springframework.web.bind.annotation.*;
import com.koreanit.spring.common.response.ApiResponse;
import com.koreanit.spring.dto.request.UserLoginRequest;
import com.koreanit.spring.dto.response.UserResponse;
import com.koreanit.spring.mapper.UserMapper;
import com.koreanit.spring.service.UserService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class AuthController {

    public static final String SESSION_USER_ID = "LOGIN_USER_ID";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<Long> login(@RequestBody UserLoginRequest req, HttpSession session) {
      Long userId = userService.login(req.getUsername(), req.getPassword());
      session.setAttribute(SESSION_USER_ID, userId);
      return ApiResponse.ok(userId);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        return ApiResponse.ok(UserMapper.toResponse(userService.get(userId)));
    }
}
```

---

## 8. API 테스트 (VSCode REST Client)

`users.http`

```http
@baseUrl = http://localhost:8080
@json = application/json

### 1) 회원가입
POST {{baseUrl}}/api/users
Content-Type: {{json}}

{
  "username": "admin",
  "password": "1234",
  "nickname": "관리자",
  "email": "admin@admin.com"
}
# 설명: 신규 유저를 생성하고 응답으로 userId를 받는다.

### 2) 로그인(세션 생성)
POST {{baseUrl}}/api/login
Content-Type: {{json}}

{
  "username": "admin",
  "password": "1234"
}

### 3) 내 정보 조회(/api/me)
GET {{baseUrl}}/api/me

### 4) 사용자 목록(limit)
GET {{baseUrl}}/api/users?limit=10

### 5) 사용자 단건 조회
GET {{baseUrl}}/api/users/1

### 6) 닉네임 변경
PUT {{baseUrl}}/api/users/1/nickname
Content-Type: {{json}}

{
  "nickname": "nickname"
}
# 설명: id=1 사용자의 nickname을 변경한다.

### 7) 비밀번호 변경
PUT {{baseUrl}}/api/users/1/password
Content-Type: {{json}}

{
  "password": "newpassword"
}
# 설명: id=1 사용자의 password를 BCrypt 해시로 변경 저장한다.

### 8) 로그아웃(세션 삭제)
POST {{baseUrl}}/api/logout

### 9) 사용자삭제
DELETE {{baseUrl}}/api/users/1
```

---


# 실습 — 이메일 변경 API 추가

## 실습 목표

이 실습의 목표는 다음과 같다.

* 기존 Users CRUD 흐름을 **확장하는 방식**을 익힌다.
* 새로운 기능 추가 시 **DTO → Controller → Service → Repository** 흐름이 어떻게 이어지는지 확인한다.
* 실패 처리나 검증을 고려하지 않고, **정상 흐름만으로 기능을 추가하는 연습**을 한다.

> 이 실습은 “기능을 하나 추가할 때 어떤 파일들이 어디까지 수정되는가”를 체감하는 것이 목적이다.

---

## 1. Request DTO 작성

파일: `user/dto/request/UserEmailChangeRequest.java`

### 파일 역할

* 이메일 변경 요청 바디(JSON)를 표현한다.
* 외부에서 전달되는 이메일 값을 그대로 수용한다.
* 검증(@Email) 여부와 무관하게 **요청 계약 구조를 고정**하는 역할만 담당한다.

```java
package com.koreanit.spring.dto.request;

import jakarta.validation.constraints.Email;

public class UserEmailChangeRequest {

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        return email;
    }
}
```

---

## 2. Controller 엔드포인트 추가

파일: `user/UserController.java`

### 파일 역할

* 이메일 변경에 대한 **HTTP API 엔드포인트**를 추가한다.
* 요청 DTO를 Service로 전달하고, 결과를 `ApiResponse`로 감싼다.
* 비즈니스 판단이나 DB 처리 로직은 포함하지 않는다.

```java
@PutMapping("/{id}/email")
public ApiResponse<Void> changeEmail(
        @PathVariable Long id,
        @RequestBody UserEmailChangeRequest req
) {
    userService.changeEmail(id, req);
    return ApiResponse.ok();
}
```

---

## 3. Service 메서드 구현

파일: `user/UserService.java`

### 파일 역할

* Users 도메인에서 “이메일 변경”이라는 **비즈니스 동작**을 수행한다.
* 요청 DTO에서 필요한 값만 추출하여 Repository에 전달한다.
* 정상 흐름 기준으로 처리하며, 실패 의미 해석은 포함하지 않는다.

```java
public void changeEmail(Long id, UserEmailChangeRequest req) {
    String email = req.getEmail();
    userRepository.updateEmail(id, email);
}
```

---

## 4. Repository 메서드 추가

### 4-1. Repository 인터페이스

파일: `user/UserRepository.java`

### 파일 역할

* Users 테이블에 대한 **이메일 수정 계약**을 정의한다.
* 구현 방식(SQL, JdbcTemplate 등)은 노출하지 않는다.
* 반환값(int)은 DB 처리 결과만 전달하며 의미 해석은 하지 않는다.

```java
int updateEmail(Long id, String email);
```

---

### 4-2. JdbcTemplate 구현체

파일: `user/JdbcUserRepository.java`

### 파일 역할

* 이메일 변경 SQL을 실행한다.
* 파라미터 바인딩과 update 실행만 담당한다.
* 영향받은 row 수 외의 판단 로직은 포함하지 않는다.

```java
@Override
public int updateEmail(Long id, String email) {
  return jdbcTemplate.update(
      "update users set email = ? where id = ?",
      email, id);
}
```

### 5. API 테스트 추가 (VSCode REST Client)
`users.http`
```
### 10) 이메일 변경
PUT {{baseUrl}}/api/users/1/email
Content-Type: application/json

{
  "email": "email@email.com"
}
```

---

## 이 실습의 핵심 포인트

* 기능 추가 시 **새 DTO + Service 메서드 + Repository 메서드**가 함께 확장된다.
* 기존 구조(Entity / Domain / DTO, 계층 책임 분리)는 그대로 유지된다.
* 정상 흐름 기준에서는 “성공했을 때의 동작”만 정확히 구현하면 된다.

이 상태는 다음 단계인 **입력 검증(400)**, **대상 없음(404)**, **중복(409)** 을 추가하기 위한 가장 안정적인 기준선이다.


---

## 이 장의 정리

* Users 도메인의 정상 흐름 CRUD가 완성되었다.
* 세션 기반 로그인으로 인증 상태를 생성할 수 있다.
* 실패 의미 해석은 아직 포함하지 않는다.
* 본 문서는 이후 단계의 기준선으로 사용된다.

---

## 다음 단계

→ [**입력 검증 (400) — DTO(@Valid) + Service(정규화)**](02-service_input_validation_400.md)
