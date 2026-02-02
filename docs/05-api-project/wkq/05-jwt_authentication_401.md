# 05. JWT Authentication (401) – Spring Security 적용

이 문서는 Users CRUD API에 **Spring Security 기반 JWT 인증(Authentication)** 을 적용하고,
REST Client로 **테스트까지 완료**하는 실습 문서다.

이 단계의 목표는 단 하나다.

> **유효한 JWT를 가진 사용자만 보호된 API를 호출할 수 있게 만든다.**

---

## 1. 이 단계 이전까지 서버가 처리하던 것

이전 단계까지 서버는 다음을 처리할 수 있었다.

* **400**: 요청 형식 오류 (`@Valid` + GlobalExceptionHandler)
* **404**: 리소스 없음 (`NOT_FOUND_RESOURCE`)
* **409**: 중복 제약 위반 (`DUPLICATE_RESOURCE`)

이제 서버는 다음 질문에 답해야 한다.

> 이 요청은 **유효한 JWT를 가진 사용자 요청인가?**

---

## 2. 이 단계의 핵심 메시지

> **인증은 Controller 이전에서 끝나야 한다.**

* 토큰 없음 / 무효 → **401 UNAUTHORIZED**
* 로그인 여부 판단은 **Security FilterChain에서 처리**
* Controller / Service는 인증을 전혀 신경 쓰지 않는다
* 모든 실패 응답은 **ApiResponse 포맷 통일**

---

## 3. 인증(Authentication)과 권한(Authorization)

이 단계에서는 **인증(Authentication)** 만 다룬다.

* 인증(Authentication): 유효한 JWT가 있는가?
* 권한(Authorization): 무엇을 할 수 있는가? → 다음 단계(403)

---

## 4. JWT 인증 서버 원칙

* 서버는 **상태를 저장하지 않는다 (STATELESS)**
* 로그인 성공 시 서버는 **JWT(Access Token)** 만 발급
* 클라이언트는 요청마다 토큰을 헤더로 전송

```text
Authorization: Bearer <token>
```

* Controller / Service는 토큰을 직접 검증하지 않는다

---

## 5. 구성 요소 역할

### CustomUserDetailsService

* DB 사용자를 조회해 `UserDetails`로 변환
* **비밀번호 비교는 수행하지 않음**
* 비밀번호 검증은 Security 내부(AuthenticationProvider) 책임
* UserRepository를 통한 **DB 로딩 전담**

---

### JsonLoginFilter

* `/api/login` 전용 필터
* JSON body에서 username/password 파싱
* 인증 성공 시 JWT 발급
* 실패 시 401 + ApiResponse

---

### JwtProvider

* JWT 생성 / 검증 / 클레임 파싱
* subject(username) 추출
* Spring Security 의존 없음

---

### JwtAuthenticationFilter

* 모든 요청 공통 필터
* Authorization 헤더 Bearer 토큰 파싱
* 검증 성공 시 SecurityContext에 Authentication 세팅
* 실패 시 401 + ApiResponse

---

### SecurityConfig

* 인증 규칙 정의
* `STATELESS` 세션 정책
* 로그인 필터 + JWT 필터 배치
* 401 / 403 응답 포맷 통일

---

## 6. 전체 처리 흐름

### 1) 로그인

```text
Client
 → POST /api/login
 → JsonLoginFilter
 → AuthenticationManager
 → CustomUserDetailsService
 → 인증 성공
 → JWT 발급
 → ApiResponse로 accessToken 반환
```

### 2) 보호 API 호출

```text
Client
 → Authorization: Bearer <token>
 → JwtAuthenticationFilter
 → 토큰 검증
 → SecurityContext에 Authentication 주입
 → Controller 진입
```

### 3) 토큰 없음 / 무효

```text
Client
 → JwtAuthenticationFilter
 → 401 응답
 → Controller 진입 ❌
```

---


## 7. 실습 전제

* Users CRUD API 정상 동작
* ApiResponse / ErrorCode / GlobalExceptionHandler 존재
* 비밀번호는 BCrypt 해시로 저장

