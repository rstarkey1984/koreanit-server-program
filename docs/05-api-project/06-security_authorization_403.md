# 06. 인가 (403) — Role 테이블 기반 접근 제어 + Method Security

이 단계에서는 **인증(401)이 완료된 이후**,
"누가 어떤 API를 사용할 수 있는가"를 **권한(Role)** 기준으로 제어한다.

본 단계의 핵심은 다음 두 가지다.

1. **URL 단위 인가**: 관리자만 접근 가능한 API를 SecurityConfig에서 Role로 차단
2. **메서드 단위 인가**: "본인 또는 관리자" 규칙을 Method Security로 제어

---

## 1. 단계 목표

이 단계가 끝나면 서버는 다음 상태를 보장한다.

* `/api/**` 는 인증된 사용자만 접근 가능
* `GET /api/users` (목록), `GET /api/users/{id}` (단건 조회)

  * **관리자(ROLE_ADMIN)만 허용**
* 닉네임 변경 / 비밀번호 변경 API는

  * **본인 또는 관리자만 허용**
* 권한 부족 시 **403 FORBIDDEN + ApiResponse(JSON)** 로 응답

---

## 2. 전제 조건

이 문서는 다음 단계들이 이미 완료되어 있음을 전제로 한다.

* 인증(401) — Session + Spring Security
* `SessionAuthenticationFilter`를 통한 SecurityContext 주입
* `LoginUser` principal 고정
* 공통 응답 포맷(ApiResponse)
* 401/403 응답도 ApiResponse(JSON)로 통일 (authenticationEntryPoint / accessDeniedHandler)

---

## 3. 권한 모델 (Role) — DB 기반

`users` 테이블은 그대로 유지하고, 권한은 별도 테이블로 분리한다.

### 3-1. 권한 테이블 설계

권장 구조: `user_roles(user_id, role)`

* 사용자 1명은 0개 이상의 Role을 가질 수 있다
* 본 강의 단계에서는 Role 문자열을 그대로 저장한다

  * `ROLE_USER`
  * `ROLE_ADMIN`

파일: (DDL) `docs/sql/06-user_roles.sql` 또는 DB 콘솔

```sql
CREATE TABLE `user_roles` (
  `user_id` bigint unsigned NOT NULL COMMENT 'users.id',
  `role` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ROLE_USER | ROLE_ADMIN',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`role`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
)
```
```sql
-- (권장) 모든 사용자에게 기본 ROLE_USER 부여
-- 신규 가입 시점에 애플리케이션에서 INSERT 하거나,
-- 운영 시에는 배치로 정리한다.
-- 예: 특정 사용자에게 관리자 권한 부여
INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_ADMIN');

-- 예: 기본 권한 부여
INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_USER');
```

### 3-2. 관리자 판별 규칙

* **`user_roles`에 `ROLE_ADMIN`이 존재하면 관리자**로 본다

---

## 4. 구현 개요

인가 처리는 다음 흐름으로 동작한다.

1. SessionAuthenticationFilter가 세션의 `LOGIN_USER_ID`로 사용자를 조회한다
2. `user_roles`에서 Role 목록을 조회하여 authorities로 주입한다
3. SecurityConfig가 URL 규칙으로 1차 인가를 수행한다
4. Service는 Method Security(@PreAuthorize)로 2차(정밀) 인가를 수행한다

---

## 5. Repository — user_roles 조회

권한 주입을 위해 Role 조회 전용 Repository를 추가한다.

### 5-1. Repository 인터페이스

파일: `repository/UserRoleRepository.java`

```java
package com.koreanit.spring.repository;

import java.util.List;

public interface UserRoleRepository {

  // 예: ["ROLE_USER", "ROLE_ADMIN"]
  List<String> findRolesByUserId(Long userId);

  // 권장: 가입 시 기본 권한 부여
  void addRole(Long userId, String role);
}
```

### 5-2. JdbcTemplate 구현체

파일: `repository/impl/JdbcUserRoleRepository.java`

```java
package com.koreanit.spring.repository.impl;

import com.koreanit.spring.repository.UserRoleRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRoleRepository implements UserRoleRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcUserRoleRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<String> findRolesByUserId(Long userId) {
    String sql = """
        SELECT role
        FROM user_roles
        WHERE user_id = ?
        ORDER BY role
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("role"), userId);
  }

  @Override
  public void addRole(Long userId, String role) {
    String sql = """
        INSERT INTO user_roles (user_id, role)
        VALUES (?, ?)
        """;

    jdbcTemplate.update(sql, userId, role);
  }
}
```

