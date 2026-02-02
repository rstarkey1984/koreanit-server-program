# 요청 로깅 전략 (Filter 기반)

이 장에서는 서버로 들어오는 **모든 HTTP 요청이 자동으로 로그에 남도록** 만든다.

이 단계의 목표는 하나다.

> **“요청이 들어왔고, 처리되었고, 최종적으로 어떤 응답이 나갔는지”를
> 요청 1건당 1줄로 정확히 남긴다.**

---

## 1. 왜 로그를 남기나

서버 개발에서 필요한 것은

> **서버가 실제로 요청을 받았고, 끝까지 처리했는지 확인할 수 있는 기록**

이다.

* 클라이언트가 "요청 보냈다"고 할 때
* 프론트엔드가 "응답이 이상하다"고 할 때
* 운영 중 "어떤 요청에서 에러가 났는지"를 찾을 때

로그 없이는 확인할 방법이 없다.

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
package com.koreanit.spring.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

  private boolean isNoiseRequest(String uri) {
    return uri.equals("/favicon.ico")
        || uri.equals("/robots.txt")
        || uri.equals("/manifest.json")
        || uri.equals("/site.webmanifest")
        || uri.equals("/browserconfig.xml")

        // iOS / Android 아이콘
        || uri.startsWith("/apple-touch-icon")
        || uri.startsWith("/android-chrome")

        // Chrome / 브라우저 내부
        || uri.startsWith("/.well-known");
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();

    return uri.equals("/error");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String uri = request.getRequestURI();

    // 브라우저 자동 노이즈 요청은 여기서 즉시 종료
    if (isNoiseRequest(uri)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // 요청 1건을 식별하기 위한 Trace ID 생성
    String requestId = UUID.randomUUID().toString();

    // 로그 MDC에 저장 (같은 요청 로그 묶기)
    MDC.put("requestId", requestId);

    // 클라이언트에서도 확인 가능하도록 응답 헤더에 포함
    response.setHeader("X-Request-Id", requestId);

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

      // ThreadLocal 정리
      MDC.clear();
    }
  }
}
```

---

## 6. logback-spring.xml 설정 (requestId 출력)
> Spring Boot 애플리케이션에서 로그를 “어떻게, 어떤 형식으로, 어디에” 남길지 정의하는 설정 파일. Spring Boot는 부팅 시 자동으로 이 파일을 찾는다.

AccessLogFilter에서 `MDC.put("requestId", ...)`로 저장한 값은
**로그 포맷에서 `%X{requestId}`를 사용해야 출력된다.**

즉,

* Filter는 requestId를 만든다
* MDC는 requestId를 "요청 컨텍스트"로 보관한다
* logback-spring.xml은 MDC 값을 로그에 찍는 "출력 규칙"을 정의한다

---

### 6-1. 파일 위치

다음 경로에 생성한다.

```text
src/main/resources/logback-spring.xml
```

---

### 6-2. 최소 설정

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{requestId}] %logger - %msg%n
            </pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

</configuration>
```

---

### 6-3. 출력 예시

```text
2026-01-30 18:45:12.321 INFO  [9f3a-...] com.koreanit.spring.logging.AccessLogFilter - [9f3a-...] GET /users/10 -> 404 (12 ms)
2026-01-30 18:45:12.325 WARN  [9f3a-...] com.koreanit.spring.error.GlobalExceptionHandler - [API_EXCEPTION] code=NOT_FOUND_RESOURCE, message=..., origin=...
```

핵심:

* 같은 요청이면 AccessLogFilter 로그와 GlobalExceptionHandler 로그에 **같은 requestId**가 찍힌다
* requestId는 코드에서 전달하지 않아도 MDC로 자동 전파된다

---

## 이 장의 핵심 요약

* 요청 로깅은 **비즈니스 로직과 분리**한다
* 요청 전체를 감싸려면 **Filter가 가장 적합**하다
* Trace ID를 사용하면 로그 추적이 쉬워진다
* 요청 1건당 로그 1줄을 안정적으로 보장할 수 있다

---

## 다음 단계

→ [**Security Module 적용**](05-security_module.md)