---

## 8. 의존성 추가

`build.gradle`

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'

    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
}
```

---

## 9. UserRepository – username 조회

```java
UserEntity findByUsername(String username);
```

```java
@Override
public UserEntity findByUsername(String username) {
    String sql = "SELECT id, username, email, password, nickname, created_at, updated_at FROM users WHERE username = ?";
    return jdbcTemplate.queryForObject(sql, userRowMapper, username);
}
```

---

## 10. CustomUserDetailsService 구현
> 로그인 시 전달된 username으로 DB에서 사용자를 조회해,
Spring Security가 이해할 수 있는 UserDetails로 변환해 주는 역할
```java
package com.koreanit.spring.security;

import com.koreanit.spring.entity.UserEntity;
import com.koreanit.spring.repository.UserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            UserEntity e = userRepository.findByUsername(username);

            return org.springframework.security.core.userdetails.User
                .withUsername(e.getUsername())
                .password(e.getPassword())
                .roles("USER")
                .build();

        } catch (EmptyResultDataAccessException ex) {
            throw new UsernameNotFoundException("not found");
        }
    }
}
```

---

## 11. JwtProvider 구현
> 로그인 성공 시 JWT를 생성하고, 이후 요청에서 전달된 JWT의 유효성을 검증하고 사용자 식별 정보를 꺼내는 역할
```java
package com.koreanit.spring.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final String secret = "CHANGE_ME_TO_LONG_SECRET_KEY_32+";
    private final long expiresMs = 1000L * 60 * 60;

    private final Key key =
        Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

    public String createToken(Authentication auth) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiresMs);

        return Jwts.builder()
            .setSubject(auth.getName())
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public void validate(String token) {
        parseClaims(token);
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
```

---

## 12. JwtAuthenticationFilter 구현
> 모든 요청에서 Authorization 헤더의 JWT를 검사해, 유효하면 해당 사용자를 인증된 상태로 SecurityContext에 등록하는 필터

```java
package com.koreanit.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanit.spring.common.error.ErrorCode;
import com.koreanit.spring.common.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        JwtProvider jwtProvider,
        UserDetailsService userDetailsService,
        ObjectMapper objectMapper
    ) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain
    ) throws ServletException, IOException {

        String auth = req.getHeader("Authorization");

        // 토큰이 없으면 다음으로 넘김(보호 API에서 401 처리됨)
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = auth.substring("Bearer ".length());

        try {
            jwtProvider.validate(token);
            String username = jwtProvider.getUsername(token);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, res);

        } catch (Exception e) {
            res.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8");
            objectMapper.writeValue(
                res.getWriter(),
                ApiResponse.fail(ErrorCode.UNAUTHORIZED.name(), "유효하지 않은 토큰입니다")
            );
        }
    }
}
```

---

## 13. JsonLoginFilter 구현
> /api/login 요청의 JSON 아이디·비밀번호를 읽어 Spring Security 인증을 수행하고, 성공 시 JWT를 발급하는 로그인 전용 필터

```java
package com.koreanit.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanit.spring.common.error.ErrorCode;
import com.koreanit.spring.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.io.IOException;
import java.util.Map;

