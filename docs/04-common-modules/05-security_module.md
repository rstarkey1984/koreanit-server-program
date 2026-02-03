# Security Module 적용 

## 1. 이 단계의 목적

**Spring Security를 프로젝트에 포함만 하고, 기존 동작은 전혀 바꾸지 않는다.**

* 인증(Authentication) / 인가(Authorization) **미적용**
* 실제 차단 정책은 **다음 단계에서** 시작

---

## 2. 왜 이 단계가 필요한가

`spring-boot-starter-security` 를 추가하면, 설정이 없어도 즉시 다음이 발생한다.

* 모든 요청 기본 차단
* 로그인 화면(Form Login) 자동 노출
* 인증 실패 시 HTML 응답

이는 현재 프로젝트 원칙과 충돌한다.

* REST API 서버
* 모든 응답은 JSON(ApiResponse)
* 아직 보안 정책 적용 단계 아님

따라서 이 단계에서는 **보안 모듈을 무력화된 상태로 먼저 도입**한다.

---

## 3. SecurityFilterChain 개념

* 모든 HTTP 요청은 Controller 전에 **SecurityFilterChain** 을 통과한다
* Spring Security는 여러 보안 필터가 **순서대로 실행되는 구조**다

```
Client
 → Servlet Filter
 → Spring Security FilterChain
 → DispatcherServlet
 → Controller
```

---

## 4. SecurityConfig의 의미

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http)
```

이 선언의 의미:

* Spring Boot의 **기본 보안 정책을 사용하지 않겠다**
* **내가 정의한 보안 정책으로 요청을 처리하겠다**

즉, SecurityConfig는 **보안 정책 선언 파일**이다.

---

## 5. 이 단계에서의 SecurityFilterChain 역할

이번 단계의 목표는 하나다.

> **Security는 존재하지만, 아무 요청도 막지 않는다**

이를 위해 다음 상태를 만든다.

* 모든 요청 허용
* 로그인 화면 없음
* 인증 팝업 없음
* 기존 예외 처리 / 로깅 흐름 유지

---

## 6. 최소 SecurityConfig (OPEN 체인)

```java
package com.koreanit.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http 
            // CSRF 비활성화
            .csrf(csrf -> csrf.disable())
            // 모든 요청 허용
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
```

이 설정의 효과:

* 기본 차단 정책 제거
* 모든 API 기존과 동일하게 동작
* SecurityFilterChain이 **OPEN 상태**로 동작

---

## 이 단계가 끝났을 때 보장되는 상태

* 모든 API 정상 동작
* 401 / 403 발생 없음
* 로그인 화면 없음
* HTML 응답 없음
* ApiResponse 규칙 유지
* GlobalExceptionHandler 그대로 사용
* AccessLogFilter 그대로 사용

즉,

> **Security가 들어왔지만, 서버 동작은 변하지 않은 상태**

---

## 다음 단계

→ [**Users & Posts API — 전체 구조**](/docs/05-api-project/00-intro.md)