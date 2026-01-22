# 요청 로깅 전략

이 장에서는
서버로 들어오는 **요청(Request)과 나가는 응답(Response)을 어떻게 기록할지** 기준을 정한다.

앞 장에서 에러 처리의 구조를 정했다면,
이번 장에서는 **서버가 실제로 어떤 일을 했는지 남기는 방법**을 다룬다.


---

## 1. 요청 로깅이 필요한 이유

에러 로그만으로는
서버에서 어떤 일이 일어났는지 알기 어렵다.

요청 로깅은 다음 질문에 답하기 위해 필요하다.

* 어떤 요청이 들어왔는가
* 언제 들어왔는가
* 어떤 경로로 처리되었는가
* 정상 처리되었는가

> 요청 로깅은
> 서버의 "행동 기록"이다.

---

## 2. 요청 로깅과 에러 로깅의 역할 차이

| 구분 | 요청 로깅 | 에러 로깅    |
| -- | ----- | -------- |
| 목적 | 흐름 추적 | 문제 원인 분석 |
| 대상 | 모든 요청 | 실패 상황    |
| 시점 | 요청/응답 | 예외 발생    |

둘은 서로 대체 관계가 아니다.

---

## 3. 무엇을 로그로 남길 것인가

요청 로깅에서
반드시 포함되어야 할 정보는 다음과 같다.

* HTTP 메서드
* 요청 URL
* 요청 시각
* 응답 상태 코드
* 처리 시간

이 정보만으로도
대부분의 요청 흐름을 추적할 수 있다.

---

## 4. 로그를 남기면 안 되는 것

요청 로깅에서
다음 정보는 기록하지 않는다.

* 비밀번호
* 인증 토큰
* 개인정보
* 요청 바디 전체

> 로그는 외부에 노출될 수 있다는 전제를 항상 가져야 한다.

---

## 5. 요청 로깅 위치

요청 로깅은
Controller 내부에서 처리하지 않는다.

권장 위치:

* 필터(Filter)
* 인터셉터(Interceptor)

이 위치에서 처리하면
모든 요청을 일관되게 기록할 수 있다.

---

## 6. 지금 단계에서 하지 않는 것

이 장에서는 다음을 다루지 않는다.

* 로그 포맷 커스터마이징
* MDC, Trace ID
* 분산 추적

지금 단계의 목표는
**"요청 흐름이 기록된다"는 감각을 만드는 것**이다.

---

## 이 장의 핵심 정리

* 요청 로깅은 서버 행동 기록이다
* 에러 로그와 목적이 다르다
* 최소한의 정보만 남긴다
* 로깅 위치는 Controller 바깥이다

---

## 실습 목표

* 모든 HTTP 요청이 자동으로 로그에 남도록 만든다
* 처리 시간과 응답 상태 코드를 함께 기록한다
* Controller 코드를 수정하지 않고 로깅을 구현한다

---

## 실습 준비

* Spring Boot 프로젝트 실행 상태
* 간단한 테스트용 Controller 존재 (`/api/health` 등)

---

## 실습 과제

### 1단계: 요청 로깅 인터셉터 생성

요청 시작 시각과 종료 시각을 측정해 로그로 남긴다.

예시 코드:

```java
package com.example.api.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        System.out.println(
            String.format(
                "%s %s -> %d (%d ms)",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration
            )
        );
    }
}
```

---

### 2단계: 인터셉터 등록

모든 요청에 대해 인터셉터가 동작하도록 설정한다.

예시 코드:

```java
package com.example.api.config;

import com.example.api.logging.RequestLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public WebConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**");
    }
}
```

---

### 3단계: 동작 확인

* `/api/health` 또는 임의의 API 호출
* 콘솔 로그에 다음 정보가 출력되는지 확인

예:

```
GET /api/health -> 200 (3 ms)
```

---

## 체크 포인트

* Controller 코드를 수정하지 않았는가?
* 모든 요청이 빠짐없이 로그로 남는가?
* 처리 시간이 정상적으로 계산되는가?
* 민감한 정보가 로그에 포함되지 않는가?

---

## 다음 단계

→ [**환경별 설정 분리**](05-profiles_and_config.md)
