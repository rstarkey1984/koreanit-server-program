# 간단한 서버 동작 확인 실습 (/health)

이 장에서는
계층 구현에 들어가기 전에,
**Spring Boot 서버가 실제로 요청을 받고 응답하는지**를
가장 단순한 코드로 확인한다.

목표는 하나다.

> "Spring Boot 서버가 실행 중이며, HTTP 요청에 응답한다"를 코드로 증명한다.

---

## 1. 사전 상태 확인

다음 조건이 만족되어야 한다.

* Spring Boot 프로젝트가 정상 생성됨

* `Application.java` 존재

* `./gradlew bootRun`으로 서버 실행 가능

---

## 2. Controller 클래스 생성

경로:

```text
src/main/java/com/koreanit/spring/controller
```

파일 생성:

```text
HealthController.java
```

---

## 3. HealthController 구현

```java
package com.koreanit.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

---

### 3-1. `@RestController`란?

`@RestController`는 이 클래스가

* HTTP 요청을 처리하는 **Controller 클래스**이며
* 메서드의 반환값을 **그대로 HTTP 응답 본문(body)** 으로 사용한다

는 것을 의미한다.

추가로 중요한 사실이 하나 있다.

> `@RestController`가 붙은 클래스는
> **Spring이 관리하는 객체(Bean)** 로 자동 등록된다.

즉, 이 클래스는

* 우리가 `new HealthController()`를 호출하지 않았지만
* Spring이 서버 시작 시 **객체를 자동으로 생성**하고
* 요청이 들어오면 해당 객체의 메서드를 실행한다

---

### 3-2. Controller는 Bean이다

이 단계에서 기억해야 할 핵심 사실은 다음 한 문장이다.

> **Controller 클래스는 Spring Bean이며,
> Spring Container에 의해 자동으로 생성·관리된다.**

현재 단계에서는
Bean의 생성 원리나 내부 동작을 이해할 필요는 없다.

지금은 다음 사실만 확인하면 충분하다.

* Controller 객체는 **개발자가 직접 생성하지 않는다**
* 서버 시작 시 **Spring이 자동으로 생성한다**
* 요청이 들어오면 **이미 생성된 객체의 메서드가 실행된다**

---

### 3-3. `@GetMapping`이란?

```java
@GetMapping("/health")
```

이 설정은 다음 의미를 가진다.

* HTTP **GET** 요청 중에서
* 요청 경로가 `/health` 인 경우
* 이 메서드가 실행된다

따라서 다음 요청이 들어오면

```text
GET /health
```

아래 메서드가 실행된다.

```java
public String health() {
    return "OK";
}
```

---

### 3-4. 이 메서드의 역할 요약

이 메서드는 한 문장으로 정리하면 다음과 같다.

> `/health`로 GET 요청이 오면
> `"OK"` 문자열을 HTTP 응답으로 반환한다.

---

## 4. 서버 실행

다음 명령어로 서버를 실행한다.

```bash
./gradlew bootRun
```

로그에서 다음 메시지가 보이면 정상 실행된 것이다.

```text
Started Application
Tomcat started on port 8080
```

---

## 5. 요청 확인

다른 터미널을 열어 다음 명령을 실행한다.

```bash
curl http://localhost:8080/health
```

정상 응답:

```text
OK
```

이 응답이 나오면 다음 사실이 모두 확인된다.

* 서버가 실행 중이다
* HTTP 요청을 받고 있다
* Spring이 생성한 Controller 객체가 존재한다
* 요청에 따라 메서드가 실행된다
* 반환값이 HTTP 응답으로 전달된다

---

## 다음 단계

다음 장에서는
이 객체를 생성하고 관리하는 주체인
**Spring Container와 Bean 개념**을 다룬다.

→ [**03. Spring Container와 Bean**](03-spring_bean.md)