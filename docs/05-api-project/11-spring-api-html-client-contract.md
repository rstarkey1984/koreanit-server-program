# Spring Boot API → HTML Client 생성을 위한 계약 문서

> 기준 프로젝트: spring.zip
> 목적: 단일 HTML(Vue3 + Axios) API Client를 안정적으로 생성하기 위한 최소·필수 문서

---

## 1. API 계약 문서 (Endpoint Specification)

### 공통 규칙

* Base URL: `{API_BASE}` (예: [http://localhost:8080](http://localhost:8080))
* 모든 응답은 `ApiResponse<T>` 형식
* `/api/**` 는 기본적으로 인증 필요 (일부 예외 존재)

---

### 1.1 인증 / 세션

#### POST `/api/login`

* 인증: ❌
* Body

```json
{
  "username": "string",
  "password": "string"
}
```

* Success

```json
{
  "success": true,
  "data": 1
}
```

* data: userId (Long)

---

#### POST `/api/logout`

* 인증: ⭕
* Body: 없음
* Success

```json
{
  "success": true,
  "data": null
}
```

---

#### GET `/api/me`

* 인증: 선택
* Success (로그인 상태)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "test",
    "nickname": "테스트",
    "email": "test@example.com"
  }
}
```

* Success (비로그인 상태)

```json
{
  "success": true,
  "data": null
}
```

---

### 1.2 Users

#### POST `/api/users` (회원가입)

* 인증: ❌
* Body

```json
{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "email": "string?"
}
```

* Success

```json
{
  "success": true,
  "data": 1
}
```

---

#### GET `/api/users`

* 인증: ⭕ (ADMIN)
* Query

```
limit?: number (default 100)
```

* Success

```json
{
  "success": true,
  "data": UserResponse[]
}
```

---

### 1.3 Posts

#### GET `/api/posts`

* 인증: ❌
* Query

```
page: number (>=1)
limit: number (1~1000)
```

* Success

```json
{
  "success": true,
  "data": PostResponse[]
}
```

---

#### GET `/api/posts/{id}`

* 인증: ❌
* 동작

  * 조회수 증가
* Success

```json
{
  "success": true,
  "data": PostResponse
}
```

---

#### POST `/api/posts`

* 인증: ⭕
* Body

```json
{
  "title": "string",
  "content": "string"
}
```

---

#### PUT `/api/posts/{id}`

* 인증: ⭕ (본인 or ADMIN)
* Body 동일

---

#### DELETE `/api/posts/{id}`

* 인증: ⭕ (본인 or ADMIN)

---

### 1.4 Comments

#### GET `/api/posts/{postId}/comments`

* 인증: ❌
* Query

```
before?: number
limit: number
```

* 규칙

  * id 내림차순 정렬
  * before 존재 시 `id < before`
* Success

```json
{
  "success": true,
  "data": CommentResponse[]
}
```

---

#### POST `/api/posts/{postId}/comments`

* 인증: ⭕
* Body

```json
{
  "content": "string"
}
```

---

#### DELETE `/api/comments/{id}`

* 인증: ⭕ (본인 or ADMIN)

---

## 2. 공통 응답 규약 (ApiResponse)

```json
// 성공
{
  "success": true,
  "data": T
}

// 실패
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "사람이 읽을 메시지"
}
```

### 프론트 처리 규칙

* HTTP 200 이라도 `success=false`면 실패로 처리
* `message`, `code`는 최상위 필드만 사용

---

## 3. 인증 / 보안 동작 문서

* 인증 방식: HttpSession 기반
* 세션 키: `LOGIN_USER_ID`
* 인증 실패: 401 UNAUTHORIZED
* 인가 실패: 403 FORBIDDEN

### Method Security 예시

```java
@PreAuthorize("hasRole('ADMIN') or @postAuthorization.isOwner(#id)")
```

---

## 4. DTO 명세 문서

### UserResponse

```json
{
  "id": number,
  "username": string,
  "nickname": string,
  "email": string?
}
```

---

### PostResponse

```json
{
  "id": number,
  "userId": number,
  "title": string,
  "content": string,
  "viewCount": number,
  "commentsCnt": number,
  "createdAt": string?
}
```

---

### CommentResponse

```json
{
  "id": number,
  "postId": number,
  "userId": number,
  "content": string,
  "createdAt": string?
}
```

---

## 5. 페이징 / 커서 규칙 문서

### 5.1 Posts (Offset Paging)

* page >= 1
* limit: 1~1000
* offset = (page - 1) * limit

---

### 5.2 Comments (Cursor Paging)

* 기준 컬럼: comment.id
* 정렬: id DESC
* before 사용 시

```sql
WHERE id < before
ORDER BY id DESC
LIMIT limit
```

* 응답 개수 === limit → hasMore = true

---

## 이 문서의 역할

* HTML API Client 생성의 유일한 기준 문서이며
* 프론트엔드 구현 / 강의 / 과제 / 테스트 클라이언트의 계약서 역할을 한다.

문서가 변경되면 클라이언트도 반드시 함께 수정되어야 한다.
