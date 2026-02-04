# Posts API 에러 핸들링 (400/404/401/403) + 조회수 증가(@Transactional)


이 문서는 **Posts API 정상 흐름** 위에 다음 항목을 실습으로 추가한다.

* 400 입력 검증(@Valid)
* 404 대상 없음 의미 해석
* 401 인증 실패
* 403 인가 실패(본인 또는 관리자)
* 조회수 증가(view_count) + `@Transactional`

> 409(중복)은 Posts에서는 일반적으로 의미가 약하므로, 본 실습에서는 제외한다.

---

## 1. 목표 상태

### 1-1. 실패 의미가 계층별로 고정된다

| 실패 의미       | 판단 계층                          | 응답 변환                                    |
| ----------- | ------------------------------ | ---------------------------------------- |
| 400 (입력 오류) | DTO + Validation               | GlobalExceptionHandler                   |
| 404 (대상 없음) | Service                        | GlobalExceptionHandler                   |
| 401 (인증 실패) | Security(Filter/EntryPoint)    | Security 응답 핸들러(이미 적용)                   |
| 403 (인가 실패) | Method Security(@PreAuthorize) | Security 응답 핸들러 또는 GlobalHandler(정책에 따름) |

### 1-2. 조회수는 단건 조회 시 1 증가한다

* `GET /api/posts/{id}` 호출 시 `view_count`가 1 증가한다
* 증가(update)와 조회(select)가 **하나의 트랜잭션**으로 묶인다
* 증가된 값이 응답에 반영된다

---

## 2. 실습 순서

1. 400 입력 검증(@Valid)
2. 404 대상 없음(Service 의미 해석)
3. 401 인증 실패(보안 설정 확인)
4. 403 인가 실패(본인 또는 관리자)
5. 조회수 증가 + @Transactional

각 단계는 독립적으로 완료/테스트 가능해야 한다.

---

## 3. 실습 01 — 입력 검증 (400)

### 3-1. 목표

* `POST /api/posts`, `PUT /api/posts/{id}` 요청에서

  * title: 필수, 1~200
  * content: 필수, 1 이상
* 검증 실패 시 400 + ApiResponse.fail 형태로 응답된다

### 3-2. TODO

* `PostCreateRequest`, `PostUpdateRequest`에 Bean Validation 애너테이션 추가
* `PostController`의 `@RequestBody`에 `@Valid` 추가

### 3-3. 체크

* title 미입력 / 빈 문자열 / 200 초과
* content 미입력 / 빈 문자열

---

## 4. 실습 02 — 대상 없음 해석 (404)

### 4-1. 목표

* 존재하지 않는 게시글 id로 조회/수정/삭제 시 404로 해석된다

### 4-2. TODO

- `PostEntity findById(long id)` 유지 + 예외를 Service에서 404로 변환


Service 책임

* `get`, `update`, `delete`, (조회수 증가 포함)에서 대상이 없으면

  * `throw new ApiException(ErrorCode.NOT_FOUND_RESOURCE, "post not found")` 형태로 발생

---

## 5. 실습 03 — 인증 실패 (401)

### 5-1. 목표

* 로그인하지 않은 상태에서 `/api/posts/**` 접근 시 401
* 응답은 ApiResponse(JSON)로 유지

### 5-2. TODO

* 이미 Users 단계에서 완료된 보안 설정을 그대로 사용한다
* 다음을 점검한다

  * 허용: `POST /api/login`, `POST /api/users`
  * 차단: 그 외 `/api/**`

테스트는 쿠키 없는 상태에서 Posts API 호출로 확인한다.

---

## 6. 실습 04 — 인가 실패 (403)

### 6-1. 목표

* 수정/삭제는 다음 규칙으로 제한된다

  * 관리자: 모든 게시글 수정/삭제 가능
  * 일반 사용자: 본인 글만 수정/삭제 가능

### 6-2. TODO

* `PostService.update`, `PostService.delete`에 `@PreAuthorize` 적용

표현식 예시(프로젝트 규칙에 맞게 조정)

* 관리자 또는 본인

  * `hasRole('ADMIN') or @postAuth.isOwner(#id)` 같은 형태

권장 구현(학습용)

1. Service에서 post 조회 후 userId 비교(인가 판단을 Service에 두지 않기 위해, 메서드 시그니처 기반으로 helper를 만들거나 security util을 사용)
2. `PostAuthorization` 같은 컴포넌트를 만들어 SpEL에서 호출

주의

* 본 단계는 “인가 실패를 403으로 처리하는 구조”가 목적이다
* 실제 구현 방식은 단순해야 한다

---

## 7. 실습 05 — 조회수 증가(view_count) + @Transactional

### 7-1. 목표

* `GET /api/posts/{id}` 호출 시 `view_count`가 1 증가한다
* 증가(update) + 조회(select)가 하나의 트랜잭션으로 묶인다

### 7-2. Repository 변경

`PostRepository`에 아래 메서드 추가
```java
int increaseViewCount(long id);
```

`JdbcPostRepository`에 increaseViewCount 메서드 추가

```java
// ------------------------
// 실습 05: 조회수 증가
// ------------------------
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

### 7-3. Service 변경

`PostService.get(long id)`를 다음 흐름으로 변경

1. 대상 존재 확인(없으면 404)
2. 조회수 증가
3. 다시 조회해서 Domain으로 반환

이 메서드에 `@Transactional` 적용

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

### 7-4. 왜 @Transactional 인가

* 단건 조회는 “읽기”처럼 보이지만, 조회수 증가가 포함되면 쓰기 작업이 된다
* 증가와 재조회가 분리되면, 중간 실패/동시성에서 의도하지 않은 결과가 나올 수 있다
* 하나의 트랜잭션으로 묶어 “조회 요청 한 번 = 증가 한 번”을 보장한다

---

## 8. REST Client 테스트 시나리오

테스트 순서

1. 로그인
2. 게시글 생성
3. 단건 조회 3회
4. 단건 응답의 view_count가 1씩 증가하는지 확인
5. 다른 사용자로 로그인 후 수정/삭제 시 403 확인
6. 로그아웃/쿠키 제거 후 호출 시 401 확인
7. 존재하지 않는 게시글 id로 호출 시 404 확인

---

## 완료 기준

* 400: DTO 검증 실패가 400으로 응답된다
* 404: 존재하지 않는 id 접근이 404로 응답된다
* 401: 비로그인 접근이 401로 응답된다
* 403: 타인 글 수정/삭제가 403으로 응답된다
* 조회수: 단건 조회 시 view_count가 증가하며, 증가와 조회가 @Transactional로 묶인다

---

## 다음 단계

* 검색/페이징(ORDER BY + LIMIT/OFFSET)
* 조회수 중복 방지(로그 테이블(post_view_logs) 기반)