---

## 6. 회원가입 시 기본 권한 부여 (권장)

06 단계에서는 최소한의 정책으로, 신규 가입 시 `ROLE_USER`를 자동 부여하는 것을 권장한다.

파일: `service/UserService.java` (회원 생성 흐름)

```java
// 정상 흐름: 회원가입 → PK 반환
public Long create(UserCreateRequest req) {
  String username = req.getUsername();
  String nickname = req.getNickname();
  String email = req.getEmail();
  String hash = passwordEncoder.encode(req.getPassword());

  try {
    Long userId = userRepository.save(username, hash, nickname, email);
    // 기본 권한 부여
    if (userId != null) {
      userRoleRepository.addRole(userId, "ROLE_USER");
    }
    return userId;
  } catch (DuplicateKeyException e) {
    throw new ApiException(
        ErrorCode.DUPLICATE_RESOURCE,
        toDuplicateMessage(e));
  }
}
```

* 기존 사용자 데이터가 이미 많다면, 운영에서는 배치로 ROLE_USER를 정리하는 것이 일반적이다

---

## 7. SessionAuthenticationFilter — authorities 주입

Role 테이블 기반 인가의 핵심은 Filter에서 authorities를 구성하는 것이다.

파일: `security/SessionAuthenticationFilter.java`

```java
package com.koreanit.spring.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.repository.UserRepository;
import com.koreanit.spring.repository.UserRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

  public static final String SESSION_USER_ID = "LOGIN_USER_ID";

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;

  public SessionAuthenticationFilter(UserRepository userRepository, UserRoleRepository userRoleRepository) {
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
  }

  private boolean needsAuthInjection(Authentication a) {
    return (a == null) || (a instanceof AnonymousAuthenticationToken);
  }

  private List<SimpleGrantedAuthority> resolveAuthorities(Long userId) {

    List<String> roles = userRoleRepository.findRolesByUserId(userId);

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    boolean hasUserRole = false;

    // DB에서 조회한 Role 변환
    for (String role : roles) {
      if ("ROLE_USER".equals(role)) {
        hasUserRole = true;
      }
      authorities.add(new SimpleGrantedAuthority(role));
    }

    // 방어적 기본값: ROLE_USER 보장
    if (!hasUserRole) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return authorities;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // 1. 현재 요청의 SecurityContext 획득
    var context = SecurityContextHolder.getContext();

    // 2. 현재 Authentication 조회
    Authentication cur = context.getAuthentication();

    // 이미 인증이 있으면 재설정하지 않음 (단, 익명 인증은 덮어씀)
    if (needsAuthInjection(cur)) {

      HttpSession session = request.getSession(false);
      if (session != null) {
        Object v = session.getAttribute(SESSION_USER_ID);

        if (v instanceof Long userId) {

          UserEntity user = userRepository.findById(userId);

          LoginUser principal = new LoginUser(
              user.getId(),
              user.getUsername(),
              user.getNickname());

          Authentication auth = new UsernamePasswordAuthenticationToken(
              principal,
              null,
              resolveAuthorities(userId));

          // 3. Authentication을 SecurityContext에 주입
          context.setAuthentication(auth);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
```

---

## 8. SecurityConfig — URL 단위 Role 인가

요구사항:

* `GET /api/users` 목록: 관리자만
* `GET /api/users/{id}` 단건: 관리자만

파일: `security/SecurityConfig.java`

