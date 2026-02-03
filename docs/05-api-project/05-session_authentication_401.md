# 인증 (401) — Session + Spring Security

## 1. 단계 목표

이 단계의 목표는 하나다.

> `/api/**` 요청을 “세션 로그인 여부”로 차단하고, 로그인하지 않은 요청은 401로 응답한다.

### 1-1. 보장해야 하는 상태

* `/api/**`는 기본적으로 인증 필요
* 인증 실패(세션 없음) → **401 UNAUTHORIZED**
* `POST /api/login`, `POST /api/users`만 예외적으로 허용
* Spring Security 기본 로그인 화면 / Basic 인증 / HTML 응답을 사용하지 않는다
* 401/403 응답도 **ApiResponse(JSON)** 로 통일한다

---

## 2. 인증 기준(세션 키)

세션 로그인 성공 시, 아래 키로 사용자 식별자를 세션에 저장한다.

* 키: `LOGIN_USER_ID`
* 값: `Long userId`

이 키가 세션에 존재하면 “인증됨”으로 본다.

---

## 3. 구조 핵심: Session → SecurityContext

Spring Security는 인증 여부를 **세션**이 아니라
`SecurityContext` 안의 `Authentication` 존재 여부로 판단한다.

따라서 이 단계에서 해야 할 일은 다음 1가지다.

1. 요청이 들어오면
2. 세션에 `LOGIN_USER_ID`가 있는지 확인하고
3. 있으면 `Authentication`을 만들어 `SecurityContext`에 주입한다
4. 이후 `authorizeHttpRequests`의 `.authenticated()`가 통과한다

요청 처리 흐름은 다음과 같다.

```
Client
 → Servlet Filter
 → Spring Security FilterChain
 → DispatcherServlet
 → Controller
```

---

## 3-1. 스프링이 실제로 보는 “인증됨”의 기준

`authenticated()`는 세션 존재 여부를 직접 확인하지 않는다.

오직 다음 조건만 본다.

* `SecurityContextHolder.getContext().getAuthentication()`

  * `null`이 아니고
  * 인증된(Authentication.isAuthenticated) 상태

따라서 우리가 하는 핵심 작업은 한 줄로 요약된다.

> **세션 → Authentication → SecurityContext 주입**

---

## 4. principal 객체: LoginUser

이 단계부터 `Authentication`의 principal을 `Long userId`가 아니라
**LoginUser 객체로 고정**한다.

파일: `security/LoginUser.java`

```java
package com.koreanit.spring.security;

public class LoginUser {

  private final Long id;
  private final String username;
  private final String nickname;

  public LoginUser(Long id, String username, String nickname) {
    this.id = id;
    this.username = username;
    this.nickname = nickname;
  }

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getNickname() { return nickname; }
}
```

---

## 5. SessionAuthenticationFilter 추가

파일: `security/SessionAuthenticationFilter.java`

### 역할

* 세션에 `LOGIN_USER_ID`가 있으면 인증 객체를 생성해 SecurityContext에 넣는다
* 세션이 없으면 아무 것도 하지 않고 다음 필터로 넘긴다
* 05 단계에서는 권한을 단순화하여 `ROLE_USER`를 고정한다

### 주의

* Spring Security는 기본적으로 익명 사용자에 대해 `AnonymousAuthenticationToken`을 넣을 수 있다
* 따라서 `Authentication == null`만 체크하면 기존 익명 인증이 덮어써지지 않을 수 있다
* `null` 또는 `AnonymousAuthenticationToken`인 경우에만 주입하도록 한다

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

## 6. SecurityConfig 작성

### 핵심 목표

* 기본 로그인 화면 / Basic 인증 비활성화
* `/api/**`는 인증 필요
* `POST /api/login`, `POST /api/users`만 permitAll
* 401/403도 ApiResponse(JSON)로 통일
* SessionAuthenticationFilter를 Security FilterChain에 등록
* 로그아웃(`/api/logout`)도 JSON 응답

