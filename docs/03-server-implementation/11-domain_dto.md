# Entity → Domain → DTO 계층 분리와 데이터 흐름

이 문서는 기존 **HelloRepository + JdbcTemplate 조회 실습**에
**Domain과 Response DTO를 적용**하여, 계층별 책임과 데이터 흐름을 고정하는 기술 문서다.

이 장의 목표는 다음 두 가지다.

* User CRUD 단계로 넘어가기 전에 **Service에서 다루는 표준 타입(Domain)** 을 한 번 확실히 익힌다

    > 서비스에서 도메인이란 비즈니스 규칙과 상태를 담고, 서비스 로직이 직접 다루는 핵심 객체다.
* 외부 응답은 반드시 DTO로 고정하여 **민감 정보 노출(예: password)** 을 구조적으로 차단한다
    > 컨트롤러에서 DTO란 요청·응답을 위해 외부와 주고받는 데이터 구조를 고정한 전달 객체다.

> Entity는 Repository에서만 사용하고,
> Domain은 Service에서만 사용하며,
> DTO는 Controller에서만 사용한다.

---

## 1. 전체 아키텍처 흐름

```text
1. Client
   └─ 요청을 보낸다

2. Controller
   └─ 요청을 받고 Service를 호출한다
   └─ Service가 준 Domain을 DTO로 바꿔서 응답한다

3. Service
   └─ Repository를 호출한다
   └─ Repository가 준 Entity를 Domain으로 변환한다
   └─ 조회 실패 같은 의미를 해석한다

4. Repository (JdbcTemplate)
   └─ Database에 SQL을 보내서 실행한다
   └─ 결과를 Entity로 만들어서 반환한다

5. Database
   └─ 실제 데이터 저장소
```

---

## 2. 역할 정의

### 2.1 Entity

* DB 테이블 구조를 그대로 표현한 객체
* 컬럼과 필드가 1:1로 매핑된다
* RowMapper의 결과물
* password 등 외부 노출되면 안 되는 값이 포함될 수 있다

사용 위치:

* Repository 구현체(JdbcTemplate)

---

### 2.2 Domain

* 서비스가 의미 있게 다루는 데이터
* Service 계층의 표준 입력/출력 타입
* DB 구조(Entity)와 외부 응답(DTO) 사이에서 **Service 로직을 안정화**한다

사용 위치:

* Service

중요 기준:

> Service는 Entity에 종속되지 않고 Domain으로 흐름을 만든다.

---

### 2.3 DTO (Response DTO)

* 외부 응답 전용 객체
* 보여줄 필드만 가진다
* 클라이언트와의 데이터 계약(응답 포맷)

사용 위치:

* Controller

중요 기준:

> 민감 정보 차단(예: password)은 DTO에서 해결한다.

Entity와 DTO는 ‘어디서 쓰는지’를 이름에 붙이고, Domain은 ‘무엇인가’를 이름으로 쓴다.

---

## 3. 변환 책임 규칙

| 변환                    | 담당         |
| --------------------- | ---------- |
| Entity → Domain       | Service    |
| Domain → Response DTO | Controller |

---

## 4. 엔드포인트

| Method | URL               | 설명                   |
| ------ | ----------------- | -------------------- |
| GET    | /hello/users      | 사용자 목록 조회 (최대 1000건) |
| GET    | /hello/users/{id} | 사용자 단건 조회            |
| GET    | /hello/posts      | 게시글 목록 조회 (최대 1000건) |
| GET    | /hello/posts/{id} | 게시글 단건 조회            |

---

## 5. 실습 코드

## 5-1. Domain

### User

`domain/User.java`

```java
package com.koreanit.spring.domain;

import java.time.LocalDateTime;

public class User {

  private final Long id;
  private final String username;
  private final String email;
  private final String password; // Domain에는 있을 수 있다(외부 응답은 DTO가 막는다)
  private final String nickname;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  private User(
      Long id,
      String username,
      String email,
      String password,
      String nickname,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.password = password;
    this.nickname = nickname;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  // Domain 생성 규칙(검증)을 모으는 팩토리
  public static User of(
      Long id,
      String username,
      String email,
      String password,
      String nickname,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    if (id == null)
      throw new IllegalArgumentException("id는 필수입니다");
    if (username == null)
      throw new IllegalArgumentException("username은 필수입니다");

    return new User(id, username, email, password, nickname, createdAt, updatedAt);
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getNickname() {
    return nickname;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
```

### Post

`domain/Post.java`