```java
package com.koreanit.spring.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanit.spring.common.error.ErrorCode;
import com.koreanit.spring.common.response.ApiResponse;
import com.koreanit.spring.repository.UserRepository;
import com.koreanit.spring.repository.UserRoleRepository; // (추가) 권한/롤 조회가 필요하면 유지

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * (공통) API 응답을 JSON으로 강제하는 유틸.
   * - Spring Security 예외/로그아웃 성공 응답에서 동일 포맷(ApiResponse) 유지 목적
   */
  private void writeJson(HttpServletResponse res, int status, ApiResponse<Void> body) {
    res.setStatus(status);
    res.setContentType("application/json; charset=UTF-8");

    try {
      objectMapper.writeValue(res.getWriter(), body);
    } catch (IOException e) {
      // 보통 여기서도 ApiResponse로 내려주긴 어려워서 런타임으로 올려버림(학습용 단순화)
      throw new RuntimeException(e);
    }
  }

  /**
   * (추가/복구) 기존 코드에서 SessionAuthenticationFilter가 UserRoleRepository까지 받았음.
   * - 네가 만든 SessionAuthenticationFilter 생성자가 (UserRepository, UserRoleRepository)
   * 형태면 이 Bean 시그니처가 필요
   * - 생성자가 (UserRepository) 1개만 받는 형태라면 아래 userRoleRepository 파라미터와 import를 제거하면
   * 됨
   */
  @Bean
  public SessionAuthenticationFilter sessionAuthenticationFilter(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository) {
    return new SessionAuthenticationFilter(userRepository, userRoleRepository);
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      SessionAuthenticationFilter sessionFilter) throws Exception {

    http
        /**
         * REST API 기준: 기본 로그인 폼/Basic 인증을 끔
         * - HTML 로그인 페이지(폼 로그인) 방지
         * - 브라우저 Basic Auth 팝업 방지
         */
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())

        /**
         * 실습 단순화를 위해 CSRF 비활성
         * - 운영에서는 보통 활성 + 토큰 전달 방식(CookieCsrfTokenRepository 등) 사용
         */
        .csrf(csrf -> csrf.disable())

        /**
         * 인증/인가 규칙
         * - 위에서 아래 순서대로 매칭됨
         */
        .authorizeHttpRequests(auth -> auth
            // (선택) 프론트 연동 시 프리플라이트 허용
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // 회원가입/로그인만 오픈
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/login").permitAll()

            // /api는 기본적으로 보호
            .requestMatchers("/api/**").authenticated()

            // 그 외(예: /hello)는 오픈
            .anyRequest().permitAll())

        /**
         * 401/403 응답을 ApiResponse(JSON)로 통일
         * - 인증 안 됨(401): authenticationEntryPoint
         * - 인가 실패(403): accessDeniedHandler
         */
        .exceptionHandling(e -> e
            .authenticationEntryPoint((req, res, ex) -> {
              writeJson(
                  res,
                  ErrorCode.UNAUTHORIZED.getStatus().value(),
                  ApiResponse.fail(ErrorCode.UNAUTHORIZED.name(), "로그인이 필요합니다"));
            })
            .accessDeniedHandler((req, res, ex) -> {
              writeJson(
                  res,
                  ErrorCode.FORBIDDEN.getStatus().value(),
                  ApiResponse.fail(ErrorCode.FORBIDDEN.name(), "권한이 없습니다"));
            }))

        /**
         * sessionFilter : 세션 → SecurityContext 주입 필터
         * - UsernamePasswordAuthenticationFilter(폼 로그인용)보다 "앞"에서 실행되게 배치
         * - 폼 로그인을 껐더라도, 필터 체인 내 기준 위치(anchor)로 자주 사용됨
         */
        .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

    /**
     * SecurityFilterChain 생성
     * - 위에서 구성한 보안 설정을 실제 필터 체인으로 빌드
     */
    return http.build();
  }
}
```

설명:

* `hasRole("ADMIN")`은 내부적으로 `ROLE_ADMIN` authority 존재 여부를 검사한다
* 목록/단건 조회만 URL 레벨에서 관리자 전용으로 고정한다
* 변경 API는 URL에서 세분화하지 않고, Method Security에서 "본인 또는 관리자"로 제어한다

---

## 9. Method Security — 본인 또는 관리자 제어

URL 패턴만으로는 "본인(id 일치)" 조건을 깔끔하게 표현하기 어렵다.
따라서 변경(수정) 계열은 Service 메서드에서 인가한다.

### 9-1. Method Security 활성화

파일: `security/MethodSecurityConfig.java`

```java
package com.koreanit.spring.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```

이 설정 파일은 **메서드 보안 애너테이션을 실제로 동작하게 만들기 위한 필수 설정 파일**이다.

Spring Security에서는
`@PreAuthorize`, `@PostAuthorize`, `@Secured` 와 같은 **메서드 보안 애너테이션을 사용하려면**,
반드시 `@EnableMethodSecurity` 를 통해 해당 기능을 **명시적으로 활성화**해야 한다.

이 설정이 있으면:

* 메서드 호출 직전에 Spring Security가 개입하여
* 현재 `SecurityContext`의 인증/권한 정보를 기준으로
* 접근 가능 여부를 검사한다
* 실패 시 `AccessDeniedException` 이 발생하며 **403 FORBIDDEN** 응답으로 처리된다

