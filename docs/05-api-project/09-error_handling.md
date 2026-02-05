# Posts API 에러 핸들링 + 조회수 증가 (@Transactional)

본 실습은 **Posts API 정상 흐름(baseline)** 위에
실패 처리(400/404/401/403)와 조회수 증가 로직을 **의미 해석 기준에 맞게 추가**한다.

> 409(중복)는 Posts 도메인에서 핵심 실패 의미가 아니므로 본 실습 범위에서 제외한다.

---

## 1. 실습 목표

### 1-1. 실패 의미 해석 위치 고정

| HTTP 상태 | 실패 의미 | 해석 위치                          | 응답 변환                             |
| ------- | ----- | ------------------------------ | --------------------------------- |
| 400     | 입력 오류 | Request DTO + Validation       | GlobalExceptionHandler            |
| 404     | 대상 없음 | Service                        | GlobalExceptionHandler            |
| 401     | 인증 실패 | Security(Filter / EntryPoint)  | Security Handler                  |
| 403     | 인가 실패 | Method Security(@PreAuthorize) | Security Handler 또는 GlobalHandler |

### 1-2. 조회수 증가 규칙

* `GET /api/posts/{id}` 호출 시 `view_count` 1 증가
* 증가(update) + 재조회(select)는 **하나의 트랜잭션**
* 응답에는 **증가된 view_count**가 반영되어야 한다

---

## 2. 실습 구성

본 실습은 아래 **5단계**로 구성된다.

* 실습 01 — 입력 검증 (400)
* 실습 02 — 대상 없음 해석 (404)
* 실습 03 — 인증 실패 (401)
* 실습 04 — 인가 실패 (403)
* 실습 05 — 조회수 증가 + @Transactional

각 실습은 **독립적으로 구현 및 테스트 가능**해야 한다.

---

## 실습 01 — 입력 검증 (400)

### 목표

다음 API에서 요청 값 검증을 수행한다.

* `POST /api/posts`
* `PUT /api/posts/{id}`

검증 규칙

* `title`

  * 필수
  * 길이 1 ~ 200
* `content`

  * 필수
  * 길이 1 이상

검증 실패 시

* HTTP 400
* `ApiResponse.fail(...)` 형식으로 응답

### 구현 TODO

* `PostCreateRequest`, `PostUpdateRequest`

  * Bean Validation 애너테이션 추가
* `PostController`

  * `@RequestBody @Valid` 적용

### 체크 포인트

* title: null / 빈 문자열 / 200자 초과
* content: null / 빈 문자열

---

## 실습 02 — 대상 없음 해석 (404)

### 목표

존재하지 않는 게시글 id로 다음 작업 수행 시
**404 (NOT_FOUND_RESOURCE)** 로 해석된다.

* 단건 조회
* 수정
* 삭제
* 조회수 증가 포함 단건 조회

### 구현 TODO

* Repository는 그대로 유지

  * `PostEntity findById(long id)`
* Service에서 의미 해석 수행

Service 규칙

```java
throw new ApiException(
    ErrorCode.NOT_FOUND_RESOURCE,
    "존재하지 않는 게시글입니다. id=" + id
);
```

포인트

* DB 예외(EmptyResultDataAccessException)는 **Service에서 HTTP 의미(404)로 변환**한다.

---

## 실습 03 — 인증 실패 (401)

### 목표

* 로그인하지 않은 상태에서 `/api/posts/**` 접근 시 401
* 응답은 JSON(ApiResponse) 형식 유지

### 구현 TODO

* Users 단계에서 구성한 Security 설정 그대로 사용
* 접근 규칙 점검

허용

* `POST /api/login`
* `POST /api/users`

차단

* 그 외 `/api/**`

### 체크 포인트

* 세션/쿠키 없는 상태에서 Posts API 호출 → 401

---

## 실습 04 — 인가 실패 (403)

### 목표

게시글 수정/삭제 권한 규칙

* 관리자

  * 모든 게시글 수정/삭제 가능
* 일반 사용자

  * 본인 게시글만 수정/삭제 가능

권한 위반 시 **403 (FORBIDDEN)**

### 구현 TODO

* `PostService.update`
* `PostService.delete`

위 메서드에 `@PreAuthorize` 적용

권장 방식(학습용)

```java
@PreAuthorize("hasRole('ADMIN') or @postAuthorization.isOwner(#id)")
public void update(long id, PostUpdateRequest req) { ... }
```

`isOwner(id)` 내부 로직

* 현재 로그인 사용자 id 조회
* 게시글의 `user_id`와 비교

### 체크 포인트

* 본인 글 수정/삭제 → 성공
* 타인 글 수정/삭제 → 403
* 관리자 → 타인 글 수정/삭제 성공

---

## 실습 05 — 조회수 증가 + @Transactional

### 목표

* `GET /api/posts/{id}` 호출 시 `view_count` 1 증가
* 증가(update)와 재조회(select)를 **하나의 트랜잭션**으로 처리

### Repository 변경

```java
int increaseViewCount(long id);
```

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

### Service 변경

```java
@Transactional
public Post get(long id) {

    // 1) 조회수 증가 + 대상 존재 확인
    int updated = postRepository.increaseViewCount(id);
    if (updated == 0) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 게시글입니다. id=" + id
        );
    }

    // 2) 증가된 값 재조회
    return PostMapper.toDomain(postRepository.findById(id));
}
```

### @Transactional을 사용하는 이유

* 단건 조회처럼 보여도 **쓰기(update)** 가 포함된다
* 증가와 재조회가 분리되면

  * 중간 실패
  * 동시성 상황에서 불일치 발생 가능
* 하나의 트랜잭션으로 묶어 **“조회 1회 = 조회수 1 증가”** 를 보장한다

---

## 3. REST Client 테스트 시나리오

1. 로그인
2. 게시글 생성
3. 단건 조회 3회
4. 응답 `view_count`가 1씩 증가하는지 확인
5. 다른 사용자 로그인 → 수정/삭제 시 403
6. 로그아웃 또는 쿠키 제거 → 호출 시 401
7. 존재하지 않는 id 호출 → 404

---

## 완료 기준

* 400: DTO 검증 실패 → 400(ApiResponse.fail)
* 404: 존재하지 않는 id → 404(ApiResponse.fail)
* 401: 비로그인 접근 → 401(ApiResponse.fail)
* 403: 권한 없는 수정/삭제 → 403(ApiResponse.fail)
* 조회수: 단건 조회 시 증가하며 트랜잭션으로 묶여 있음

---

## 다음 단계

* Comments API CRUD
