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

즉,

> 이 클래스는 **웹 요청을 받는 진입점** 역할을 한다.

---

### 3-2. `@GetMapping`이란?

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

### 3-3. 이 메서드의 역할 요약

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

이 응답이 나오면,

* 서버가 실행 중이며
* HTTP 요청을 받고
* Controller 메서드가 실행되어
* 응답을 반환하고 있음이 확인된다

---

## 다음 단계

→ [**04. Controller 계층 구현**](04-controller_layer.md)

다음 장부터는
Controller가 어떤 역할을 가지는지,
그리고 요청 데이터를 어떻게 처리하는지를 단계적으로 살펴본다.
