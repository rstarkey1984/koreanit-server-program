# 간단한 서버 동작 확인 실습 (/health)

이 장에서는
계층 구현에 들어가기 전에,
**Spring Boot 서버가 실제로 요청을 받고 응답하는지**를
가장 단순한 코드로 확인한다.

이 단계의 목표는 하나다.

> **“Spring Boot 서버가 실행 중이며,
> HTTP 요청에 응답한다”는 사실을 코드로 증명한다.**

---

## 1. 사전 상태 확인

다음 조건이 모두 만족되어야 한다.

* Spring Boot 프로젝트가 정상 생성되어 있다
* `Application.java` 파일이 존재한다
* `./gradlew bootRun` 명령으로 서버 실행이 가능하다

이 조건이 충족되지 않으면
이 장의 실습은 진행하지 않는다.

---

## 2. Controller 클래스 생성

Controller는
**HTTP 요청을 직접 받는 계층**이므로
`controller` 패키지 아래에 생성한다.

### 생성 경로

```text
src/main/java/com/koreanit/spring/controller
```

### 파일 생성

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

이 코드는
서버가 요청을 받을 수 있는 **가장 단순한 형태의 Controller**다.

---

### 3-1. `@RestController`란?

`@RestController`는 이 클래스가

* HTTP 요청을 처리하는 **Controller 클래스**이며
* 메서드의 반환값을 **그대로 HTTP 응답 본문(body)** 으로 사용한다

는 의미를 가진다.

그리고 이 단계에서 반드시 기억해야 할 사실이 하나 있다.

> `@RestController`가 붙은 클래스는
> **Spring이 관리하는 객체(Bean)** 로 자동 등록된다.

즉,

* 우리가 `new HealthController()`를 호출하지 않았지만
* 서버 시작 시 Spring이 **객체를 자동으로 생성**하고
* 요청이 들어오면 해당 객체의 메서드를 실행한다

---

### 3-2. Controller는 Bean이다

이 단계에서 기억해야 할 핵심 문장은 이것 하나다.

> **Controller 클래스는 Spring Bean이며,
> Spring Container에 의해 자동으로 생성·관리된다.**

지금 단계에서는

* Bean이 어떻게 생성되는지
* Spring Container가 내부에서 무엇을 하는지

까지 이해할 필요는 없다.

지금은 다음 사실만 명확히 인식하면 충분하다.

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
* 이 메서드를 실행한다

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

### 3-4. 반환값의 의미

이 메서드는 한 문장으로 정리할 수 있다.

> `/health` 경로로 GET 요청이 오면
> `"OK"` 문자열을 HTTP 응답으로 반환한다.

지금은 문자열을 반환하지만,
실제 서버에서는 **JSON 객체를 반환하는 경우가 대부분**이다.

이 부분은 이후 단계에서 다룬다.

---

## 4. 서버 실행

다음 명령어로 서버를 실행한다.

```bash
./gradlew bootRun
```

아래와 유사한 로그가 보이면 정상 실행된 것이다.

```text
Started Application
Tomcat started on port(s): 8080
```

---

## 5. 요청 확인

다른 터미널을 열고 다음 명령을 실행한다.

```bash
curl http://localhost:8080/health
```

정상 응답:

```text
OK
```

이 응답이 나오면
다음 사실이 모두 증명된 것이다.

* 서버가 실행 중이다
* HTTP 요청을 정상적으로 받고 있다
* Spring이 생성한 Controller 객체가 존재한다
* 요청에 따라 메서드가 실행된다
* 반환값이 HTTP 응답으로 전달된다

---

## 이 장의 핵심 포인트

* Spring Boot 서버는 **실행만으로도 HTTP 요청을 처리할 수 있다**
* Controller는 **HTTP 요청의 진입점**이다
* Controller 객체는 **Spring이 자동으로 생성·관리한다**
* 우리는 아직 객체를 `new`로 생성하지 않았다

---

## 다음 단계

다음 장에서는
이 객체를 생성하고 관리하는 주체인 **Spring Container와 Bean 개념**을 정리한다.

→ [**03. Spring Container와 Bean**](03-spring_bean.md)
