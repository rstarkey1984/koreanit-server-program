# Posts API 정규화 및 에러 핸들링 + 조회수 증가 (@Transactional)

본 실습은 **Posts API 정상 흐름(baseline)** 위에
실패 처리(400/404/403)와 조회수 증가 로직을 **의미 해석 기준에 맞게 추가**한다.

> **401(인증)** 단계는 이미 적용되어 있어서 본 실습 범위에서 제외한다.    
> **409(중복)** 는 Posts 도메인에서 핵심 실패 의미가 아니라서 제외한다.   

---

## 실습 목표

### 실패 의미 해석 위치 고정

| HTTP 상태 | 실패 의미 | 해석 위치                                | 응답 변환              |
| --------- | --------- | ---------------------------------------- | ---------------------- |
| 400       | 입력 오류 | Request DTO + Validation                 | GlobalExceptionHandler |
| 404       | 대상 없음 | Service                                  | GlobalExceptionHandler |
| 403       | 인가 실패 | Method Security(@PreAuthorize) | accessDeniedHandler |

---

## 실습 01 — DTO 입력 검증 (400)

### PostCreateRequest / PostUpdateRequest (추가)

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
```

검증 규칙 추가
```java
@NotBlank(message = "title은 필수입니다")
@Size(max = 200, message = "title은 200자 이하여야 합니다")
private String title;

@NotBlank(message = "content는 필수입니다")
private String content;
```

### PostController (수정)
```java
import jakarta.validation.Valid;
```

각 메서드 파라미터(DTO)에 `@Valid` 추가
```java
@PostMapping
public ApiResponse<Long> create(@RequestBody @Valid PostCreateRequest req)

@PutMapping("/{id}")
public ApiResponse<Void> update(
    @PathVariable long id,
    @RequestBody @Valid PostUpdateRequest req
)
```

---

## 실습 02 — 게시글작성: 정규화

### PostService (수정)

```java
public Post create(long userId, String title, String content) {
  title = title.trim(); // 제목 앞뒤 공백 제거(정규화)
  long id = postRepository.save(userId, title, content);
  return PostMapper.toDomain(postRepository.findById(id));
}
```

---

## 실습 03 — 게시글조회: 조회수 증가 + 트랜잭션 + 404(대상 없음)

### PostRepository (추가)

```java
int increaseViewCount(long id);
```

### JdbcPostRepository (추가)
```java
@Override
public int increaseViewCount(long id) {
    String sql = """
        UPDATE posts
        SET view_count = view_count + 1
        WHERE id = ?
    """;
    return jdbcTemplate.update(sql, id);
}
```

### PostService (수정)

```java
@Transactional
public Post get(long id) {

    int updated = postRepository.increaseViewCount(id);
    if (updated == 0) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 게시글입니다. id=" + id
        );
    }

    return PostMapper.toDomain(postRepository.findById(id));
}
```

---

## 실습 04 — 게시글 수정: 정규화 + 404(대상 없음)

### PostService (수정)

```java
@Transactional
public Post update(long id, String title, String content) {
  title = title.trim();
  content = content.trim();

  int updated = postRepository.update(id, title, content);
  if (updated == 0) {
    throw new ApiException(ErrorCode.NOT_FOUND_RESOURCE, "존재하지 않는 게시글입니다. id=" + id);
  }

  return PostMapper.toDomain(postRepository.findById(id));
}
```

---

## 실습 05 — 게시글 삭제: 404(대상 없음)

### PostService (수정)

```java
public void delete(long id) {
  int deleted = postRepository.delete(id);
  if (deleted == 0) {
    throw new ApiException(
        ErrorCode.NOT_FOUND_RESOURCE,
        "존재하지 않는 게시글입니다. id=" + id);
  }
}
```

---

## 실습 06 — 게시글 수정/삭제: 인가 체크 (403)

본 실습의 목표는 **인가 실패(403)** 를 **Method Security(@PreAuthorize)** 단계에서 발생시키고,
응답 변환은 **AccessDeniedHandler(또는 GlobalExceptionHandler 정책)** 로 통일하는 것이다.

핵심 규칙:

* `PostAuthorization`은 **인가 판단만 수행**한다.
* `PostAuthorization`은 **대상 없음(404)을 해석하지 않는다.**

  * 대상이 없으면 `false`를 반환한다.
* **대상 없음(404)은 Service에서 row count 기반으로 의미 해석**한다.

---

### 1) PostRepository (추가)

```java
boolean isOwner(long postId, long userId);
```

---

### 3) JdbcPostRepository (추가)

```java
@Override
public boolean isOwner(long postId, long userId) {
    String sql = """
        SELECT 1
        FROM posts
        WHERE id = ? AND user_id = ?
        LIMIT 1
    """;

    Integer found = jdbcTemplate.query(
        sql,
        rs -> rs.next() ? 1 : null,
        postId,
        userId
    );

    return found != null;
}
```

---

### 4) 전제: Method Security 활성화(이미 적용)

```java
@EnableMethodSecurity
```

---

### 5) PostService 수정 및 @PreAuthorize 추가

```java
/**
 * 현재 로그인 사용자가 해당 게시글의 작성자인지 확인
 * - PreAuthorize 전용 보조 메서드
 */
public boolean isOwner(Long postId) {
  Long currentUserId = SecurityUtils.currentUserId();
  if (currentUserId == null) {
    return false;
  }

  return postRepository.isOwner(postId, currentUserId);
}
```

```java
@PreAuthorize("hasRole('ADMIN') or @postService.isOwner(#id)")
@Transactional
public Post update(long id, PostUpdateRequest req)

@PreAuthorize("hasRole('ADMIN') or @postService.isOwner(#id)")
public void delete(long id)
```

> 메서드 실행 이전에 인가를 먼저 검사하므로 대상이 없어도 403 에러로 내려올 수 있음

---

## 완료 기준 체크리스트

* Validation 의존성(`spring-boot-starter-validation`)이 추가되어 있다
* DTO에 Validation 규칙이 적용되고 Controller에서 `@Valid`로 400이 처리된다
* 게시글 생성 시 title이 `trim()` 정규화되어 저장된다
* 게시글 조회 시 조회수가 트랜잭션 내에서 증가한다
* 조회/수정/삭제 시 대상이 없으면 Service에서 404로 처리된다
* 수정/삭제에 `@PreAuthorize`가 적용되어 있다
* 인가 실패는 403으로 처리된다
* `PostAuthorization`은 예외 없이 boolean 판단만 수행한다
* `@PreAuthorize` 특성상 403이 404보다 먼저 나올 수 있다

---

## 다음 단계

[**Comments API CRUD**](10-comments_flow_crud.md)