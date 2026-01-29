# Users CRUD 설계와 JdbcTemplate CRUD 구현

이 장에서는 앞 장에서 완성한 **Repository 인터페이스 + JdbcTemplate 구현체 구조**를 그대로 유지한 채,
실제 `users` 테이블을 대상으로 **CRUD(Create / Read / Update / Delete)** 를 구현한다.

이 장의 핵심은 기능 구현이 아니라 **책임 분리와 흐름 유지**다.

---

## 이 장의 목표

* `users` 테이블 기준 CRUD 메서드를 설계한다
* Repository 인터페이스에 **역할 중심 메서드**를 정의한다
* JdbcTemplate을 이용해 CRUD SQL을 구현한다
* Service는 구현체를 모르고 인터페이스만 사용한다
* Controller는 HTTP 요청 → Service 호출만 담당한다
* **DELETE는 대상이 없을 경우 Service에서 예외를 발생**시킨다
* 공통 에러는 다음 챕터에서 구현하므로, 지금은 **RuntimeException만 사용**한다

---

## 전제 조건

다음 항목이 이미 준비되어 있어야 한다.

* DataSource 설정 완료
* JdbcTemplate Bean 자동 생성 확인
* Repository 인터페이스 + 구현체 분리 구조 완료

---

## 1. users 테이블 다시 확인

```sql
CREATE TABLE users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);
```

이 장에서는 다음 컬럼만 사용한다.

* id
* username
* password
* nickname
* email (값이 없으면 `NULL` 허용)

---

## 2. 도메인 객체(User) 생성

패키지:

```text
com.koreanit.spring.model
```

```java
package com.koreanit.spring.model;

public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

* DB 한 row ↔ Java 객체 1개
* SQL 결과를 담는 용도

---

## 3. Repository 인터페이스에 CRUD 역할 정의

패키지:

```text
com.koreanit.spring.repository
```

```java
package com.koreanit.spring.repository;

import com.koreanit.spring.model.User;
import java.util.List;

public interface UserRepository {

    Long save(User user);

    User findById(Long id);

    List<User> findAll();

    int updateNickname(Long id, String nickname);

    int deleteById(Long id);
}
```

### 포인트

* SQL 없음
* JdbcTemplate 없음
* 오직 **역할 정의**만 존재
* UPDATE는 결과를 해석하지 않는다
* DELETE는 결과(row 수)를 반환한다

---

## 4. JdbcTemplate 기반 Repository 구현체

패키지:

```text
com.koreanit.spring.repository.impl
```

```java
package com.koreanit.spring.repository.impl;

import com.koreanit.spring.model.User;
import com.koreanit.spring.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class JdbcUserRepository implements UserRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setUsername(rs.getString("username"));
    user.setPassword(rs.getString("password"));
    user.setNickname(rs.getString("nickname"));
    user.setEmail(rs.getString("email"));
    return user;
  };

  @Override
  public Long save(User user) {
    String sql = "INSERT INTO users (username, password, nickname, email) " +
        "VALUES (?, ?, ?, ?)";

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, user.getUsername());
      ps.setString(2, user.getPassword());
      ps.setString(3, user.getNickname());
      ps.setString(4, user.getEmail());
      return ps;
    }, keyHolder);

    return keyHolder.getKey().longValue();
  }

  @Override
  public User findById(Long id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    return jdbcTemplate.queryForObject(sql, userRowMapper, id);
  }

  @Override
  public List<User> findAll() {
    String sql = "SELECT * FROM users ORDER BY id DESC LIMIT 100";
    return jdbcTemplate.query(sql, userRowMapper);
  }

  @Override
  public int updateNickname(Long id, String nickname) {
    String sql = "UPDATE users SET nickname = ? WHERE id = ?";
    return jdbcTemplate.update(sql, nickname, id);
  }

  @Override
  public int deleteById(Long id) {
    String sql = "DELETE FROM users WHERE id = ?";
    return jdbcTemplate.update(sql, id);
  }
}
```

---

## 5. Service 예외 처리 방식

* 전용 예외 클래스 사용 안 함
* **RuntimeException만 사용**
* UPDATE는 예외를 발생시키지 않는다
* DELETE만 결과를 보고 예외를 판단한다

---

## 6. Service는 인터페이스만 의존

```java
package com.koreanit.spring.service;

import com.koreanit.spring.model.User;
import com.koreanit.spring.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long register(User user) {
        // email 값이 비어 있으면 null 로 보정
        if (user.getEmail() != null && user.getEmail().isBlank()) {
            user.setEmail(null);
        }
        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    public int changeNickname(Long id, String nickname) {
        return userRepository.updateNickname(id, nickname);
    }

    public int removeUser(Long id) {
        return userRepository.deleteById(id);
    }
}
```

---

## 7. Controller는 요청 / 응답만 담당

```java
package com.koreanit.spring.controller;

import com.koreanit.spring.common.ApiResponse;
import com.koreanit.spring.model.User;
import com.koreanit.spring.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody User user) {
        return ApiResponse.ok(userService.register(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.ok(userService.getUsers());
    }

    @PutMapping("/{id}/nickname")
    public ApiResponse<Integer> updateNickname(
            @PathVariable Long id,
            @RequestParam String nickname
    ) {
        return ApiResponse.ok(userService.changeNickname(id, nickname));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Integer> delete(@PathVariable Long id) {
        return ApiResponse.ok(userService.removeUser(id));
    }
}
```

---

## 8. `spring/.http/users.http`

```
@baseUrl = http://localhost:8080

### 1) 사용자 생성
POST {{baseUrl}}/users
Content-Type: application/json

{
  "username": "test1",
  "password": "1234",
  "nickname": "유저1",
  "email": ""
}

### 2) 단건 조회
GET {{baseUrl}}/users/1

### 3) 전체 조회
GET {{baseUrl}}/users

### 4) 닉네임 수정
PUT {{baseUrl}}/users/1/nickname?nickname=새닉네임

### 5) 사용자 삭제
DELETE {{baseUrl}}/users/1
```

---

## 이 장의 핵심 정리

* CRUD는 Repository에서만 DB를 건드린다
* DELETE는 대상이 없으면 Service에서 예외를 발생시킨다
* Service는 구현체를 모른다
* Controller는 HTTP ↔ Service 연결만 담당한다
* 공통 에러 / 공통 응답은 다음 챕터에서 도입한다

