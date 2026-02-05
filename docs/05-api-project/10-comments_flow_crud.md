# Comments API 실습

본 문서는 **Comments API 실습을 시작하기 전에 제공되는 기준 코드**를 정리한 것이다.
이 단계에서는 **엔드포인트, DTO / Domain / Entity, Repository 인터페이스와 구현체**까지를 제공하고,
Service와 Controller의 내부 로직은 실습에서 직접 구현하도록 남겨둔다.

핵심 목표는 다음과 같다.

* 댓글 도메인의 **데이터 구조와 책임 경계**를 고정한다
* Entity → Domain → DTO 흐름을 Posts API와 동일한 규칙으로 유지한다
* DB 접근 방식(JdbcTemplate)을 기준 구현으로 제공한다

---

## 1. 엔드포인트 정의

| Method | Path                         | 역할       |
| ------ | ---------------------------- | -------- |
| POST   | /api/posts/{postId}/comments | 댓글 생성    |
| GET    | /api/posts/{postId}/comments | 댓글 목록 조회 |
| DELETE | /api/comments/{id}           | 댓글 삭제    |

---

## 2. Request DTO

### CommentCreateRequest

```java
public class CommentCreateRequest {

    @NotBlank(message = "content는 필수입니다.")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

* 댓글 생성 요청에서 **클라이언트가 전달하는 입력 값**을 담당한다
* Bean Validation을 통해 **400 입력 오류를 Controller 단계에서 차단**한다
* 이 DTO는 Controller에서만 사용되며 Service로 그대로 전달되지 않는다

---

## 3. Response DTO

### CommentResponse

```java
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

* 댓글 조회 결과를 **외부 응답 형태로 고정**하기 위한 DTO
* 내부 Domain 객체를 그대로 노출하지 않기 위해 분리한다
* Controller에서만 사용되며 ApiResponse의 data로 감싸진다

---

## 4. Domain

### Comment

```java
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

* Service 계층에서 다루는 **표준 타입(domain)**
* 비즈니스 로직과 상태 해석의 기준이 되는 객체다
* Repository(Entity)와 Controller(DTO) 사이의 완충 역할을 한다

---

## 5. Entity

### CommentEntity

```java
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

* Database 테이블(`comments`) 구조를 그대로 반영한 객체
* Repository 계층에서만 사용된다
* 비즈니스 의미 해석이나 응답 책임을 가지지 않는다

---

## 6. Mapper

### CommentMapper

```java
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

* 계층 간 타입 변환을 전담하는 유틸리티
* 변환 로직을 한 곳에 모아 Controller/Service의 책임을 단순화한다
* 규칙이 바뀌어도 Mapper만 수정하면 된다

---

## 7. Repository 인터페이스

### CommentRepository

```java
public interface CommentRepository {

    long save(long postId, long userId, String content);

    CommentEntity findById(long id);

    List<CommentEntity> findAllByPostId(long postId, int limit);

    int deleteById(long id);
}
```

* 댓글 데이터 접근에 대한 **계약(interface)**
* Service는 구현체가 아닌 인터페이스에만 의존한다
* DB 기술(JdbcTemplate)은 이 계층 아래로 숨긴다

---

## 8. JdbcTemplate 구현체

### JdbcCommentRepository

```java
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
    public List<CommentEntity> findAllByPostId(long postId, int limit) {
        String sql = """
            SELECT id, post_id, user_id, content, created_at
            FROM comments
            WHERE post_id = ?
            ORDER BY id DESC
            LIMIT ?
        """;
        return jdbcTemplate.query(sql, rowMapper, postId, limit);
    }

    @Override
    public int deleteById(long id) {
        String sql = "DELETE FROM comments WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
```

* CommentRepository의 **JdbcTemplate 기반 구현체**
* SQL과 RowMapper를 통해 DB ↔ Entity 변환을 담당한다
* 비즈니스 의미 판단이나 권한 로직은 포함하지 않는다

---

## 정리

이 문서까지 제공되면, 실습에서는 다음만 구현하면 된다.

* CommentService 내부 로직
* CommentController에서 Service 호출 및 응답 매핑


