# Controller 구현 – 회원가입 엔드포인트 추가

이 문서에서는 **회원가입 API의 Controller를 구현**하되,
IDE 에러로 흐름이 끊기지 않도록 **Repository / Service 메서드 뼈대를 먼저 고정**한 뒤
Controller를 연결한다.

> 이 단계의 핵심은
> **구현이 아니라 흐름과 계약(메서드 시그니처)을 먼저 고정하는 것**이다.

---

## 1. 이 단계의 목표

이 문서를 끝내면 다음이 성립해야 한다.

* `POST /api/v1/users` 요청이 Controller → Service 까지 도달한다
* Repository / Service에 **컴파일 가능한 메서드 시그니처**가 존재한다
* Controller는 항상 `ApiResponse` 형태로만 응답한다

> 실제 SQL, 검증, 비밀번호 처리 로직은 다음 단계에서 구현한다.

---

## 2. Repository – 회원가입 관련 메서드 추가

> 데이터 접근 계층의 **계약(contract)** 만 먼저 정의한다.

```java
// 사용자 아이디 중복 개수 조회
public int countByUsername(String username) {
    return 0; // TODO: 다음 단계에서 SQL 구현
}

// 사용자 저장 후 생성된 ID 반환
public long insertUser(String username,
                       String password,
                       String nickname,
                       String email) {
    return 1L; // TODO: 다음 단계에서 INSERT + PK 반환
}
```

포인트:

* SQL / JdbcTemplate 코드는 일부러 작성하지 않는다
* Service가 무엇을 호출할지만 먼저 고정한다

---

## 3. Service – 회원가입 메서드 추가

> Controller가 호출할 **회원가입 흐름의 진입점**을 먼저 만든다.

```java
public UserSignupResponse signup(UserSignupRequest req) {

  // 1) 아이디 중복 여부 확인 (아직 구현은 안 함)
  int duplicated = userRepository.countByUsername(req.username);

  // TODO: 다음 단계에서
  // duplicated > 0 인 경우 ApiException 발생

  // 2) 사용자 저장 (아직 구현은 안 함)
  long userId = userRepository.insertUser(
      req.username,
      req.password,
      req.nickname,
      req.email);

  return new UserSignupResponse(
      userId,
      req.username,
      req.nickname);
}
```

포인트:

* return 타입을 `UserSignupResponse`로 확정한다
* 실패 처리(ApiException)는 아직 넣지 않는다

---

## 4. Controller – 회원가입 엔드포인트 추가

> Controller는 구현 여부와 관계없이 **항상 가장 얇아야 한다**.

```java
@PostMapping("/api/v1/users")
public ApiResponse<UserSignupResponse> signup(
        @RequestBody UserSignupRequest req) {

    return ApiResponse.ok(
        "회원가입 완료",
        userService.signup(req)
    );
}
```

Controller 책임 정리:

* HTTP 요청 수신
* JSON → DTO 변환 (`@RequestBody`)
* Service 호출
* 공통 응답 포맷으로 감싸서 반환

---

## 5. 이 단계에서 확인할 것

* IDE 에러 없이 컴파일되는가
* Controller → Service → Repository 호출 흐름이 보이는가
* 구현되지 않은 부분이 TODO로 명확히 표시되어 있는가

---

## 다음 단계

다음 문서에서 `signup()` 메서드를 **실제 비즈니스 로직**으로 완성한다.

* 필수값 검증
* 아이디 중복 체크 (`countByUsername`)
* 사용자 저장 (`insertUser`)
* 실패 시 `ApiException` 발생

→ [**03. Service 회원가입 로직 구현**](03-service-signup.md)