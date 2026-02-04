# 도메인 기준 패키지 리팩토링

## 목적

본 문서는 기존 **레이어 중심 패키지 구조**(controller / service / repository / dto / entity)에서 발생하는
구조적 복잡도를 해소하기 위해, **도메인(기능) 기준 패키지 구조**로 리팩토링하는 기준을 정리한다.

리팩토링의 목적은 다음과 같다.

1. 기능 단위(user, post 등)로 코드 탐색 비용을 줄인다
2. 기존 책임 분리 규칙(Entity / Domain / DTO)을 패키지 구조로 변경한다
3. 이후 도메인 확장 시 동일한 구조를 복제 가능한 템플릿으로 만든다

본 리팩토링은 **동작 변경이 아닌 구조 변경**을 목표로 한다.

---

## 1. 도메인 중심 패키지 구조

```text
com.koreanit.spring
├─ Application.java
├─ common
├─ security
│  ├─ AuthController.java
│  ├─ CsrfController.java
│  ├─ SecurityConfig.java
│  ├─ MethodSecurityConfig.java
│  ├─ SessionAuthenticationFilter.java
│  ├─ LoginUser.java
│  ├─ SecurityUtils.java
│  ├─ UserRoleRepository.java
│  └─ JdbcUserRoleRepository.java
│
└─ user
   ├─ UserController.java
   ├─ UserService.java
   ├─ UserRepository.java
   ├─ JdbcUserRepository.java
   ├─ UserEntity.java
   ├─ User.java
   ├─ UserMapper.java
   └─ dto
      ├─ request
      │  ├─ UserCreateRequest.java
      │  ├─ UserLoginRequest.java
      │  ├─ UserEmailChangeRequest.java
      │  └─ UserPasswordChangeRequest.java
      └─ response
         └─ UserResponse.java
```

---

## 2. User 도메인 중심 폴더 생성

### 2-1. 패키지 폴더 생성
```
mkdir -p src/main/java/com/koreanit/spring/user/dto/request src/main/java/com/koreanit/spring/user/dto/response
```

### 2-2. VSCode 로 파일 이동 

---

## 3. 빌드 및 동작 확인

* 컴파일 성공 여부 확인
* 최소 기능 점검

  * 회원가입
  * 로그인
  * 인증이 필요한 API 호출

ApiResponse 형식과 예외 처리 흐름이 이전과 동일한지 확인한다.

---

## 4. Post 도메인 폴더 생성 및 파일 생성

### 4-1. 패키지 폴더 생성
```
mkdir -p src/main/java/com/koreanit/spring/post/dto/request src/main/java/com/koreanit/spring/post/dto/response
```

### 4-2. 파일 생성
```
touch src/main/java/com/koreanit/spring/post/PostController.java src/main/java/com/koreanit/spring/post/PostService.java src/main/java/com/koreanit/spring/post/PostRepository.java src/main/java/com/koreanit/spring/post/JdbcPostRepository.java src/main/java/com/koreanit/spring/post/PostEntity.java src/main/java/com/koreanit/spring/post/Post.java src/main/java/com/koreanit/spring/post/PostMapper.java src/main/java/com/koreanit/spring/post/dto/request/PostCreateRequest.java src/main/java/com/koreanit/spring/post/dto/request/PostUpdateRequest.java src/main/java/com/koreanit/spring/post/dto/response/PostResponse.java
```

### 4-3. VSCode 로 파일 이동 

---

## 완료 기준

* 기존 Users API 기능이 동일하게 동작한다
* 공통 응답(ApiResponse) 규칙이 유지된다
* 예외 처리(GlobalExceptionHandler)가 정상 동작한다
* 보안 흐름(Session → SecurityContext)이 유지된다
* Post 도메인을 동일한 구조로 추가할 수 있다

---

## 다음 단계

[**Posts API 정상 흐름 CRUD**](09-posts_flow_crud.md)