public class JsonLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    public JsonLoginFilter(
            AuthenticationManager authenticationManager,
            ObjectMapper objectMapper,
            JwtProvider jwtProvider
    ) {
        this.objectMapper = objectMapper;

        setAuthenticationManager(authenticationManager);

        // JSON 로그인 엔드포인트
        setFilterProcessesUrl("/api/login");

        // 성공: JWT 발급
        setAuthenticationSuccessHandler((req, res, auth) -> {
            String token = jwtProvider.createToken(auth);

            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/json; charset=UTF-8");

            objectMapper.writeValue(
                res.getWriter(),
                ApiResponse.ok(Map.of("accessToken", token))
            );
        });

        // 실패: 401
        setAuthenticationFailureHandler((req, res, ex) -> {
            res.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
            res.setContentType("application/json; charset=UTF-8");
            objectMapper.writeValue(
                res.getWriter(),
                ApiResponse.fail(
                    ErrorCode.UNAUTHORIZED.name(),
                    "아이디 또는 비밀번호가 올바르지 않습니다"
                )
            );
        });
    }

    @Override
    public org.springframework.security.core.Authentication attemptAuthentication(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws AuthenticationException {

        try {
            LoginBody body = objectMapper.readValue(req.getInputStream(), LoginBody.class);

            String username = body.username() == null ? "" : body.username();
            String password = body.password() == null ? "" : body.password();

            UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, password);

            return this.getAuthenticationManager().authenticate(authRequest);

        } catch (IOException e) {
            throw new AuthenticationException("invalid login body", e) {};
        }
    }

    public record LoginBody(String username, String password) {}
}
```

---

## 14. SecurityConfig 설정
> Spring Security의 전체 보안 규칙과 인증 필터 흐름(JWT 인증 + JSON 로그인)을 조립하는 보안 설정 클래스

```java
package com.koreanit.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanit.spring.common.error.ErrorCode;
import com.koreanit.spring.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final ObjectMapper objectMapper;
  private final JwtProvider jwtProvider;
  private final CustomUserDetailsService userDetailsService;

  public SecurityConfig(
      ObjectMapper objectMapper,
      JwtProvider jwtProvider,
      CustomUserDetailsService userDetailsService
  ) {
    this.objectMapper = objectMapper;
    this.jwtProvider = jwtProvider;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {

    JsonLoginFilter jsonLoginFilter = new JsonLoginFilter(authManager, objectMapper, jwtProvider);
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, userDetailsService, objectMapper);

    http
        // REST 서버: 브라우저 폼 로그인/Basic 인증 흐름 끔
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())

        // 실습 기준 CSRF 비활성
        .csrf(csrf -> csrf.disable())

        // JWT: 서버 상태 없음
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .authorizeHttpRequests(auth -> auth
            // 인증 엔드포인트
            .requestMatchers("/api/login").permitAll()

            // 회원가입만 공개
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

            // 그 외는 인증 필요
            .anyRequest().authenticated()
        )

        // JWT 검증 필터(요청마다)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        // JSON 로그인 필터(/api/login)
        .addFilterAt(jsonLoginFilter, UsernamePasswordAuthenticationFilter.class)

        // 401 / 403 응답을 ApiResponse로 통일
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((req, res, e) ->
                write(res, ErrorCode.UNAUTHORIZED, "인증이 필요합니다")
            )
            .accessDeniedHandler((req, res, e) ->
                write(res, ErrorCode.FORBIDDEN, "권한이 없습니다")
            )
        );

    return http.build();
  }

  private void write(HttpServletResponse res, ErrorCode code, String message) {
    try {
      res.setStatus(code.getStatus().value());
      res.setContentType(MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8");
      objectMapper.writeValue(res.getWriter(), ApiResponse.fail(code.name(), message));
    } catch (Exception ignore) {
      try {
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } catch (Exception ignored) {
      }
    }
  }
}
```

---

## 15. REST Client 테스트

```http
@baseUrl = http://localhost:8080
@token = 

### 1) 토큰 없음 (401)
PUT {{baseUrl}}/api/users/5/nickname?nickname=test

### 2) 로그인 (토큰 발급)
POST {{baseUrl}}/api/login
Content-Type: application/json

{
  "username": "test",
  "password": "1234"
}

### 3) 로그인 응답에서 accessToken 값을 복사해서 @token에 넣는다

### 4) 토큰 포함 요청 (성공)
PUT {{baseUrl}}/api/users/5/nickname?nickname=test
Authorization: Bearer {{token}}
```

---

## 16. 실습 완료 체크리스트

* 토큰 없이 보호 API는 401
* 로그인 성공 시 accessToken 반환
* 토큰 포함 요청 정상 처리
* 인증 실패는 Controller 이전에서 차단
* 서버는 세션을 생성하지 않는다 (STATELESS)

---

## 다음 단계

* Authorization (403)
* 본인만 수정 가능 처리
* 관리자 전용 API
* Refresh Token 전략
