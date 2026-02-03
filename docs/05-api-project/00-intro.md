# Users & Posts API — 전체 구조

본 문서는 본 강의에서 구현할 **전체 구조와 API 범위를 한 장으로 요약**한 문서다.
세부 구현 규칙과 예외 처리 방식은 이후 단계 문서에서 다룬다.

---

## 1. 서버 전체 요청 흐름

```text
Client
  ↓
AccessLogFilter
  ↓
Spring Security Filter Chain
  ├─ 인증 실패 → 401
  ├─ 인가 실패 → 403
  ↓
DispatcherServlet
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

* 요청은 **위 → 아래 단방향**으로 흐른다
* 각 계층은 **자기 책임만 수행**한다
* 실패는 발생 지점에서 **의미만 결정**되고, 응답은 공통 규칙으로 변환된다

---

## 2. 계층별 책임 요약

| 계층                     | 책임                |
| ---------------------- | ----------------- |
| Filter                 | 요청 로깅, 인증 컨텍스트 준비 |
| Security               | 인증(401), 인가(403)  |
| Controller             | 요청 전달, 응답 반환      |
| Service                | 비즈니스 규칙, 실패 의미 해석 |
| Repository             | DB 접근             |
| GlobalExceptionHandler | 예외 → HTTP 응답 변환   |

---

## 3. 데이터 타입 사용 규칙

```text
Repository → Entity
Service     → Domain
Controller  → DTO
```

* Entity: DB 구조 표현
* Domain: Service 내부 표준 타입
* DTO: 외부 API 계약

계층 간 타입 혼용을 허용하지 않는다.

---

## 4. 인증 / 인가 구조 요약

### 인증 (401)

```text
Session
 → SessionAuthenticationFilter
 → Authentication 생성
 → SecurityContext 주입
```

* 인증 기준은 **세션 존재 여부가 아니라**
  **SecurityContext 내 Authentication 존재 여부**

### 인가 (403)

* URL 단위 인가: SecurityConfig
* 메서드 단위 인가: @PreAuthorize
* 권한 모델: ROLE 기반 (ROLE_USER / ROLE_ADMIN)

---

## 5. API 엔드포인트 전체 목록

### Users API

| Method | Path                     | 설명           |
| ------ | ------------------------ | ------------ |
| POST   | /api/users               | 회원가입         |
| POST   | /api/login               | 로그인 (세션 생성)  |
| POST   | /api/logout              | 로그아웃         |
| GET    | /api/me                  | 내 정보 조회      |
| GET    | /api/users               | 사용자 목록 (관리자) |
| GET    | /api/users/{id}          | 사용자 단건 (관리자) |
| PUT    | /api/users/{id}/nickname | 닉네임 변경       |
| PUT    | /api/users/{id}/password | 비밀번호 변경      |
| DELETE | /api/users/{id}          | 사용자 삭제       |

---

### Posts API

| Method | Path            | 설명     |
| ------ | --------------- | ------ |
| POST   | /api/posts      | 게시글 생성 |
| GET    | /api/posts      | 게시글 목록 |
| GET    | /api/posts/{id} | 게시글 단건 |
| PUT    | /api/posts/{id} | 게시글 수정 |
| DELETE | /api/posts/{id} | 게시글 삭제 |

* 수정/삭제는 **본인 또는 관리자** 규칙 적용

---

### Comments API (실습)

| Method | Path                         | 설명    |
| ------ | ---------------------------- | ----- |
| POST   | /api/posts/{postId}/comments | 댓글 작성 |
| GET    | /api/posts/{postId}/comments | 댓글 목록 |
| DELETE | /api/comments/{id}           | 댓글 삭제 |

---

## 이 문서의 역할

* 강의 전체의 **구조·범위 안내용 지도 문서**다

* 이후 모든 구현은 **이 구조를 전제로 진행**된다

---

## 다음 단계

→ [**Users 정상 흐름 CRUD + 세션 로그인**](01-normal_flow_crud.md)