```java
package com.koreanit.spring.domain;

import java.time.LocalDateTime;

public class Post {

  private final Long id;
  private final Long userId;
  private final String title;
  private final String content;
  private final Integer viewCount;
  private final Integer commentsCnt;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  private Post(
      Long id,
      Long userId,
      String title,
      String content,
      Integer viewCount,
      Integer commentsCnt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.title = title;
    this.content = content;
    this.viewCount = viewCount;
    this.commentsCnt = commentsCnt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Post of(
      Long id,
      Long userId,
      String title,
      String content,
      Integer viewCount,
      Integer commentsCnt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    if (id == null)
      throw new IllegalArgumentException("id는 필수입니다");
    return new Post(id, userId, title, content, viewCount, commentsCnt, createdAt, updatedAt);
  }

  public String summary(int maxLen) {
    if (maxLen <= 0)
      return "";
    if (content == null)
      return "";
    return content.length() <= maxLen ? content : content.substring(0, maxLen) + "...";
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public Integer getViewCount() {
    return viewCount;
  }

  public Integer getCommentsCnt() {
    return commentsCnt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
```

---

## 5-2. Response DTO

### UserResponse

`dto/response/UserResponse.java`

```java
package com.koreanit.spring.dto.response;

import com.koreanit.spring.domain.User;
import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // domain -> dto 
    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.password = u.getPassword();
        r.email = u.getEmail();
        r.nickname = u.getNickname();
        r.createdAt = u.getCreatedAt();
        return r;
    }
}
```

### PostResponse

`dto/response/PostResponse.java`

```java
package com.koreanit.spring.dto.response;

import com.koreanit.spring.domain.Post;
import java.time.LocalDateTime;

public class PostResponse {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer commentsCnt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getViewCount() { return viewCount; }
    public Integer getCommentsCnt() { return commentsCnt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // domain -> dto 
    public static PostResponse from(Post p) {
        PostResponse r = new PostResponse();
        r.id = p.getId();
        r.userId = p.getUserId();
        r.title = p.getTitle();
        r.content = p.getContent();
        r.viewCount = p.getViewCount();
        r.commentsCnt = p.getCommentsCnt();
        r.createdAt = p.getCreatedAt();
        return r;
    }
}
```

---

## 5-3. Mapper (변환 책임 분리)

변환은 매번 필요하지만,
변환 코드를 Service/Controller에 흩어지게 하지 않기 위해 Mapper로 고정한다.

`mapper/UserMapper.java`

```java
package com.koreanit.spring.mapper;

import com.koreanit.spring.domain.User;
import com.koreanit.spring.dto.response.UserResponse;
import com.koreanit.spring.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class UserMapper {

    private UserMapper() {}

    // Entity -> Domain (단건)
    public static User toDomain(UserEntity e) {
        return User.of(
                e.getId(),
                e.getUsername(),
                e.getEmail(),
                e.getPassword(),
                e.getNickname(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // Entity -> Domain (리스트)
    public static List<User> toDomainList(List<UserEntity> entities) {
        List<User> result = new ArrayList<>(entities.size());
        for (UserEntity e : entities) {
            result.add(toDomain(e));
        }
        return result;
    }

    // Domain -> Response DTO (단건)
    public static UserResponse toResponse(User u) {
        return UserResponse.from(u);
    }

    // Domain -> Response DTO (리스트)
    public static List<UserResponse> toResponseList(List<User> users) {
        List<UserResponse> result = new ArrayList<>(users.size());
        for (User u : users) {
            result.add(toResponse(u));
        }
        return result;
    }
}
```

`mapper/PostMapper.java`

```java
package com.koreanit.spring.mapper;

import com.koreanit.spring.domain.Post;
import com.koreanit.spring.dto.response.PostResponse;
import com.koreanit.spring.entity.PostEntity;

import java.util.ArrayList;
import java.util.List;

public class PostMapper {

    private PostMapper() {}

    // Entity -> Domain (단건)
    public static Post toDomain(PostEntity e) {
        return Post.of(
                e.getId(),
                e.getUserId(),
                e.getTitle(),
                e.getContent(),
                e.getViewCount(),
                e.getCommentsCnt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // Entity -> Domain (리스트)
    public static List<Post> toDomainList(List<PostEntity> entities) {
        List<Post> result = new ArrayList<>(entities.size());
        for (PostEntity e : entities) {
            result.add(toDomain(e));
        }
        return result;
    }

    // Domain -> Response DTO (단건)
    public static PostResponse toResponse(Post p) {
        return PostResponse.from(p);
    }

    // Domain -> Response DTO (리스트)
    public static List<PostResponse> toResponseList(List<Post> posts) {
        List<PostResponse> result = new ArrayList<>(posts.size());
        for (Post p : posts) {
            result.add(toResponse(p));
        }
        return result;
    }
}

```

