# HelloRepository + JdbcTemplate 실습 (Entity 기반 조회)

## 1. 문서 목적

이 문서는 Spring Boot 환경에서 **JdbcTemplate 기반 조회(SELECT) 로직**을 구현하기 위한 기술 문서다.

본 단계의 목적은 다음과 같다.

* Repository 인터페이스와 구현체 분리 구조를 이해한다
* DB 조회 결과를 **Entity 객체**로 매핑하는 표준 패턴을 확립한다

---

## 2. 적용 범위 및 제약

* 대상 테이블: `users`, `posts`
* 처리 유형: **조회(SELECT) 전용**
* 생성/수정/삭제(CUD)는 다루지 않는다
* 인증/인가, 입력 검증, 비즈니스 규칙은 포함하지 않는다

---

## 3. 아키텍처 개요

### 3.1 계층 구조

```
Controller
  ↓
Service
  ↓
Repository (interface)
  ↓
Repository 구현체 (JdbcTemplate)
  ↓
Database
```

### 3.2 계층별 책임

| 계층             | 책임                              |
| -------------- | ------------------------------- |
| Controller     | HTTP 요청 수신 및 응답 반환              |
| Service        | 처리 흐름 제어, 조회 결과 의미 해석           |
| Repository     | DB 접근 추상화                       |
| JdbcRepository | SQL 실행, RowMapper를 통한 Entity 매핑 |

---

## 4. 엔드포인트 정의

| Method | URL               | 설명                   |
| ------ | ----------------- | -------------------- |
| GET    | /hello/users      | 사용자 목록 조회 (최대 1000건) |
| GET    | /hello/users/{id} | 사용자 단건 조회            |
| GET    | /hello/posts      | 게시글 목록 조회 (최대 1000건) |
| GET    | /hello/posts/{id} | 게시글 단건 조회            |

---

## 5. Entity 설계

### 5.1 설계 원칙

* Entity는 **DB 테이블 구조를 코드로 표현한 객체**다
* 테이블 컬럼과 1:1 매핑한다
* 비즈니스 로직을 포함하지 않는다

### 5.2 UserEntity

`entity/UserEntity.java`

```java
package com.koreanit.spring.entity;

import java.time.LocalDateTime;

public class UserEntity {
    private Long id;                 // PK
    private String username;         // 로그인 아이디
    private String email;            // 이메일
    private String password;         // 비밀번호 해시
    private String nickname;         // 닉네임
    private LocalDateTime createdAt; // 가입일
    private LocalDateTime updatedAt; // 수정일

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### 5.3 PostEntity

`entity/PostEntity.java`

```java
package com.koreanit.spring.entity;

import java.time.LocalDateTime;

