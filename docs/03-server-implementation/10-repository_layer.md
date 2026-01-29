# HelloRepository + JdbcTemplate 실습 (users/posts 조회)

이 실습은 **Repository 인터페이스 + JdbcTemplate 구현체** 구조를 유지하면서,
이미 만들어진 `users`, `posts` 테이블을 대상으로 **조회(SELECT)** 를 구현한다.

핵심 규칙

* Controller → Service → Repository(interface) → Repository(impl: JdbcTemplate)
* JdbcTemplate은 **Repository 구현체에서만** 사용
* 전체 목록은 **LIMIT 1000** 으로 제한
* 단건 조회는 `id` 기반으로 추가

---

## 엔드포인트

* `GET /hello/users` : users 목록 (최대 1000)
* `GET /hello/users/{id}` : users 단건
* `GET /hello/posts` : posts 목록 (최대 1000)
* `GET /hello/posts/{id}` : posts 단건

---

## 1) 조회용 DTO (Row)

### UserRow

`model/UserRow.java`

```java
package com.koreanit.spring.model;

public class UserRow {
    private Long id;
    private String username;
    private String email;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
}
```

### PostRow

`model/PostRow.java`

```java
package com.koreanit.spring.model;

public class PostRow {
    private Long id;
    private Long userId;
    private String title;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
}
```

---

## 2) HelloRepository (interface)

`repository/HelloRepository.java`

```java
package com.koreanit.spring.repository;

import com.koreanit.spring.model.PostRow;
import com.koreanit.spring.model.UserRow;

import java.util.List;

public interface HelloRepository {

    List<UserRow> findUsers();

    UserRow findUserById(Long id);

    List<PostRow> findPosts();

    PostRow findPostById(Long id);
}
```

---

## 3) JdbcHelloRepository (JdbcTemplate 구현체)

`repository/impl/JdbcHelloRepository.java`

```java
package com.koreanit.spring.repository.impl;

import com.koreanit.spring.model.PostRow;
import com.koreanit.spring.model.UserRow;
import com.koreanit.spring.repository.HelloRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcHelloRepository implements HelloRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcHelloRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserRow> userRowMapper = (rs, rowNum) -> {
        UserRow u = new UserRow();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        return u;
    };

    private final RowMapper<PostRow> postRowMapper = (rs, rowNum) -> {
        PostRow p = new PostRow();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setTitle(rs.getString("title"));
        return p;
    };

    @Override
    public List<UserRow> findUsers() {
        String sql =
            "SELECT id, username, email " +
            "FROM users " +
            "ORDER BY id DESC " +
            "LIMIT 1000";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public UserRow findUserById(Long id) {
        String sql =
            "SELECT id, username, email " +
            "FROM users " +
            "WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, userRowMapper, id);
    }

    @Override
    public List<PostRow> findPosts() {
        String sql =
            "SELECT id, user_id, title " +
            "FROM posts " +
            "ORDER BY id DESC " +
            "LIMIT 1000";
        return jdbcTemplate.query(sql, postRowMapper);
    }

    @Override
    public PostRow findPostById(Long id) {
        String sql =
            "SELECT id, user_id, title " +
            "FROM posts " +
            "WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, postRowMapper, id);
    }
}
```

RowMapper 포인트

* **SELECT에 쓴 컬럼만** 꺼낸다
* 타입은 `getLong`, `getString` 등으로 명확히

---

## 4) HelloService

`service/HelloService.java`

```java
package com.koreanit.spring.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.koreanit.spring.model.PostRow;
import com.koreanit.spring.model.UserRow;
import com.koreanit.spring.repository.HelloRepository;

@Service
public class HelloService {

    private final HelloRepository helloRepository;

    public HelloService(HelloRepository helloRepository) {
        this.helloRepository = helloRepository;
    }

    public String helloMessage() {
        return "Hello from Service";
    }

    public Map<String, String> helloMessageJSON() {
        Map<String, String> result = new HashMap<>();
        result.put("message", "Hello JSON");
        result.put("date", LocalDateTime.now().toString());
        return result;
    }

    public List<UserRow> users() {
        return helloRepository.findUsers();
    }

    public UserRow user(Long id) {
        return helloRepository.findUserById(id);
    }

    public List<PostRow> posts() {
        return helloRepository.findPosts();
    }

    public PostRow post(Long id) {
        return helloRepository.findPostById(id);
    }
}
```

