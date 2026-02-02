# 05. 인증 (401) — Session + Spring Security

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

Spring Security는 인증 여부를 `SecurityContext`의 `Authentication` 존재로 판단한다.

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

역할:

* 세션에 `LOGIN_USER_ID`가 있으면 인증 객체를 생성해 SecurityContext에 넣는다
* 세션이 없으면 아무 것도 하지 않고 다음 필터로 넘긴다
* 05 단계에서는 권한을 단순화하여 `ROLE_USER`를 고정한다(06 단계에서 확장)

주의:

* Spring Security는 익명 사용자 처리를 위해 `AnonymousAuthenticationToken`을 넣을 수 있다
* 따라서 `Authentication == null`만 체크하면 세션 로그인 정보가 있어도 덮어쓰지 못하는 경우가 생길 수 있다
* `null` 또는 `AnonymousAuthenticationToken`인 경우에만 주입하도록 한다

구현 방식:

* 세션에서 `LOGIN_USER_ID`를 꺼낸다
* 기존 `UserRepository.findById()`로 사용자를 조회한다
* 조회 결과 `UserEntity`로 `LoginUser`를 만들어 principal에 넣는다

```java
package com.koreanit.spring.security;

import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

  public static final String SESSION_USER_ID = "LOGIN_USER_ID";

  private final UserRepository userRepository;

  public SessionAuthenticationFilter(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  private boolean needsAuthInjection(Authentication a) {
    return (a == null) || (a instanceof AnonymousAuthenticationToken);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    Authentication cur = SecurityContextHolder.getContext().getAuthentication();

    // 이미 인증이 있으면 재설정하지 않음 (단, 익명 인증은 덮어씀)
    if (needsAuthInjection(cur)) {

      HttpSession session = request.getSession(false); // 세션이 없으면 만들지 않음
      if (session != null) {
        Object v = session.getAttribute(SESSION_USER_ID);

        if (v instanceof Long userId) {

          UserEntity user = userRepository.findById(userId);
          LoginUser principal = new LoginUser(
              user.getId(),
              user.getUsername(),
              user.getNickname()
          );

          var auth = new UsernamePasswordAuthenticationToken(
              principal, // principal
              null,
              List.of(new SimpleGrantedAuthority("ROLE_USER"))
          );

          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
```

---

## 6. SecurityConfig 작성

파일: `security/SecurityConfig.java`

목표:

* 기본 로그인 화면/Basic 비활성화
* `/api/**`는 인증 필요
* `POST /api/login`, `POST /api/users`만 permitAll
* 401/403도 ApiResponse(JSON)로 응답
* SessionAuthenticationFilter를 FilterChain에 등록
* 로그아웃(`/api/logout`)도 JSON(ApiResponse)로 응답

주의:

* `SessionAuthenticationFilter`는 `UserRepository` 주입이 필요하다
* 따라서 `new SessionAuthenticationFilter()`로 생성하지 않고 **Bean으로 등록해서 주입**한다

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
        // REST API 기준: HTML 기반 인증 UI 사용하지 않음
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())

        // 세션 기반 인증: 필요할 때만 세션 생성 (학습용 명시)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

        // 실습 단순화: CSRF 비활성
        .csrf(csrf -> csrf.disable())

        // 401/403 응답을 ApiResponse(JSON)로 통일
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

        // 인증 규칙
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

        // 로그아웃도 API답게 JSON으로
        .logout(lo -> lo
            .logoutUrl("/api/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((req, res, auth) -> {
              writeJson(res, 200, ApiResponse.ok(null));
            }))

        // 세션 → SecurityContext 주입 필터
        .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
```

---

## 7. UserService 사용자 및 비밀번호 검증 예외처리

```java
// 정상 흐름: 로그인 자격 검증 → userId 반환 (세션 저장은 Controller 책임)
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

* `/api/**`가 실제로 보호되기 시작한다
* 세션이 없으면 401(ApiResponse)
* 로그인/회원가입만 예외적으로 열린다
* Controller/Service에서 인증 체크를 하지 않는다(요청 진입점에서 차단)
* 로그아웃도 JSON(ApiResponse)로 응답한다
* principal이 `LoginUser`로 고정된다

---

## 다음 단계

인가 (403)