public class PostEntity {
    private Long id;                  // PK
    private Long userId;              // 작성자 ID (FK)
    private String title;             // 제목
    private String content;           // 내용
    private Integer viewCount;        // 조회수
    private Integer commentsCnt;      // 댓글 수
    private LocalDateTime createdAt;  // 작성일
    private LocalDateTime updatedAt;  // 수정일

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getViewCount() { return viewCount; }
    public Integer getCommentsCnt() { return commentsCnt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public void setCommentsCnt(Integer commentsCnt) { this.commentsCnt = commentsCnt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

---

## 6. Repository 인터페이스

`repository/HelloRepository.java`

```java
package com.koreanit.spring.repository;

import com.koreanit.spring.entity.PostEntity;
import com.koreanit.spring.entity.UserEntity;
import java.util.List;

public interface HelloRepository {

    List<UserEntity> findUsers();

    UserEntity findUserById(Long id);

    List<PostEntity> findPosts();

    PostEntity findPostById(Long id);
}
```

---

## 7. JdbcHelloRepository (JdbcTemplate 구현체)

### 7.1 책임 범위

JdbcHelloRepository는 다음 책임만 가진다.

* SQL 작성
* 파라미터 바인딩
* ResultSet → Entity 매핑(RowMapper)

조회 결과의 의미 해석(존재 여부 판단, 예외 변환 등)은 수행하지 않는다.

---

### 7.2 JdbcHelloRepository 구현

`repository/impl/JdbcHelloRepository.java`

```java
package com.koreanit.spring.repository.impl;

import com.koreanit.spring.entity.PostEntity;
import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.repository.HelloRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcHelloRepository implements HelloRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcHelloRepository(JdbcTemplate jdbcTemplate) {
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
        if (c != null) {
            u.setCreatedAt(c.toLocalDateTime());
        }

        Timestamp up = rs.getTimestamp("updated_at");
        if (up != null) {
            u.setUpdatedAt(up.toLocalDateTime());
        }

        return u;
    };

    private final RowMapper<PostEntity> postRowMapper = (rs, rowNum) -> {
        PostEntity p = new PostEntity();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setViewCount(rs.getInt("view_count"));
        p.setCommentsCnt(rs.getInt("comments_cnt"));

        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) {
            p.setCreatedAt(c.toLocalDateTime());
        }

        Timestamp up = rs.getTimestamp("updated_at");
        if (up != null) {
            p.setUpdatedAt(up.toLocalDateTime());
        }

        return p;
    };

    @Override
    public List<UserEntity> findUsers() {
        String sql =
            "SELECT id, username, email, password, nickname, created_at, updated_at " +
            "FROM users ORDER BY id DESC LIMIT 1000";

        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public UserEntity findUserById(Long id) {
        String sql =
            "SELECT id, username, email, password, nickname, created_at, updated_at " +
            "FROM users WHERE id = ?";

        return jdbcTemplate.queryForObject(sql, userRowMapper, id);
    }

    @Override
    public List<PostEntity> findPosts() {
        String sql =
            "SELECT id, user_id, title, content, view_count, comments_cnt, created_at, updated_at " +
            "FROM posts ORDER BY id DESC LIMIT 1000";

        return jdbcTemplate.query(sql, postRowMapper);
    }

    @Override
    public PostEntity findPostById(Long id) {
        String sql =
            "SELECT id, user_id, title, content, view_count, comments_cnt, created_at, updated_at " +
            "FROM posts WHERE id = ?";

        return jdbcTemplate.queryForObject(sql, postRowMapper, id);
    }
}
```

---

## 8. HelloService (수정)

### 8.1 역할

* `/hello/users`, `/hello/posts` 조회 흐름 처리

`service/HelloService.java`

```java
package com.koreanit.spring.service;

import com.koreanit.spring.entity.PostEntity;
import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.repository.HelloRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

  private final HelloRepository helloRepository;

  public HelloService(HelloRepository helloRepository) {
    this.helloRepository = helloRepository;
  }

  public List<UserEntity> users() {
    return helloRepository.findUsers();
  }

  public UserEntity user(Long id) {
    return helloRepository.findUserById(id);
  }

  public List<PostEntity> posts() {
    return helloRepository.findPosts();
  }

  public PostEntity post(Long id) {
    return helloRepository.findPostById(id);
  }
}
```

---

## 9. HelloController (적용)

### 9.1 역할

* URL 매핑
* PathVariable 바인딩
* Service 호출 및 결과 반환

`controller/HelloController.java`

```java
package com.koreanit.spring.controller;

import com.koreanit.spring.entity.PostEntity;
import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.service.HelloService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  private final HelloService helloService;

  public HelloController(HelloService helloService) {
    this.helloService = helloService;
  }

  @GetMapping("/hello/users")
  public List<UserEntity> users() {
    return helloService.users();
  }

  @GetMapping("/hello/users/{id}")
  public UserEntity user(@PathVariable Long id) {
    return helloService.user(id);
  }

  @GetMapping("/hello/posts")
  public List<PostEntity> posts() {
    return helloService.posts();
  }

  @GetMapping("/hello/posts/{id}")
  public PostEntity post(@PathVariable Long id) {
    return helloService.post(id);
  }
}
```

---

## 10. 테스트 (VSCode REST Client)

`hello.http`

```http
@baseUrl = http://localhost:8080

### users (limit 1000)
GET {{baseUrl}}/hello/users

### user by id
GET {{baseUrl}}/hello/users/100

### posts (limit 1000)
GET {{baseUrl}}/hello/posts

### post by id
GET {{baseUrl}}/hello/posts/100
```

---

# 실습 1. limit 파라미터로 목록 개수 동적 변경

## 실습 목표

* `/hello/users`, `/hello/posts`에서 `?limit=N`으로 조회 개수를 제어한다
* 조회 개수 제한은 **Service 책임**으로 고정한다

---

## 1단계. Repository 인터페이스 변경

```java
List<UserEntity> findUsers(int limit);
List<PostEntity> findPosts(int limit);
```

단건 조회 메서드는 변경하지 않는다.

---

## 2단계. JdbcHelloRepository 수정

### users

```java
@Override
public List<UserEntity> findUsers(int limit) {
    String sql =
        "SELECT id, username, email, password, nickname, created_at, updated_at " +
        "FROM users ORDER BY id DESC LIMIT ?";

    return jdbcTemplate.query(sql, userRowMapper, limit);
}
```

### posts

```java
@Override
public List<PostEntity> findPosts(int limit) {
    String sql =
        "SELECT id, user_id, title, content, view_count, comments_cnt, created_at, updated_at " +
        "FROM posts ORDER BY id DESC LIMIT ?";

    return jdbcTemplate.query(sql, postRowMapper, limit);
}
```

---

## 3단계. HelloService – limit 검증

### 상수

```java
private static final int MAX_LIMIT = 1000;
```

### 정규화 메서드

```java
private int normalizeLimit(int limit) {
    if (limit <= 0) {
        throw new IllegalArgumentException("limit must be >= 1");
    }
    if (limit > MAX_LIMIT) {
        return MAX_LIMIT;
    }
    return limit;
}
```

### 목록 조회 메서드

```java
public List<UserEntity> users(int limit) {
    int safeLimit = normalizeLimit(limit);
    return helloRepository.findUsers(safeLimit);
}

public List<PostEntity> posts(int limit) {
    int safeLimit = normalizeLimit(limit);
    return helloRepository.findPosts(safeLimit);
}
```

---

## 4단계. HelloController

```java
@GetMapping("/hello/users")
public List<UserEntity> users(@RequestParam(defaultValue = "1000") int limit) {
    return helloService.users(limit);
}

@GetMapping("/hello/posts")
public List<PostEntity> posts(@RequestParam(defaultValue = "1000") int limit) {
    return helloService.posts(limit);
}
```

---

## 실습 1 요약

* limit은 **HTTP 파라미터**
* 의미 해석과 제한은 **Service 책임**
* Repository는 SQL + Entity 매핑만 수행

---

# 실습 2. 단건 조회에서 없는 ID 예외 의미 해석

## 실습 목표

* 존재하지 않는 ID로 단건 조회를 수행한다
* `queryForObject()`가 발생시키는 예외를 **Service에서 해석**한다

---

## 1단계. 의도적으로 예외를 발생시킨다

`JdbcTemplate.queryForObject()` 동작:

* 1행 → 정상 반환
* 0행 → `EmptyResultDataAccessException`

Repository는 이 동작을 **그대로 유지**한다.

---

## 2단계. Service에서 예외 해석

### user 단건 조회

```java
public UserEntity user(Long id) {
    try {
        return helloRepository.findUserById(id);
    } catch (EmptyResultDataAccessException e) {
        throw new RuntimeException("존재하지 않는 사용자입니다: id=" + id);
    }
}
```

### post 단건 조회

```java
public PostEntity post(Long id) {
    try {
        return helloRepository.findPostById(id);
    } catch (EmptyResultDataAccessException e) {
        throw new RuntimeException("존재하지 않는 게시글입니다: id=" + id);
    }
}
```

---

## 3단계. 테스트 (의도적 실패)

```http
GET /hello/users/99999999
GET /hello/posts/99999999
```

---

## 실습 2 요약

* Repository는 **조회 실패를 예외로 표현**한다
* Service는 그 예외를 **의미 있는 상태(대상 없음)** 로 해석한다
* Controller는 예외를 처리하지 않는다

---

## 이 장의 핵심 정리

* 이 단계에서 entity는 **DB 테이블 구조 표현 객체**다
* 조회 단계에서는 entity를 그대로 사용한다
* 단건 조회의 0행 예외는 Service에서 의미 해석 후 RuntimeException으로 변환한다

## 다음 단계

* 다음 단계에서 dto/domain 개념을 알아본다.

→ [**Entity → Domain → DTO 적용**](11-domain_dto.md)