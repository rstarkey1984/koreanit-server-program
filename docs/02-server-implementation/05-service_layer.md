# Service 계층 구현

이 장에서는
Controller에서 처리하던 로직을 분리하여
**Service 계층의 역할과 책임**을 명확히 한다.

이 단계의 핵심은
"왜 Service가 필요한가"를 코드로 체감하는 것이다.

---

## 1. 왜 Service 계층이 필요한가

이전 단계에서 만든 Controller는
요청을 받아 바로 응답을 반환했다.

```java
@GetMapping("/hello")
public String hello() {
    return "Hello Spring Server";
}
```

이 방식은 간단하지만,
로직이 복잡해지면 문제가 생긴다.

* Controller가 비대해진다
* 테스트가 어려워진다
* 재사용이 불가능해진다

그래서 **로직을 전담하는 계층**이 필요하다.

---

## 2. Service 계층의 역할

Service는 **비즈니스 로직을 담당하는 계층**이다.

역할:

* 도메인 규칙 처리
* 여러 작업의 흐름 제어
* Repository 호출
* 트랜잭션 경계 설정(나중에 다룸)

핵심 원칙:

> Service는 HTTP를 모른다.
> Request/Response, URL, Controller 어노테이션을 사용하지 않는다.
> 순수한 자바 로직만 가진다.

---

## 3. Service 패키지 생성

다음 패키지를 생성한다.

```text
src/main/java/com/koreanit/spring/service
```

---

## 4. Service 클래스 생성

`HelloService` 클래스를 생성한다.

```java
package com.koreanit.spring.service;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String helloMessage() {
        return "Hello from Service";
    }
}
```

* `@Service`: Spring이 이 클래스를 Bean으로 등록한다.

---

## 5. Controller에서 Service 사용하기

Controller 코드를 수정한다.

```java
package com.koreanit.spring.controller;

import com.koreanit.spring.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping("/hello")
    public String hello() {
        return helloService.helloMessage();
    }
}
```

중요 포인트:

* Controller는 로직을 직접 처리하지 않는다
* Service를 **호출만** 한다
* HelloService를 `new`로 생성하지 않는다
* 객체 생성과 의존성 연결은 **Spring 컨테이너의 책임**이다(DI)

---

## 6. 동작 흐름 정리

### 1) 서버 시작 시

* Spring이 `@RestController`, `@Service`가 붙은 클래스를 찾아 Bean으로 등록한다.
* HelloController 생성 시 HelloService를 자동 주입한다(DI).

### 2) 요청이 들어오면

* `GET /hello` 요청 → DispatcherServlet(요청 진입점)이 받는다.
* URL 매핑에 따라 `HelloController.hello()`가 선택된다.

### 3) 컨트롤러 실행

* `hello()` 메서드가 실행된다.
* 실제 로직은 `HelloService.helloMessage()`에 위임된다.

### 4) 응답 반환

* Service의 반환값(String)을 `@RestController`가 HTTP 응답 바디로 변환해 반환한다.

---

## 7. 서버 실행 및 확인

```bash
./gradlew bootRun
```

접속:

```text
http://localhost:8080/hello
```

응답:

```text
Hello from Service
```

---

## 8. 계층 분리의 효과

이제 구조는 다음과 같다.

```text
Controller → Service
```

장점:

* Controller는 가벼워진다
* 서비스 로직 재사용이 가능해진다
* 테스트 가능성이 증가한다

다음 단계에서 Repository를 추가하면
구조는 더 명확해진다.

---

## 이 장의 핵심 정리

* Service는 비즈니스 로직 담당
* Controller는 요청/응답만 담당
* 로직은 Service로 이동한다

---

## 다음 단계

→ [**06. Repository 계층의 역할과 책임**](06-repository_layer.md)
