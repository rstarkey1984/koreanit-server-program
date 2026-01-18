# Service 계층 구현

이 장에서는
Controller에서 처리하던 로직을 분리하여
**Service 계층의 역할과 책임**을 명확히 한다.

이 단계의 핵심은
"왜 Service가 필요한가"를 코드로 체감하는 것이다.

---

## 강의 목표

* Service 계층의 역할을 설명할 수 있다
* Controller와 Service의 책임을 구분할 수 있다
* Controller에서 로직을 제거하고 Service로 위임한다
* 계층 분리의 필요성을 이해한다

---

## 1. 왜 Service 계층이 필요한가

이전 장에서 만든 Controller는
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

Service는
**비즈니스 로직을 담당하는 계층**이다.

역할:

* 도메인 규칙 처리
* 여러 작업의 흐름 제어
* Repository 호출
* 트랜잭션 경계 설정(나중에 다룸)

> Service는
> HTTP를 모른다.

---

## 3. Service 패키지 생성

다음 패키지를 생성한다.

```text
src/main/java/com/koreanit/server/service
```

---

## 4. Service 클래스 생성

`HelloService` 클래스를 생성한다.

```java
package com.koreanit.server.service;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String helloMessage() {
        return "Hello from Service";
    }
}
```

* `@Service`:
  Spring이 이 클래스를 Service 컴포넌트로 인식

---

## 5. Controller에서 Service 사용하기

Controller 코드를 수정한다.

```java
package com.koreanit.server.controller;

import com.koreanit.server.service.HelloService;
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
* Service를 호출만 한다

---

## 6. 서버 실행 및 확인

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

> 응답이 동일하더라도
> **구조는 완전히 달라졌다.**

---

## 7. 계층 분리의 효과

이제 구조는 다음과 같다.

```text
Controller → Service
```

장점:

* Controller는 가벼워진다
* 로직 재사용 가능
* 테스트 가능성 증가

이 다음 단계에서
Repository를 추가하면 구조는 더 명확해진다.

---

## 이 장의 핵심 정리

* Service는 비즈니스 로직 담당
* Controller는 요청/응답만 담당
* 로직은 반드시 Service로 이동한다

---

## 다음 단계

→ [**06. Repository 계층과 SQL**](06-mysql_install_setup.md)
