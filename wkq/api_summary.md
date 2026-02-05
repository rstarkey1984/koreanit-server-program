# API 계약서 (Endpoints & ErrorCode 기준)

본 문서는 **현재 서버 코드 기준**으로 API 엔드포인트, 요청/응답 형태, 에러 코드 매핑을 고정한다.
프론트엔드 및 다른 컨텍스트에서 **계약 문서**로 그대로 사용한다.

---

## 1. 공통 응답 포맷

모든 API는 `ApiResponse<T>` 형태로 응답한다.

### 성공 응답

```json
{
  "success": true,
  "message": "OK",
  "data": "...",
  "code": null
}
```

### 실패 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null,
  "code": "ERROR_CODE"
}
```

* HTTP Status는 `ErrorCode`에 의해 결정된다.
* 프론트에서는 **HTTP Status 의존 없이** `success / code / message`만 사용한다.

---

## 2. ErrorCode 정의

서버에서 사용하는 에러 코드는 다음으로 고정한다.

| ErrorCode          | HTTP Status | 의미         |
| ------------------ | ----------- | ---------- |
| INVALID_REQUEST    | 400         | 요청 값 검증 실패 |
| NOT_FOUND_RESOURCE | 404         | 대상 리소스 없음  |
| DUPLICATE_RESOURCE | 409         | 중복 리소스     |
| UNAUTHORIZED       | 401         | 인증되지 않음    |
| FORBIDDEN          | 403         | 권한 없음      |
| INTERNAL_ERROR     | 500         | 서버 내부 오류   |

---

## 3. Users API

Base Path: `/api/users`

### 3-1. 회원가입

* **POST** `/api/users`

Request

```json
{
  "username": "user1",
  "email": "user1@example.com",
  "password": "password",
  "nickname": "닉네임"
}
```

Response

* Success: `ApiResponse<Long>` (userId)

Failure Code

* INVALID_REQUEST (400)
* DUPLICATE_RESOURCE (409)

---

### 3-2. 유저 단건 조회

* **GET** `/api/users/{id}`

Response

* Success: `ApiResponse<UserResponse>`

Failure Code

* NOT_FOUND_RESOURCE (404)

---

### 3-3. 유저 목록 조회

* **GET** `/api/users?limit=1000`

Query

* limit (default: 1000)

Response

* Success: `ApiResponse<List<UserResponse>>`

---

### 3-4. 닉네임 변경

* **PUT** `/api/users/{id}/nickname`

Response

* Success: `ApiResponse<Void>`

Failure Code

* INVALID_REQUEST (400)
* NOT_FOUND_RESOURCE (404)
* FORBIDDEN (403)

---

### 3-5. 비밀번호 변경

* **PUT** `/api/users/{id}/password`

Failure Code

* INVALID_REQUEST (400)
* NOT_FOUND_RESOURCE (404)
* FORBIDDEN (403)

---

### 3-6. 이메일 변경

* **PUT** `/api/users/{id}/email`

Failure Code

* INVALID_REQUEST (400)
* DUPLICATE_RESOURCE (409)
* NOT_FOUND_RESOURCE (404)
* FORBIDDEN (403)

---

### 3-7. 유저 삭제

* **DELETE** `/api/users/{id}`

Failure Code

* NOT_FOUND_RESOURCE (404)
* FORBIDDEN (403)

---

## 4. Posts API

Base Path: `/api/posts`

### 4-1. 게시글 작성

* **POST** `/api/posts`

Request

```json
{
  "title": "제목",
  "content": "내용"
}
```

Response

* Success: `ApiResponse<PostResponse>`

Failure Code

* UNAUTHORIZED (401)
* INVALID_REQUEST (400)

---

### 4-2. 게시글 목록 조회

* **GET** `/api/posts?limit=20`

Query

* limit (default: 20)

Response

* Success: `ApiResponse<List<PostResponse>>`

---

### 4-3. 게시글 단건 조회

* **GET** `/api/posts/{id}`

Failure Code

* NOT_FOUND_RESOURCE (404)

---

### 4-4. 게시글 수정

* **PUT** `/api/posts/{id}`

Failure Code

* INVALID_REQUEST (400)
* NOT_FOUND_RESOURCE (404)
* UNAUTHORIZED (401)
* FORBIDDEN (403)

---

### 4-5. 게시글 삭제

* **DELETE** `/api/posts/{id}`

Response

* Success: `ApiResponse<Void>`

Failure Code

* NOT_FOUND_RESOURCE (404)
* UNAUTHORIZED (401)
* FORBIDDEN (403)

---

## 5. DTO 요약

### UserResponse

* id: Long
* username: String
* email: String
* nickname: String
* createdAt: LocalDateTime

### PostResponse

* id: Long
* userId: Long
* title: String
* content: String
* summary: String
* viewCount: Integer
* commentsCnt: Integer
* createdAt: LocalDateTime

---

## 6. 공통 실패 응답 예시

### 401 UNAUTHORIZED

```json
{
  "success": false,
  "message": "로그인이 필요합니다.",
  "data": null,
  "code": "UNAUTHORIZED"
}
```

### 403 FORBIDDEN

```json
{
  "success": false,
  "message": "권한이 없습니다.",
  "data": null,
  "code": "FORBIDDEN"
}
```

### 404 NOT_FOUND_RESOURCE

```json
{
  "success": false,
  "message": "리소스를 찾을 수 없습니다.",
  "data": null,
  "code": "NOT_FOUND_RESOURCE"
}
```

### 409 DUPLICATE_RESOURCE

```json
{
  "success": false,
  "message": "이미 존재하는 리소스입니다.",
  "data": null,
  "code": "DUPLICATE_RESOURCE"
}
```

---

## 7. 계약 원칙 요약

* 모든 API는 ApiResponse로 통일
* 에러 판단은 `success + code` 기준
* 프론트는 HTTP Status 직접 분기하지 않음
* 본 문서는 프론트/서버 간 **단일 기준 계약서**로 사용
