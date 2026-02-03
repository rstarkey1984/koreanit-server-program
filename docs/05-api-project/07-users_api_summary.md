# Users API 전체 구조 요약 (01~06)

본 문서는 Users API 구현 과정에서 **01~06 단계까지 완성된 전체 구조를 기술 문서 톤으로 정리**한 것이다.

본 단계까지 서버는 **요청 처리 흐름, 계층 책임, 실패 의미 해석, 보안 구조**가 모두 고정된 상태이며, 이후 도메인 확장 및 운영 단계의 기준 문서로 사용된다.

---

## 1. 전체 요청 처리 흐름

```text
Client HTTP Request
        ↓
AccessLogFilter
        ↓
Spring Security Filter Chain
  ├─ 인증 실패 → 401 (UNAUTHORIZED)
  ├─ 인가 실패 → 403 (FORBIDDEN)
        ↓
DispatcherServlet
        ↓ 입력 의미 해석 (400)
Controller
        ↓ 메서드 인가 실패 (403)
Service
  ├─ 대상 없음 해석 (404)
  ├─ 중복 제약 해석 (409)  
        ↓
Repository
        ↓
Database
```

요청은 **위에서 아래 방향으로 단방향 처리**되며, 실패는 발생 지점에서 의미만 결정되고 응답 변환은 공통 계층에서 수행된다.

---

## 2. 계층 구조와 책임 분리

### 2-1. 계층별 책임

| 계층                     | 책임                | 비고            |
| ---------------------- | ----------------- | ------------- |
| Filter                 | 요청 로깅, 인증 컨텍스트 준비 | 비즈니스 로직 없음    |
| Security               | 인증(401), 인가(403)  | 도메인 로직 없음     |
| Controller             | 요청 전달, 응답 반환      | 판단/예외 처리 금지   |
| Service                | 비즈니스 규칙, 실패 의미 해석 | HTTP 응답 생성 금지 |
| Repository             | DB 접근             | 의미 해석 금지      |
| GlobalExceptionHandler | 예외 → HTTP 응답 변환   | 단일 처리 지점      |

---

### 2-2. 데이터 타입 사용 규칙

```text
Repository → Entity
Service     → Domain
Controller  → DTO
```

* Entity는 DB 구조 표현 전용
* Domain은 Service 내부 표준 타입
* DTO는 외부 API 계약

계층 간 타입 혼용을 허용하지 않는다.

---

## 3. 정상 흐름 기준선

### 01. Users 정상 CRUD + 세션 로그인

정상 흐름(baseline)이란 다음 조건을 모두 만족하는 상태를 의미한다.

* 요청 데이터가 올바름
* 대상 리소스 존재
* 중복 없음
* 인증/인가 문제 없음

본 기준선 위에 이후 단계(02~06)의 실패 의미가 **점진적으로 추가** 되었다.

---

## 4. 실패 처리 구조 (의미별 분리)

### 4-1. 실패 유형별 책임

| 실패 의미      | 판단 계층                   | HTTP 상태 |
| ---------- | ----------------------- | ------- |
| 요청 형식/값 오류 | DTO + Spring Validation | 400     |
| 대상 리소스 없음  | Service                 | 404     |
| 중복 제약 위반   | Service                 | 409     |
| 인증 실패      | Security                | 401     |
| 인가 실패      | Security / Method       | 403     |

각 실패 유형은 **서로 겹치지 않으며**, 단일 계층에서만 판단된다.

---

### 4-2. Service 계층의 역할

Service 계층은 다음 책임만을 가진다.

* 정상 비즈니스 흐름 수행
* Repository 결과를 업무 의미로 해석
* 실패 시 ApiException(ErrorCode, message) 발생

HTTP 상태 코드 및 응답 포맷 생성은 Service의 책임이 아니다.

---

## 5. 공통 모듈 구성 (완료 상태)

### 5-1. 공통 응답 포맷

* 모든 응답은 ApiResponse 구조로 통일
* 성공/실패 여부는 success 필드로 판단
* 401/403 포함 전 구간 동일 포맷 유지

---

### 5-2. 전역 예외 처리

* 모든 예외는 상위로 전파
* GlobalExceptionHandler에서 단일 처리
* 로그 기록 + 공통 실패 응답 변환

---

### 5-3. 요청 로깅

* 요청 1건당 로그 1줄
* 성공/실패/예외 여부와 무관
* requestId 기준으로 로그 상호 연계 가능

---

## 6. Security 구조 요약

### 6-1. 인증 (401)

```text
Session
 → SessionAuthenticationFilter
 → Authentication 생성
 → SecurityContext 주입
```

인증 판단 기준은 세션 존재 여부가 아니라 **SecurityContext 내 Authentication 존재 여부**이다.

---

### 6-2. 인가 (403)

* URL 레벨 인가: SecurityConfig
* 메서드 레벨 인가: @PreAuthorize

권한 모델은 ROLE 기반이며, "본인 또는 관리자" 규칙은 메서드 레벨에서 처리한다.

---

## 7. 현재 서버 상태 요약

본 단계까지 서버는 다음 상태를 만족한다.

* 요청–보안–비즈니스–예외–응답이 계층별로 분리됨
* 실패 의미가 400/404/409/401/403으로 명확히 구분됨
* 공통 규칙(ApiResponse, Logging, Exception)이 전 구간에 적용됨

---

## 8. 다음 단계

이 구조를 기반으로 다음 확장이 가능하다.

* 추가 도메인(게시글, 댓글 등) 확장
* 트랜잭션 경계 및 일관성 설명
* 배치/워커 구조 분리
* 테스트 전략(단위/통합)
* 운영/아키텍처 설계 단계
