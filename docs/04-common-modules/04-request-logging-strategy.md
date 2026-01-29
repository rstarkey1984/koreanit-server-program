# 요청 로깅 전략 (Filter 기반)

이 장에서는 서버로 들어오는 **모든 HTTP 요청이 자동으로 로그에 남도록** 만든다.

이 단계의 목표는 하나다.

> **“요청이 들어왔고, 처리되었고, 최종적으로 어떤 응답이 나갔는지”를
> 요청 1건당 1줄로 정확히 남긴다.**

---

## 1. 왜 로그를 남기나

서버 개발에서 가장 먼저 필요한 것은

> **서버가 실제로 요청을 받았고, 끝까지 처리했는지 확인할 수 있는 기록**

이다.

그래서 이 단계에서는 **비즈니스 로직과 무관한 “접근 로그(Access Log)”** 를 먼저 만든다.

---

## 2. 지금 남길 로그 정보

아래 4가지만 남긴다.

* HTTP 메서드
* 요청 경로
* 최종 응답 상태 코드
* 전체 처리 시간(ms)

이 로그는:

* 성공 요청
* 실패 요청
* 예외 발생 요청

모두에 대해 **동일한 형식으로** 남아야 한다.

---

## 3. 로깅은 어디에서 처리할까

Controller 안에서 로그를 찍지 않는다.

이 강의에서는 **Filter에서 요청 로그를 처리**한다.

---

### 왜 Filter인가?

Filter는 다음 특징을 가진다.

* **서블릿 컨테이너 레벨**에서 동작한다
* 요청의 시작부터 끝까지 **한 번에 감쌀 수 있다**
* 예외가 발생해도 `finally`에서 **항상 실행된다**

즉,

> **“요청 1건 = 로그 1줄”을 가장 안정적으로 보장할 수 있는 위치**

가 Filter다.

---

## 4. 요청이 처리되는 전체 흐름과 Filter 위치

브라우저나 클라이언트가 요청을 보내면 서버 내부에서는 다음 순서로 처리된다.

```text
[Client]
   ↓
[Filter]                ← 서블릿 컨테이너 영역
   ↓
[DispatcherServlet]     ← 서블릿 컨테이너 + Spring 진입점
   ↓
===============================
   Spring Container 영역
   (Bean 관리 공간)
===============================
[Controller Bean]
   ↓
[Service Bean]
   ↓
[Repository Bean]
===============================
   ↓
[DispatcherServlet]
   ↓
[Filter]
   ↓
[Response]
```

중요한 점:

* Filter는 **DispatcherServlet 앞뒤를 모두 감싼다**
* Filter는 **요청 전체를 한 번으로 처리**한다

그래서 Access Log 용도로 가장 적합하다.

---

## 5. 요청 로깅 Filter 만들기

### 5-1. Filter 파일 생성

파일 경로:

```text
logging/AccessLogFilter.java
```

Spring Boot에서는 `OncePerRequestFilter`를 사용한다.

이 필터는:

* 요청 1건당 **딱 한 번만 실행**된다
* 중복 실행을 자동으로 막아준다

---

### 5-2. AccessLogFilter 구현

```java
package com.koreanit.spring.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 브라우저 자동 요청 / 내부 처리 요청 제외
        return uri.equals("/favicon.ico")
                || uri.startsWith("/.well-known")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // 실제 요청 처리
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
        }
    }
}
```

### 지금 코드의 핵심은 이 구조
```java
long start = System.currentTimeMillis();

try {
    filterChain.doFilter(request, response);
} finally {
    log.info(... response.getStatus() ...);
}
```

### 이게 의미하는 실제 흐름
```text
1. Tomcat이 요청 받음
2. Tomcat → Filter 호출
3. Filter → DispatcherServlet
4. Controller/Service 처리
5. DispatcherServlet 종료
6. Filter finally 실행 (로그)
7. Filter 메서드 종료 (암묵적 return)
8. Tomcat 코드 재개
9. Tomcat이 응답을 네트워크로 전송
```


---

## 6. 동작 확인

테스트 API 호출해보기

---

## 이 장의 핵심 요약

* Access Log는 **비즈니스 로직과 분리**한다
* 요청 전체를 감싸려면 **Filter가 가장 적합**하다

---


## 다음 단계

→ **API 프로젝트 시작**

