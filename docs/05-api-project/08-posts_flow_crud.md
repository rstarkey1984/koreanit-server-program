# Posts API 정상 흐름 CRUD

## 0. 범위

이 문서는 **Posts CRUD의 정상 흐름** 만 구현한다.

이 단계에서는 의도적으로 다음을 제외한다.

* 400 입력 검증(@Valid)
* 404 대상 없음 의미 해석
* 409 중복 제약 의미 해석
* 401/403 인증/인가 의미 해석

---

## 1. 엔드포인트

| Method | Path                | 역할             |
| ------ | ------------------- | -------------- |
| POST   | /api/posts          | 게시글 생성         |
| GET    | /api/posts?limit=20 | 게시글 목록 조회(최신순) |
| GET    | /api/posts/{id}     | 게시글 단건 조회      |
| PUT    | /api/posts/{id}     | 게시글 수정         |
| DELETE | /api/posts/{id}     | 게시글 삭제         |

응답은 전부 `ApiResponse`로 통일한다.

게시글 목록과 게시글 단건 조회는 아무나 사용할수 있게 열어둔다.

`SecurityConfig.java`
```java
// 게시글목록/단건조회 오픈
.requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
.requestMatchers(HttpMethod.GET, "/api/posts/{id}").permitAll()
```

---

## 2. 코드

패키지 기준:

* 도메인: `com.koreanit.spring.post`
* DTO: `com.koreanit.spring.post.dto.*`

### 2-1. PostCreateRequest ( Request DTO )

* 클라이언트는 `title`, `content`만 전달한다.
* `user_id`는 요청으로 받지 않고 서버(SecurityContext)에서 결정한다.
* 검증(400)은 다음 단계에서 추가한다.

`post/dto/request/PostCreateRequest.java`

```java
package com.koreanit.spring.post.dto.request;

public class PostCreateRequest {
    private String title;
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

---

### 2-2. PostUpdateRequest ( Request DTO )

* 수정 대상은 PathVariable `id`로 식별한다.
* 본 단계에서는 정상 흐름만 다루므로, 인가(본인/관리자)는 다음 단계에서 추가한다.
* 검증(400)은 다음 단계에서 추가한다.

`post/dto/request/PostUpdateRequest.java`

```java
package com.koreanit.spring.post.dto.request;