```java
package com.koreanit.spring.security;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanit.spring.common.error.ErrorCode;
import com.koreanit.spring.common.response.ApiResponse;
import com.koreanit.spring.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

  private final ObjectMapper objectMapper;

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
  public SessionAuthenticationFilter sessionAuthenticationFilter(UserRepository userRepository) {
    return new SessionAuthenticationFilter(userRepository);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
                                        SessionAuthenticationFilter sessionFilter) throws Exception {

    http
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())

        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

        .csrf(csrf -> csrf.disable())

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

        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())

        .logout(lo -> lo
            .logoutUrl("/api/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((req, res, auth) -> {
              writeJson(res, 200, ApiResponse.ok(null));
            }))

        .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
```

---

## 6-1. build()와 구현체 구조 이해

* `SecurityFilterChain`은 인터페이스(규약)
* `DefaultSecurityFilterChain`은 기본 구현체

`http.build()`는 지금까지 설정한 보안 DSL을 해석해
**실행 가능한 필터 체인 객체(DefaultSecurityFilterChain)** 를 생성한다.

이 메서드는 서버 시작 시 딱 한 번 실행되며,
요청마다 실행되지 않는다.

---

## 6-2. addFilterBefore의 의미

```java
.addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
```

의미:

* `UsernamePasswordAuthenticationFilter`를 기준점으로
* 그 앞에 `SessionAuthenticationFilter`를 끼워 넣는다

폼 로그인을 비활성화했더라도
“필터 위치 기준점”으로 사용하는 것은 유효하다.

---

## 6-3. 요청 시 실제로 실행되는 것

* `@Bean SecurityFilterChain` → 서버 시작 시 1회 실행
* `SecurityFilterChain` 내부의 Filter들 → 요청마다 실행

우리가 만든 `SessionAuthenticationFilter`는 요청마다 실행되어
세션을 근거로 `SecurityContext`를 채운다.

---

## 7. UserService 로그인 검증

```java
public Long login(String username, String password) {
  try {
    UserEntity en = userRepository.findByUsername(username);
    boolean ok = passwordEncoder.matches(password, en.getPassword());
    if (!ok) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "비밀번호 검증 실패");
    }
    return en.getId();
  } catch (EmptyResultDataAccessException e) {
    throw new ApiException(ErrorCode.NOT_FOUND_RESOURCE, "존재하지 않는 사용자입니다. username=" + username);
  }
}
```

---

## 8. 테스트

`401.http`

```
@baseUrl = http://localhost:8080

###
# 서버 동작 확인용 헬스체크
GET {{baseUrl}}/hello/users?limit=1

###
# 로그인 없이 보호 API 호출 → 401(ApiResponse)
GET {{baseUrl}}/api/me

###
# 회원가입은 인증 없이 허용 → 200 또는 409(중복)
POST {{baseUrl}}/api/users
Content-Type: application/json

{
  "username": "testuser01",
  "password": "1234",
  "nickname": "테스트",
  "email": "testuser01@example.com"
}

###
# 로그인 → 200 + Set-Cookie(JSESSIONID)
POST {{baseUrl}}/api/login
Content-Type: application/json

{
  "username": "testuser01",
  "password": "1234"
}

###
# 로그인 후 보호 API 호출 → 200
GET {{baseUrl}}/api/me

###
# /api/** 보호 규칙 확인(로그인 전 401, 후 200/403/404)
GET {{baseUrl}}/api/users?limit=1

###
# 로그아웃 (세션 무효화 + 쿠키 삭제)
POST {{baseUrl}}/api/logout
```

---

## 체크리스트

* `/api/**` 보호 시작
* 세션 기반 인증 정상 동작
* Controller/Service에서 인증 체크 제거
* principal = LoginUser 고정

---

## 다음 단계

→ [**인가 (403) — Role 테이블 기반 접근 제어 + Method Security**](06-security_authorization_403.md)