---

## 5-4. Service

* Repository(Entity 반환) 호출
* Entity → Domain 변환(Mapper)

`service/HelloService.java`

기존코드:
```java
public List<UserEntity> users(int limit) {
  int safeLimit = normalizeLimit(limit);
  return helloRepository.findUsers(safeLimit);
}

public UserEntity user(Long id) {
  try {
    return helloRepository.findUserById(id);
  } catch (EmptyResultDataAccessException e) {
    throw new RuntimeException("존재하지 않는 사용자입니다: id=" + id);
  }
}

public List<PostEntity> posts(int limit) {
  int safeLimit = normalizeLimit(limit);
  return helloRepository.findPosts(safeLimit);
}

public PostEntity post(Long id) {
  try {
    return helloRepository.findPostById(id);
  } catch (EmptyResultDataAccessException e) {
    throw new RuntimeException("존재하지 않는 게시글입니다: id=" + id);
  }
}
```
변경된 전체코드:
```java
package com.koreanit.spring.service;

import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import com.koreanit.spring.domain.Post;
import com.koreanit.spring.domain.User;
import com.koreanit.spring.mapper.PostMapper;
import com.koreanit.spring.mapper.UserMapper;
import com.koreanit.spring.repository.HelloRepository;

@Service
public class HelloService {

  private static final int MAX_LIMIT = 1000;

  private int normalizeLimit(int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be >= 1");
    }
    if (limit > MAX_LIMIT) {
      return MAX_LIMIT;
    }
    return limit;
  }

  private final HelloRepository helloRepository;

  public HelloService(HelloRepository helloRepository) {
    this.helloRepository = helloRepository;
  }

  public List<User> users(int limit) {
    int safeLimit = normalizeLimit(limit);
    return UserMapper.toDomainList(helloRepository.findUsers(safeLimit));
  }

  public User user(Long id) {
    try {
      return UserMapper.toDomain(helloRepository.findUserById(id));
    } catch (EmptyResultDataAccessException e) {
      throw new RuntimeException("존재하지 않는 사용자입니다: id=" + id);
    }
  }

  public List<Post> posts(int limit) {
    int safeLimit = normalizeLimit(limit);
    return PostMapper.toDomainList(helloRepository.findPosts(safeLimit));
  }

  public Post post(Long id) {
    try {
      return PostMapper.toDomain(helloRepository.findPostById(id));
    } catch (EmptyResultDataAccessException e) {
      throw new RuntimeException("존재하지 않는 게시글입니다: id=" + id);
    }
  }
}
```

---

## 5-5. Controller

* Service(Domain 반환) 호출
* Domain → Response DTO 변환(Mapper)

`controller/HelloController.java`

기존코드:
```java
@GetMapping("/hello/users")
public List<UserResponse> users() {
    return UserMapper.toResponseList(helloService.users());
}

@GetMapping("/hello/users/{id}")
public UserResponse user(@PathVariable Long id) {
    return UserMapper.toResponse(helloService.user(id));
}

@GetMapping("/hello/posts")
public List<PostResponse> posts() {
    return PostMapper.toResponseList(helloService.posts());
}

@GetMapping("/hello/posts/{id}")
public PostResponse post(@PathVariable Long id) {
    return PostMapper.toResponse(helloService.post(id));
}
```

변경된 전체코드:
```java
package com.koreanit.spring.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.koreanit.spring.dto.response.PostResponse;
import com.koreanit.spring.dto.response.UserResponse;
import com.koreanit.spring.mapper.PostMapper;
import com.koreanit.spring.mapper.UserMapper;
import com.koreanit.spring.service.HelloService;

@RestController
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping("/hello/users")
    public List<UserResponse> users(@RequestParam(defaultValue = "1000") int limit) {
        return UserMapper.toResponseList(helloService.users(limit));
    }

    @GetMapping("/hello/users/{id}")
    public UserResponse user(@PathVariable Long id) {
        return UserMapper.toResponse(helloService.user(id));
    }

    @GetMapping("/hello/posts")
    public List<PostResponse> posts(@RequestParam(defaultValue = "1000") int limit) {
        return PostMapper.toResponseList(helloService.posts(limit));
    }

    @GetMapping("/hello/posts/{id}")
    public PostResponse post(@PathVariable Long id) {
        return PostMapper.toResponse(helloService.post(id));
    }
}
```

