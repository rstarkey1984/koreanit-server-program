# 인가 (403) — Role 테이블 기반 접근 제어 + Method Security

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
* 401/403 응답도 ApiResponse(JSON)로 통일
  (authenticationEntryPoint / accessDeniedHandler)

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

### 5-1. Repository 인터페이스 ( 작성 )

```java
package com.koreanit.spring.security;

import java.util.List;

public interface UserRoleRepository {

  List<String> findRolesByUserId(Long userId);

  void addRole(Long userId, String role);
}
```

### 5-2. JdbcTemplate 구현체 ( 작성 )

```java
package com.koreanit.spring.security;

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

## 6. SessionAuthenticationFilter ( 수정 ) — authorities 주입

```java
package com.koreanit.spring.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.koreanit.spring.user.UserEntity;
import com.koreanit.spring.user.UserRepository;
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

  public SessionAuthenticationFilter(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository) {
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

    for (String role : roles) {
      if ("ROLE_USER".equals(role)) {
        hasUserRole = true;
      }
      authorities.add(new SimpleGrantedAuthority(role));
    }

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

    SecurityContext context = SecurityContextHolder.getContext();
    Authentication cur = context.getAuthentication();

    if (needsAuthInjection(cur)) {

      HttpSession session = request.getSession(false);
      if (session != null) {
        Object v = session.getAttribute(SESSION_USER_ID);

        if (v instanceof Long userId) {

          try {
            UserEntity user = userRepository.findById(userId);

            LoginUser principal = new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getNickname());

            Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                resolveAuthorities(userId));

            // Authentication을 SecurityContext에 주입
            context.setAuthentication(auth);
          } catch (EmptyResultDataAccessException e) {
            // 사용자 조회 실패 시 무시(인증 주입 안 함)
            // 세션에 쓰레기 userId가 남은 케이스: 로그인 해제 처리
            session.removeAttribute(SESSION_USER_ID);
            // 또는 session.invalidate();
            // 인증 주입 안 하고 익명으로 통과
          }
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
```

### SecurityConfig ( 수정 )
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
import com.koreanit.spring.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private void writeJson(HttpServletResponse res, int status, ApiResponse<Void> body) {
    res.setStatus(status);
    res.setContentType("application/json; charset=UTF-8");

    try {
      objectMapper.writeValue(res.getWriter(), body);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  public SessionAuthenticationFilter sessionAuthenticationFilter(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository) {
    return new SessionAuthenticationFilter(userRepository, userRoleRepository);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      SessionAuthenticationFilter sessionFilter) throws Exception {

    http
        // 기본 로그인 폼 비활성화
        .formLogin(f -> f.disable())

        // HTTP Basic 인증 비활성화
        .httpBasic(b -> b.disable())

        // CSRF 보호 비활성화 (JSON API 기준)
        .csrf(csrf -> csrf.disable())        

        // 요청 경로별 접근 권한 설정
        .authorizeHttpRequests(auth -> auth
            // CORS preflight 요청 허용
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // 회원 가입 허용
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

            // 로그인 요청 허용
            .requestMatchers(HttpMethod.POST, "/api/login").permitAll()

            // 로그아웃 요청 허용
            .requestMatchers(HttpMethod.POST, "/api/logout").permitAll()

            // API 경로는 인증 필요
            .requestMatchers("/api/**").authenticated()

            // 그 외 요청은 모두 허용
            .anyRequest().permitAll())

        // 인증/인가 실패 시 JSON 응답 처리
        .exceptionHandling(e -> e

            // 미인증 접근 시 401
            .authenticationEntryPoint((req, res, ex) -> {

              log.warn("[{}] message={}",
                  ex.getClass().getName(),
                  ex.getMessage());

              writeJson(
                  res,
                  ErrorCode.UNAUTHORIZED.getStatus().value(),
                  ApiResponse.fail(
                      ErrorCode.UNAUTHORIZED.name(),
                      "로그인이 필요합니다"));
            })

            // 권한 없는 접근 시 403 응답
            .accessDeniedHandler((req, res, ex) -> {
              
              log.warn("[{}] message={}",
                  ex.getClass().getName(),
                  ex.getMessage());

              writeJson(
                  res,
                  ErrorCode.FORBIDDEN.getStatus().value(),
                  ApiResponse.fail(ErrorCode.FORBIDDEN.name(), "권한이 없습니다"));
            }))

        // 세션 기반 인증 필터 등록
        .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();

  }
}
```

---

## 7. Method Security — 본인 또는 관리자 제어

### 현재 로그인 사용자 id 조회 유틸

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

---

## 8. @PreAuthorize 를 사용한 403 인가 정책 적용

### 8-1. `@PreAuthorize` 를 사용하기 위해서 Method Security 활성화

```java
package com.koreanit.spring.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```


### 8-2. UserService 메서드에 `@PreAuthorize` 적용

`@PreAuthorize` 에서 사용될 유틸 메서드 작성

```java
public boolean isSelf(Long userId) {
  Long currentUserId = SecurityUtils.currentUserId();
  return currentUserId != null && userId != null && currentUserId.equals(userId);
}
```

관리자 또는 본인 일 경우
```java
@PreAuthorize("hasRole('ADMIN') or @userService.isSelf(#id)")
public User get(Long id)

@PreAuthorize("hasRole('ADMIN')")
public List<User> list(int limit)

@PreAuthorize("hasRole('ADMIN') or @userService.isSelf(#id)")
public void changeNickname(Long id, String nickname)

@PreAuthorize("hasRole('ADMIN') or @userService.isSelf(#id)")
public void changePassword(Long id, String password)

@PreAuthorize("hasRole('ADMIN') or @userService.isSelf(#id)")
public void delete(Long id)
```

---

## 9. @PreAuthorize 개념과 사용 규칙

이 문서는 **Service 메서드 단위 인가 처리**를 위해 사용하는
`@PreAuthorize`의 개념, 동작 위치, 표현식 작성 방법, 사용 규칙을 정리한 기술 문서다.

---

### 9-1. @PreAuthorize란 무엇인가

`@PreAuthorize`는 **메서드가 실행되기 전에 인가(Authorization) 조건을 검사**하는
Spring Security의 **메서드 보안 애너테이션**이다.

* “이 메서드를 누가 실행할 수 있는가”를 선언적으로 표현한다
* 조건을 만족하지 못하면 **메서드 본문은 실행되지 않는다**

즉,

> 요청을 허용할지 말지를
> **비즈니스 로직에 들어가기 전에** 결정하는 장치다.

---

### 9-2. 선행 조건 (@EnableMethodSecurity)

`@PreAuthorize`를 사용하려면 **메서드 보안 기능을 반드시 활성화**해야 한다.

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```
1. `@Configuration`
   - 스프링 설정 클래스임을 의미
   - 애플리케이션 시작 시 컨텍스트에 로딩됨
2. `@EnableMethodSecurity`
   - 메서드 단위 보안 활성화
   - 이게 켜지면 `@PreAuthorize` 가 동작한다

---

### 9-3. 내부 동작 개념

`@PreAuthorize`는 내부적으로 다음을 수행한다.

1. SpEL(Spring Expression Language) 표현식 평가
2. 결과가 `false`이면
   * `AccessDeniedException` 또는 `AuthorizationDeniedException` 발생
3. 결과가 `true`이면

   * 실제 메서드 실행
4. 발생한 에러는 GlobalExceptionHandler 에서 잡힘

---

## 10. SpEL 표현식 사용법

`@PreAuthorize`의 조건은 **문자열 형태의 SpEL**로 작성한다.

---

### 10-1. 권한(Role) 검사

```java
@PreAuthorize("hasRole('ADMIN')")
```

* `ROLE_ADMIN` 권한을 가진 사용자만 허용
* 내부적으로 `GrantedAuthority`를 검사한다

---

### 10-2. 메서드 파라미터 참조

```java
@PreAuthorize("#id == 10")
```

* `#파라미터명` 형태로 메서드 인자 참조 가능
* 실제 호출 시 전달된 값으로 비교된다

---

### 10-3. Bean 객체 메서드 호출

```java
@PreAuthorize("hasRole('ADMIN') or @userService.isSelf(#id)")
public Post update(long id, String title, String content)
```

* **@빈이름.메서드(인자)** 형태로 스프링 빈의 메서드를 SpEL에서 호출한다.
* 복잡한 인가 로직(소유자 검사, DB 조회 포함)을 표현식 문자열 밖(자바 코드)으로 빼는 대표 패턴이다.
* 표현식에는 “규칙 선언”만 남고, 실제 판단 로직은 테스트 가능한 자바 메서드로 이동한다.
* 클래스가 `@Service` / `@Component` 로 등록되면 기본 빈 이름은 클래스명의 첫 글자 소문자

  * `UserService` → `userService`
  * `PostService` → `postService`

---

### 10-4. 논리 연산

```java
@PreAuthorize("hasRole('ADMIN') or ...")
```

* `and`, `or`, `not` 사용 가능

---

## 한 줄 요약

`@PreAuthorize`는
**Service 메서드 실행 전에 인가 규칙을 선언적으로 고정하기 위한
Spring Security 메서드 보안 장치**다.

---

## 11. AuthorizationDeniedException 예외 처리 ( 추가 )

```java
@ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception e) {

  log.warn("[{}] message=\"{}\" origin={}", e.getClass().getName(),
      e.getMessage(), origin(e));

  return ResponseEntity
      .status(ErrorCode.FORBIDDEN.getStatus())
      .body(ApiResponse.fail(
          ErrorCode.FORBIDDEN.name(),
          "권한이 없습니다"));
}
```

---

## 테스트

파일: `403.http`

```http
@baseUrl = http://localhost:8080
@json = application/json

### 1) 일반 사용자 로그인
POST {{baseUrl}}/api/login
Content-Type: {{json}}

{
  "username": "test",
  "password": "1234"
}

### 2) 관리자 계정으로 로그인
POST {{baseUrl}}/api/login
Content-Type: {{json}}

{
  "username": "admin",
  "password": "1234"
}

### 3) 사용자 목록 조회
GET {{baseUrl}}/api/users?limit=1

### 4) 비밀번호 변경
PUT {{baseUrl}}/api/users/1/password
Content-Type: {{json}}

{
  "password": "1234"
}

### 5) 닉네임 변경
PUT {{baseUrl}}/api/users/11/nickname
Content-Type: {{json}}

{
  "password": "newpassword"
}
```

---

## 체크리스트

* `user_roles` 테이블 생성 완료
* 신규 가입 시 `ROLE_USER` 부여(권장)
* SessionAuthenticationFilter가 `user_roles` 기반으로 authorities 주입
* Method Security 활성화(`@EnableMethodSecurity`)
* Service 변경 메서드는 `@PreAuthorize`로 "관리자 또는 본인" 고정
* 권한 부족 시 GlobalExceptionHandler 에서 403(ApiResponse) 응답 확인

---

## 이 단계의 핵심 정리

* Role은 DB에서 관리한다
* Filter가 authorities를 주입하면, 이후 인가는 선언적으로 처리할 수 있다

  * URL 인가: SecurityConfig
  * 메서드 인가: Service(@PreAuthorize)
* 401(인증 실패)과 403(인가 실패)은 의미가 다르며 응답도 분리한다

---

## 다음 단계

→ [**Users API 전체 구조 요약 (01~06)**](07-users_api_summary.md)