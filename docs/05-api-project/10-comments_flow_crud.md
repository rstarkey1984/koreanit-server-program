# Comments API CRUD 

본 장에서는 프로젝트의 패키지 구조 규칙에 맞춰 **Comments 도메인 파일을 생성**하고
**CRUD + 실패 의미 해석(400 / 401 / 403 / 404)** 까지를 **한 장에서 모두 완성**한다.

또한 댓글 삭제 인가 로직을
**Service 내부 if 문이 아닌 `@PreAuthorize` 기반 Method Security로 고정**하여
실무에서 사용하는 구조와 동일한 기준을 확정한다.

이 문서가 끝나면 Comments API는 **실무 기준으로 바로 사용 가능한 상태**가 된다.

---

## 1. 이번 장의 목표

* Comments 도메인의 **패키지 구조와 파일 역할 고정**
* Comments API의 **CRUD 정상 흐름 완성**
* 실패 상황을 **의미 단위로 해석하여 위치별로 처리**

---

## 2. 패키지 구조 규칙

Posts 도메인과 동일한 규칙을 따른다.

* `dto`만 하위 패키지로 분리
* Domain / Entity / Repository / Service / Controller는 동일 패키지에 둔다

```text
com.koreanit.spring.comment
  ├─ Comment.java
  ├─ CommentEntity.java
  ├─ CommentMapper.java
  ├─ CommentRepository.java
  ├─ JdbcCommentRepository.java
  ├─ CommentService.java
  ├─ CommentController.java
  └─ dto
      ├─ request
      │   └─ CommentCreateRequest.java
      └─ response
          └─ CommentResponse.java
```

---

## 3. 엔드포인트 구성

| Method | Path                         | 설명           |
| ------ | ---------------------------- | -------------- |
| POST   | /api/posts/{postId}/comments | 댓글 생성      |
| GET    | /api/posts/{postId}/comments | 댓글 목록 조회 |
| DELETE | /api/comments/{id}           | 댓글 삭제      |

정책:

* 댓글 목록 조회 → **비로그인 허용**
* 댓글 생성 / 삭제 → **로그인 필요**
* 댓글 삭제 → **관리자(ADMIN)는 모두 가능 / 일반 사용자는 본인만 가능**

---

## 4. Security 설정 (조회 오픈)

댓글 목록 조회만 공개 API로 설정한다.

```java
// 댓글목록 허용
.requestMatchers(HttpMethod.GET, "/api/posts/*/comments").permitAll()
```

결과:

* GET 댓글 목록 → permitAll
* POST / DELETE → authenticated
* 삭제 인가 → Method Security(@PreAuthorize) + Service 유틸 메서드로 처리

---

## 5. 실패 의미와 처리 위치 (최종 기준)

| 상황                              | HTTP | 의미               | 처리 위치                         |
| --------------------------------- | ---- | ------------------ | --------------------------------- |
| content(dto) 비어있음 / limit ≤ 0 | 400  | INVALID_REQUEST    | DTO / Service                     |
| 로그인 안 됨                      | 401  | UNAUTHORIZED       | Security                          |
| 댓글 없음                         | 404  | NOT_FOUND_RESOURCE | Service                           |
| 본인 댓글 아님(일반 사용자)       | 403  | FORBIDDEN          | Method Security (`@PreAuthorize`) |

* **401**: Security Filter 단계에서 차단
* **403**: 메서드 진입 전(Method Security) 차단
* **404**: Service에서 대상 의미 해석
* Service 메서드 본문: “인가 완료된 상태에서 비즈니스 트랜잭션 수행”

---

## 6. Method Security 적용

### Method Security 활성화

```java
@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {
}
```

`@PreAuthorize`를 사용하기 위한 필수 설정이다.

---

## 7. 코드 전체 (파일별)

> 아래 코드는 **CRUD + 400 / 401 / 403 / 404까지 모두 포함한 최종 기준 코드**다.
> 파일 경로는 `src/main/java/` 기준이다.

---

### 7-1. CommentCreateRequest

`com/koreanit/spring/comment/dto/request/CommentCreateRequest.java`

```java
package com.koreanit.spring.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CommentCreateRequest {

    @NotBlank(message = "content는 필수입니다")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

---

### 7-2. CommentResponse

`com/koreanit/spring/comment/dto/response/CommentResponse.java`

```java
package com.koreanit.spring.comment.dto.response;

import java.time.LocalDateTime;

public class CommentResponse {

    private final long id;
    private final long postId;
    private final long userId;
    private final String content;
    private final LocalDateTime createdAt;