---

## 7. 테스트 (VSCode REST Client)

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

# 실습 단계

이 단계에서는 **Response DTO를 조정하여 보안과 표현 책임을 강화**한다.

---

## 실습 1. UserResponse에서 password 제거

### 목적

* 비밀번호는 **Domain 내부 로직에서만 사용**한다
* 외부 응답(Response DTO)에서는 **구조적으로 노출 불가**하도록 한다

> Service나 Controller 실수로도 password가 나갈 수 없게 만드는 것이 목표다.

---

### 수정 대상

`dto/response/UserResponse.java`

---

### 변경 코드

```java
package com.koreanit.spring.dto.response;

import com.koreanit.spring.domain.User;
import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Domain -> Response DTO
    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.email = u.getEmail();
        r.nickname = u.getNickname();
        r.createdAt = u.getCreatedAt();
        return r;
    }
}
```

---

### 체크 포인트

* `password` 필드가 **DTO에 존재하지 않는다**
* `from()` 메서드에서도 password를 매핑하지 않는다
* 이후 Controller / Mapper 수정이 있어도 password 노출은 구조적으로 불가능

---

## 실습 2. PostResponse에 summary 필드 추가

### 목적

* 게시글 본문(content)은 길 수 있다
* 목록 응답에서 **요약(summary)** 을 제공하여 표현 책임을 DTO 가 가진다.

> 요약 로직은 Domain에 있고, **응답에 무엇을 보여줄지는 DTO가 결정**한다

---

### 전제 조건 (이미 구현됨)

`domain/Post.java`

```java
public String summary(int maxLen) {
    if (maxLen <= 0) return "";
    if (content == null) return "";
    return content.length() <= maxLen
        ? content
        : content.substring(0, maxLen) + "...";
}
```

---

### 수정 대상

`dto/response/PostResponse.java`

---

### 변경 코드

```java
package com.koreanit.spring.dto.response;

import com.koreanit.spring.domain.Post;
import java.time.LocalDateTime;

public class PostResponse {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String summary;   // 추가
    private Integer viewCount;
    private Integer commentsCnt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public Integer getViewCount() { return viewCount; }
    public Integer getCommentsCnt() { return commentsCnt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Domain -> Response DTO
    public static PostResponse from(Post p) {
        PostResponse r = new PostResponse();
        r.id = p.getId();
        r.userId = p.getUserId();
        r.title = p.getTitle();
        r.content = p.getContent();
        r.summary = p.summary(5); // 요약 길이
        r.viewCount = p.getViewCount();
        r.commentsCnt = p.getCommentsCnt();
        r.createdAt = p.getCreatedAt();
        return r;
    }
}
```

---

### 체크 포인트

* summary는 **DTO 전용 필드**다
* 요약 로직은 Domain(Post)에 있고, DTO는 이를 호출만 한다
* 표현 규칙(50자)은 DTO에서 결정한다

---

## 실습 3. Domain 파생 규칙 – 사용자 표시 이름

### 목적

* 화면에 표시할 사용자 이름 규칙을 **Domain으로 이동**한다
* DTO나 Service에서 조건 분기를 제거한다

---

### 4-1. Domain 수정

`domain/User.java`

```java
public String displayName() {
  if (nickname != null && !nickname.isBlank()) {
    return nickname + "(" + username + ")";
  }else{
    return username;
  }
}
```

의미:

* nickname이 있으면 `nickname ( username )`
* 없으면 username
* "사용자를 어떻게 표시할 것인가"라는 규칙을 Domain이 가진다

---

### 4-2. DTO 수정

`dto/response/UserResponse.java`

```java
public static UserResponse from(User u) {
    UserResponse r = new UserResponse();
    r.id = u.getId();
    r.username = u.getUsername();
    r.email = u.getEmail();
    r.nickname = u.displayName();
    r.createdAt = u.getCreatedAt();
    return r;
}
```

체크 포인트:

* DTO는 계산하지 않는다
* 표현 규칙의 원천은 Domain이다

---

## 이 장의 핵심 정리

* Repository/JdbcTemplate은 Entity를 만들고 반환한다
* Service는 Entity를 Domain으로 변환해 반환한다(Mapper로 고정)
* Controller는 Domain을 DTO로 변환해 응답한다(Mapper로 고정)
* password 같은 민감 정보는 DTO에서 제외하여 외부 노출을 차단한다

## 다음 단계

→ [**Spring Boot 애플리케이션 전체 흐름**](12-springboot_flow.md)