public class PostUpdateRequest {
    private String title;
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

---

### 2-3. PostRepository

* Posts 테이블에 대한 데이터 접근 계약(인터페이스)이다.
* Repository는 DB 접근만 담당하고 의미 해석(404/409 등)은 하지 않는다.
* 반환 타입은 Entity(PostEntity)로 고정한다.
* Service는 이 결과를 Domain(Post)으로 변환하여 사용한다.

`post/PostRepository.java`

```java
package com.koreanit.spring.post;

import java.util.List;

public interface PostRepository {
    long save(long userId, String title, String content);
    List<PostEntity> findAll(int limit);
    PostEntity findById(long id);
    int update(long id, String title, String content);
    int delete(long id);
}
```

---

### 2-4. JdbcPostRepository

* PostRepository의 JdbcTemplate 구현체다.
* SQL 실행과 RowMapper를 통해 ResultSet을 PostEntity로 변환한다.
* 목록은 `created_at DESC`로 최신순 조회한다.

`post/JdbcPostRepository.java`

```java
package com.koreanit.spring.post;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class JdbcPostRepository implements PostRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PostEntity> rowMapper = (rs, rowNum) -> {
        PostEntity e = new PostEntity();
        e.setId(rs.getLong("id"));
        e.setUserId(rs.getLong("user_id"));
        e.setTitle(rs.getString("title"));
        e.setContent(rs.getString("content"));
        e.setViewCount(rs.getInt("view_count"));
        e.setCommentsCnt(rs.getInt("comments_cnt"));
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null) {
            e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return e;
    };

    @Override
    public long save(long userId, String title, String content) {
        String sql = "INSERT INTO posts(user_id, title, content) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public List<PostEntity> findAll(int limit) {
        String sql = """
                SELECT id, user_id, title, content, view_count, comments_cnt, created_at, updated_at
                FROM posts
                ORDER BY created_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, rowMapper, limit);
    }

    @Override
    public PostEntity findById(long id) {
        String sql = """
                SELECT id, user_id, title, content, view_count, comments_cnt, created_at, updated_at
                FROM posts
                WHERE id = ?
                """;
        return jdbcTemplate.queryForObject(sql, rowMapper, id);
    }

    @Override
    public int update(long id, String title, String content) {
        String sql = "UPDATE posts SET title = ?, content = ? WHERE id = ?";
        return jdbcTemplate.update(sql, title, content, id);
    }

    @Override
    public int delete(long id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
```

---

### 2-5. PostService

* Posts의 정상 CRUD 흐름을 수행하는 비즈니스 계층이다.
* Repository 결과(Entity)를 Domain(Post)으로 변환해 표준 타입으로 사용한다.
* 본 단계에서는 대상 없음/중복/권한 등의 의미 해석을 하지 않는다.

`post/PostService.java`

```java
package com.koreanit.spring.post;

import com.koreanit.spring.post.dto.request.PostCreateRequest;
import com.koreanit.spring.post.dto.request.PostUpdateRequest;
import com.koreanit.spring.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post create(PostCreateRequest req) {
        long userId = SecurityUtils.currentUserId(); // 정상 로그인 가정
        long id = postRepository.save(userId, req.getTitle(), req.getContent());
        return PostMapper.toDomain(postRepository.findById(id));
    }

    public List<Post> list(int limit) {
        return PostMapper.toDomainList(postRepository.findAll(limit));
    }

    public Post get(long id) {
        return PostMapper.toDomain(postRepository.findById(id));
    }

    public Post update(long id, PostUpdateRequest req) {
        postRepository.update(id, req.getTitle(), req.getContent());
        return PostMapper.toDomain(postRepository.findById(id));
    }

    public void delete(long id) {
        postRepository.delete(id);
    }
}
```

---

### 2-6. PostController

* HTTP 요청을 받아 Service를 호출하고, 응답 DTO로 변환해 반환한다.
* Controller는 판단/예외 처리/의미 해석을 하지 않는다.
* 응답 포맷은 `ApiResponse`로 통일한다.
* 목록 조회는 `limit` 파라미터로 개수만 제어한다.

`post/PostController.java`

```java
package com.koreanit.spring.post;

import com.koreanit.spring.common.response.ApiResponse;
import com.koreanit.spring.post.dto.request.PostCreateRequest;
import com.koreanit.spring.post.dto.request.PostUpdateRequest;
import com.koreanit.spring.post.dto.response.PostResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ApiResponse<PostResponse> create(@RequestBody PostCreateRequest req) {
        Post p = postService.create(req);
        return ApiResponse.ok(PostMapper.toResponse(p));
    }

    @GetMapping
    public ApiResponse<List<PostResponse>> list(@RequestParam(defaultValue = "20") int limit) {
        List<Post> posts = postService.list(limit);
        return ApiResponse.ok(PostMapper.toResponseList(posts));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> get(@PathVariable long id) {
        Post p = postService.get(id);
        return ApiResponse.ok(PostMapper.toResponse(p));
    }

    @PutMapping("/{id}")
    public ApiResponse<PostResponse> update(@PathVariable long id, @RequestBody PostUpdateRequest req) {
        Post p = postService.update(id, req);
        return ApiResponse.ok(PostMapper.toResponse(p));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        postService.delete(id);
        return ApiResponse.ok(null);
    }
}
```

---

## 4. API 테스트 (VSCode REST Client)

- 세션 기반 인증을 사용한다.

- POST /api/posts, PUT/DELETE /api/posts/{id}는 로그인 후 쿠키가 유지되는 상태에서 테스트한다.

- VSCode REST Client는 같은 파일 내 요청 간 쿠키를 유지한다(기본 동작).

- 아래는 “정상 흐름”만 확인하는 테스트 시나리오

`posts.http`
```
@host = http://localhost:8080

### 로그아웃(세션 삭제)
POST {{host}}/api/logout

### 회원가입
POST {{host}}/api/users
Content-Type: application/json

{
  "username": "post_user1",
  "password": "pass1234!",
  "nickname": "게시글작성자",
  "email": "post_user1@example.com"
}

### 로그인 - 세션 쿠키 발급
POST {{host}}/api/login
Content-Type: application/json

{
  "username": "post_user1",
  "password": "pass1234!"
}

### 게시글 생성
# 작성자(user_id)는 서버(SecurityContext)에서 결정된다고 가정
# @name createPost
POST {{host}}/api/posts
Content-Type: application/json

{
  "title": "테스트",
  "content": "내용입니다"
}

### 게시글 단건 조회
GET {{host}}/api/posts/1

### 게시글 목록 조회
GET {{host}}/api/posts?limit=20

### 게시글 수정
PUT {{host}}/api/posts/1
Content-Type: application/json

{
  "title": "수정된 제목",
  "content": "수정된 내용"
}

### 게시글 삭제
DELETE {{host}}/api/posts/1
```

---

## 완료 기준

* 위 5개 엔드포인트가 정상 흐름으로 동작한다.

* 응답은 모두 `ApiResponse`로 반환된다.

* 실패 의미(400/404/409/401/403)는 다음 단계에서 구현한다.

---

## 다음 단계

[**Posts API 에러 핸들링 (400/404/401/403) + 조회수 증가(@Transactional)**](09-error_handling.md)