즉, 이 파일은 **“메서드 레벨 보안을 사용하겠다는 선언용 설정 파일”** 이며,
실제 권한 규칙은 이후 Service 메서드에 붙는 애너테이션으로 정의한다.


### 9-2. 현재 로그인 사용자 id 조회 유틸

principal은 05 단계에서 `LoginUser`로 고정되어 있다.

파일: `security/SecurityUtils.java`

```java
package com.koreanit.spring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

  private SecurityUtils() {}

  public static Long currentUserId() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) return null;

    Object p = a.getPrincipal();
    if (p instanceof LoginUser u) {
      return u.getId();
    }
    return null;
  }
}
```

### 9-3. Service 메서드에 @PreAuthorize 적용

적용 규칙:

* 관리자: 누구든 변경 가능
* 일반 사용자: 본인만 변경 가능

파일: `service/UserService.java`

```java

// 정상 흐름: 단건 조회 → Domain 반환
@PreAuthorize("hasRole('ADMIN') or #id == T(com.koreanit.spring.security.SecurityUtils).currentUserId()")
public User get(Long id) 

// 정상 흐름: 목록 조회 → Domain 리스트 반환
@PreAuthorize("hasRole('ADMIN')")
public List<User> list(int limit) 

// 정상 흐름: 닉네임 변경
@PreAuthorize("hasRole('ADMIN') or #id == T(com.koreanit.spring.security.SecurityUtils).currentUserId()")
public void changeNickname(Long id, UserNicknameChangeRequest req) 

// 정상 흐름: 비밀번호 변경 (해시 저장)
@PreAuthorize("hasRole('ADMIN') or #id == T(com.koreanit.spring.security.SecurityUtils).currentUserId()")
public void changePassword(Long id, UserPasswordChangeRequest req) 
```

---

## 10. 테스트

파일: `403.http`

```http
@baseUrl = http://localhost:8080
@json = application/json

### 1) 로그인 없이 관리자 API 호출 -> 401
GET {{baseUrl}}/api/users?limit=1

### 2) 일반 사용자 로그인
POST {{baseUrl}}/api/login
Content-Type: {{json}}

{
  "username": "user1",
  "password": "1234"
}

### 3) 일반 사용자로 목록 조회 -> 403
GET {{baseUrl}}/api/users?limit=1

### 4) 관리자 권한 부여 (DB에서 수행)
# INSERT INTO user_roles(user_id, role) VALUES (<admin_id>, 'ROLE_ADMIN');

### 5) 관리자 계정으로 로그인
POST {{baseUrl}}/api/login
Content-Type: {{json}}

{
  "username": "admin",
  "password": "1234"
}

### 6) 관리자 목록 조회 -> 200
GET {{baseUrl}}/api/users?limit=5

### 7) 본인 비밀번호 변경 -> 200
PUT {{baseUrl}}/api/users/10060/password
Content-Type: {{json}}

{
  "password": "newpassword"
}

### 8) 다른 사용자 비밀번호 변경 (일반 사용자면 403, 관리자면 200)
PUT {{baseUrl}}/api/users/99999/password
Content-Type: {{json}}

{
  "password": "newpassword"
}
```

---

## 11. 체크리스트

* `user_roles` 테이블 생성 완료
* 신규 가입 시 `ROLE_USER` 부여(권장)
* SessionAuthenticationFilter가 `user_roles` 기반으로 authorities 주입
* SecurityConfig에서 GET `/api/users`, GET `/api/users/*`는 `hasRole('ADMIN')`
* Method Security 활성화(`@EnableMethodSecurity`)
* Service 변경 메서드는 `@PreAuthorize`로 "본인 또는 관리자" 고정
* 권한 부족 시 403(ApiResponse) 응답 확인

---

## 12. 이 단계의 핵심 정리

* Role은 DB에서 관리한다
* Filter가 authorities를 주입하면, 이후 인가는 선언적으로 처리할 수 있다

  * URL 인가: SecurityConfig
  * 메서드 인가: Service(@PreAuthorize)
* 401(인증 실패)과 403(인가 실패)은 의미가 다르며 응답도 분리한다

---

## 다음 단계

* Role DB 모델 정교화(roles 마스터 테이블, 권한 계층)
* 관리자 기능 확장(관리자 전용 리소스)
* 리소스 단위 세분화된 인가 정책(예: 게시글/댓글 권한)