---

## 5) HelloController

`controller/HelloController.java`

```java
package com.koreanit.spring.controller;

import com.koreanit.spring.model.PostRow;
import com.koreanit.spring.model.UserRow;
import com.koreanit.spring.service.HelloService;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  private final HelloService helloService;

  public HelloController(HelloService helloService) {
    this.helloService = helloService;
  }

  @GetMapping("/hello")
  public String hello() {
    return helloService.helloMessage();
  }

  @GetMapping("/hello/json")
  public Map<String, String> helloJson() {
    return helloService.helloMessageJSON();
  }

  @GetMapping("/hello/users")
  public List<UserRow> users() {
    return helloService.users();
  }

  @GetMapping("/hello/users/{id}")
  public UserRow user(@PathVariable Long id) {
    return helloService.user(id);
  }

  @GetMapping("/hello/posts")
  public List<PostRow> posts() {
    return helloService.posts();
  }

  @GetMapping("/hello/posts/{id}")
  public PostRow post(@PathVariable Long id) {
    return helloService.post(id);
  }
}
```

---

## 6) REST Client 테스트

`api.http`

```http
### users (limit 1000)
GET http://localhost:8080/hello/users

### user by id
GET http://localhost:8080/hello/users/100

### posts (limit 1000)
GET http://localhost:8080/hello/posts

### post by id
GET http://localhost:8080/hello/posts/100
```

---

# 실습: 단건 조회 에러 catch (Service + RuntimeException)

단건 조회에서
존재하지 않는 id를 조회하면 `JdbcTemplate.queryForObject()`는
`EmptyResultDataAccessException`을 던진다.

이 실습의 목표는 다음이다.

> **조회 결과가 없을 때 Service에서 RuntimeException을 던진다**

(아직 공통 예외, ApiException, GlobalExceptionHandler는 도입하지 않는다)

---

### 1) 문제 상황 확인

```http
GET http://localhost:8080/hello/users/99999999
```

현재 동작

* `queryForObject()` 실행
* 결과 없음
* `EmptyResultDataAccessException` 발생
* 그대로 전파되어 500 에러

---

### 2) Repository는 수정하지 않는다

`JdbcHelloRepository.java`

```java
@Override
public UserRow findUserById(Long id) {
    String sql =
        "SELECT id, username, email " +
        "FROM users " +
        "WHERE id = ?";

    return jdbcTemplate.queryForObject(sql, userRowMapper, id);
}
```

`PostRow` 단건 조회도 동일하게 둔다.

---

### 3) Service에서 try–catch로 RuntimeException 처리

`HelloService.java`

```java
import org.springframework.dao.EmptyResultDataAccessException;
```

```java
public UserRow user(Long id) {
    try {
        return helloRepository.findUserById(id);
    } catch (EmptyResultDataAccessException e) {
        throw new RuntimeException("존재하지 않는 사용자입니다: id=" + id);
    }
}
```

```java
public PostRow post(Long id) {
    try {
        return helloRepository.findPostById(id);
    } catch (EmptyResultDataAccessException e) {
        throw new RuntimeException("존재하지 않는 게시글입니다: id=" + id);
    }
}
```

포인트

* Repository는 DB 접근만 담당한다
* 조회 실패의 **의미 해석은 Service 책임**이다
* 아직은 전용 예외 클래스 없이 `RuntimeException`만 사용한다

---

### 4) 결과 확인

```http
GET http://localhost:8080/hello/users/99999999
```

결과

* HTTP 상태: 500
* 메시지: `user not found: id=99999999`


---

## 다음 단계

→ [**공통 응답 포맷**](/docs/04-common-modules/01-common_response_format.md)