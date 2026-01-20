# Controller 계층 구현

이 장에서는
Spring Boot 서버에서 **가장 바깥에서 요청을 받는 계층인 Controller**를 구현한다.

이 단계의 목적은
비즈니스 로직을 완성하는 것이 아니라,
**HTTP 요청이 서버 코드에 도달하고 응답이 돌아가는 흐름을 직접 체감**하는 것이다.


---

## 1. Controller 계층의 역할

Controller는
**외부 요청과 서버 내부 코드의 경계선**이다.

역할은 명확하다.

* HTTP 요청 수신
* 요청 데이터 파싱
* Service 호출
* HTTP 응답 반환

> Controller는
> **비즈니스 로직을 처리하지 않는다.**

---

## 2. Controller 패키지 생성

`com.koreanit.server` 아래에
controller 패키지를 생성한다.

```text
src/main/java/com/koreanit/server/controller
```

---

## 3. 첫 Controller 클래스 생성

다음 클래스를 생성한다.

```text
HelloController.java
```

```java
package com.koreanit.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Server";
    }
}
```

---

## 4. 서버 실행 및 확인

서버를 실행한다.

```bash
./gradlew bootRun
```

브라우저에서 접속:

```text
http://localhost:8080/hello
```

응답:

```text
Hello Spring Server
```

> 이 응답이 보이면
> **Controller가 정상적으로 동작**하고 있다.

---

## 5. HTTP 요청과 메서드의 연결

```java
@GetMapping("/hello")
```

의미:

* HTTP GET 요청
* 경로: `/hello`
* 이 요청이 들어오면
  `hello()` 메서드가 실행된다

Spring Boot는
이 연결을 자동으로 처리해준다.

---

## 6. JSON 응답으로 바꿔보기

문자열 대신
객체를 반환해본다.

```java
@GetMapping("/hello-json")
public Map<String, String> helloJson() {
    Map<String, String> result = new HashMap<>();
    result.put("message", "Hello JSON");
    return result;
}
```

응답:

```json
{
  "message": "Hello JSON"
}
```

> `@RestController`는
> 반환값을 자동으로 JSON으로 변환한다.

---

## 7. Controller에서 하면 안 되는 것

Controller에서는 다음을 하지 않는다.

* DB 직접 접근
* 복잡한 계산
* 비즈니스 규칙 처리

이것들은
**Service 계층의 역할**이다.

---

## 이 장의 핵심 정리

* Controller는 요청/응답 담당
* `@RestController`는 JSON 응답용
* Controller는 가볍게 유지한다
* 로직은 다음 단계로 넘긴다

---

## 다음 단계

→ [**05. Service 계층 구현**](05-service_layer.md)