    public CommentResponse(long id, long postId, long userId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getPostId() { return postId; }
    public long getUserId() { return userId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

---

### 7-3. Domain: Comment

`com/koreanit/spring/comment/Comment.java`

```java
package com.koreanit.spring.comment;

import java.time.LocalDateTime;

public class Comment {

    private final long id;
    private final long postId;
    private final long userId;
    private final String content;
    private final LocalDateTime createdAt;

    private Comment(long id, long postId, long userId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Comment of(long id, long postId, long userId, String content, LocalDateTime createdAt) {
        return new Comment(id, postId, userId, content, createdAt);
    }

    public long getId() { return id; }
    public long getPostId() { return postId; }
    public long getUserId() { return userId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

---

### 7-4. Entity: CommentEntity

`com/koreanit/spring/comment/CommentEntity.java`

```java
package com.koreanit.spring.comment;

import java.time.LocalDateTime;

public class CommentEntity {

    private long id;
    private long postId;
    private long userId;
    private String content;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

---

### 7-5. Mapper: CommentMapper

`com/koreanit/spring/comment/CommentMapper.java`

```java
package com.koreanit.spring.comment;

import java.util.ArrayList;
import java.util.List;

import com.koreanit.spring.comment.dto.response.CommentResponse;

public class CommentMapper {

    private CommentMapper() {}

    public static Comment toDomain(CommentEntity e) {
        return Comment.of(
            e.getId(),
            e.getPostId(),
            e.getUserId(),
            e.getContent(),
            e.getCreatedAt()
        );
    }

    public static List<Comment> toDomainList(List<CommentEntity> entities) {
        List<Comment> result = new ArrayList<>(entities.size());
        for (CommentEntity e : entities) {
            result.add(toDomain(e));
        }
        return result;
    }

    public static CommentResponse toResponse(Comment c) {
        return new CommentResponse(
            c.getId(),
            c.getPostId(),
            c.getUserId(),
            c.getContent(),
            c.getCreatedAt()
        );
    }

    public static List<CommentResponse> toResponseList(List<Comment> list) {
        List<CommentResponse> result = new ArrayList<>(list.size());
        for (Comment c : list) {
            result.add(toResponse(c));
        }
        return result;
    }
}
```

---

### 7-6. CommentRepository

`com/koreanit/spring/comment/CommentRepository.java`

```java
package com.koreanit.spring.comment;

import java.util.List;

public interface CommentRepository {

    long save(long postId, long userId, String content);

    CommentEntity findById(long id);

    List<CommentEntity> findAllByPostId(long postId, Long beforeId, int limit);

    int deleteById(long id);

    boolean isOwner(long commentId, long userId);
}
```

---

### 7-7. JdbcCommentRepository

`com/koreanit/spring/comment/JdbcCommentRepository.java`

```java
package com.koreanit.spring.comment;

import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCommentRepository implements CommentRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcCommentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final RowMapper<CommentEntity> rowMapper = (rs, rowNum) -> {
    CommentEntity e = new CommentEntity();
    e.setId(rs.getLong("id"));
    e.setPostId(rs.getLong("post_id"));
    e.setUserId(rs.getLong("user_id"));
    e.setContent(rs.getString("content"));
    e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    return e;
  };

  @Override
  public long save(long postId, long userId, String content) {
    String sql = """
            INSERT INTO comments (post_id, user_id, content, created_at)
            VALUES (?, ?, ?, NOW())
        """;

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(con -> {
      var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, postId);
      ps.setLong(2, userId);
      ps.setString(3, content);
      return ps;
    }, keyHolder);

    return keyHolder.getKey().longValue();
  }

  @Override
  public CommentEntity findById(long id) {
    String sql = """
            SELECT id, post_id, user_id, content, created_at
            FROM comments
            WHERE id = ?
        """;
    return jdbcTemplate.queryForObject(sql, rowMapper, id);
  }

  @Override
  public List<CommentEntity> findAllByPostId(long postId, Long beforeId, int limit) {

    String sql;
    Object[] args;

    if (beforeId == null) {
      sql = """
              SELECT id, post_id, user_id, content, created_at
              FROM comments
              WHERE post_id = ?
              ORDER BY id DESC
              LIMIT ?
          """;
      args = new Object[] { postId, limit };
    } else {
      sql = """
              SELECT id, post_id, user_id, content, created_at
              FROM comments
              WHERE post_id = ?
                AND id < ?
              ORDER BY id DESC
              LIMIT ?
          """;
      args = new Object[] { postId, beforeId, limit };
    }

    return jdbcTemplate.query(sql, rowMapper, args);
  }

  @Override
  public int deleteById(long id) {
    String sql = "DELETE FROM comments WHERE id = ?";
    return jdbcTemplate.update(sql, id);
  }

  @Override
  public boolean isOwner(long commentId, long userId) {
      String sql = """
          SELECT COUNT(*)
          FROM comments
          WHERE id = ? AND user_id = ?
      """;

      int count = jdbcTemplate.queryForObject(
          sql,
          Integer.class,
          commentId,
          userId
      );

      return count > 0;
  }
}
```

---

### 7-8. CommentService

`com/koreanit/spring/comment/CommentService.java`

```java
package com.koreanit.spring.comment;

import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreanit.spring.common.error.ApiException;
import com.koreanit.spring.common.error.ErrorCode;
import com.koreanit.spring.post.PostRepository;
import com.koreanit.spring.security.SecurityUtils;

@Service
public class CommentService {

  private static final int MAX_LIMIT = 1000;

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;

  public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
    this.commentRepository = commentRepository;
    this.postRepository = postRepository;
  }

  private int normalizeLimit(int limit) {
    if (limit <= 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "limit 값이 유효하지 않습니다");
    }
    return Math.min(limit, MAX_LIMIT);
  }

  @Transactional
  public Comment create(long postId, long userId, String content) {
    long id = commentRepository.save(postId, userId, content);
    postRepository.increaseCommentsCnt(postId);
    return CommentMapper.toDomain(commentRepository.findById(id));
  }

  public List<Comment> list(long postId, Long before, int limit) {
    return CommentMapper.toDomainList(
        commentRepository.findAllByPostId(postId, before, normalizeLimit(limit)));
  }

  public boolean isOwner(long id) {
    Long userId = SecurityUtils.currentUserId();
    if (userId == null) {
      return false;
    }
    return commentRepository.isOwner(id, userId);
  }

  @PreAuthorize("hasRole('ADMIN') or @commentService.isOwner(#id)")
  @Transactional
  public void delete(long id) {

    try {
      CommentEntity comment = commentRepository.findById(id);
      int deleted = commentRepository.deleteById(id);
      if (deleted == 0) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 댓글입니다. id=" + id);
      }
      if (comment.getPostId() != null)
        postRepository.decreaseCommentsCnt(comment.getPostId());

    } catch (EmptyResultDataAccessException e) {
      throw new ApiException(ErrorCode.NOT_FOUND_RESOURCE, "댓글이 존재하지 않습니다");
    }

  }

}
```

---

### 7-9. CommentController

`com/koreanit/spring/comment/CommentController.java`

```java
package com.koreanit.spring.comment;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreanit.spring.comment.dto.request.CommentCreateRequest;
import com.koreanit.spring.comment.dto.response.CommentResponse;
import com.koreanit.spring.common.response.ApiResponse;
import com.koreanit.spring.security.SecurityUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @PostMapping("/posts/{postId}/comments")
  public ApiResponse<CommentResponse> create(
      @PathVariable long postId,
      @Valid @RequestBody CommentCreateRequest req) {
    Long userId = SecurityUtils.currentUserId();
    Comment created = commentService.create(postId, userId, req.getContent());
    return ApiResponse.ok(CommentMapper.toResponse(created));
  }

  @GetMapping("/posts/{postId}/comments")
  public ApiResponse<List<CommentResponse>> list(
      @PathVariable long postId,
      @RequestParam(required = false) Long before,
      @RequestParam(defaultValue = "20") int limit) {
    return ApiResponse.ok(
        CommentMapper.toResponseList(commentService.list(postId, before, limit)));
  }

  @DeleteMapping("/comments/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    commentService.delete(id);
    return ApiResponse.ok();
  }
}
```

---

## API 테스트 (VSCode REST Client)

```http
### 댓글 생성 (로그인 필요)
POST {{host}}/api/posts/100039/comments
Content-Type: {{json}}

{
  "content": "첫 댓글입니다"
}

### 댓글 목록 조회 (공개)
GET {{host}}/api/posts/100039/comments?limit=50

### 댓글 삭제 (로그인 필요 + 본인만 가능)
DELETE {{host}}/api/comments/1
```

---

## 이번 장 완료 기준

* Comments 패키지 및 파일 생성 완료
* 애플리케이션 정상 실행
* 댓글 생성 / 목록 조회 / 삭제 정상 동작
* 댓글 생성/삭제 시 `posts.comments_cnt` 증감 확인
* 400 / 401 / 403 / 404가 의도한 위치에서 발생
* Service 내부에 인가 if 문이 없고, 인가 책임이 `@PreAuthorize`로 고정됨

---

## 다음 단계

[**Spring Boot API → HTML Client 생성을 위한 계약 문서**](11-spring-api-html-client-contract.md)