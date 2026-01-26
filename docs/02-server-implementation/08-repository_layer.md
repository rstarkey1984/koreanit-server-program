# Repository 계층의 역할과 책임

이 장에서는
Spring Boot 서버에서 **Repository 계층이 왜 필요한지**,
그리고 **어떤 책임만 가져야 하는지**를 구조 관점에서 정리한다.

이 단계의 목적은
아직 데이터를 실제로 저장하거나 조회하지 않고,
**“Service 다음 계층은 무엇이며, 왜 분리되어야 하는가”를 이해하는 것**이다.

---

## 1. 서버 애플리케이션의 관심사 분리

서버 애플리케이션은
다음과 같이 서로 다른 책임을 가진 코드들로 구성된다.

* HTTP 요청/응답 처리
* 비즈니스 규칙 및 흐름 제어
* 데이터 조회 및 저장

이 책임들이 한 곳에 섞이기 시작하면
코드는 빠르게 복잡해지고 유지보수가 어려워진다.

그래서 서버 구조는
**역할에 따라 계층을 분리**한다.

---

## 2. Repository 계층은 왜 필요한가

Service 계층은
비즈니스 흐름과 규칙을 담당한다.

하지만 Service가 직접
데이터의 저장 방식이나 조회 방법까지 알게 되면:

* Service 코드가 특정 저장소에 종속되고
* 테스트와 재사용이 어려워지며
* 구조 변경 시 영향 범위가 커진다

이 문제를 해결하기 위해
**데이터 접근만 전담하는 계층**이 필요하다.

이 계층이 바로 **Repository**다.

---

## 3. Repository 계층의 책임

Repository는
**데이터 접근만 담당하는 계층**이다.

Repository의 역할은 명확하다.

* 데이터 조회
* 데이터 저장
* 저장소로부터 결과를 받아 반환

Repository는
**비즈니스 판단을 하지 않는다.**

조건 분기, 규칙 판단, 흐름 제어는
모두 Service의 책임이다.

---

## 4. Service와 Repository의 관계

```text
Service
  - 흐름 제어
  - 조건 판단
  - 여러 작업 조합
  - 비즈니스 규칙 처리

Repository
  - 데이터 접근 전담
  - 저장소와의 통신
  - 결과 반환
```

Service는
Repository가 반환한 결과를 바탕으로
**의미 있는 판단과 흐름 제어**를 수행한다.

---

## 5. Repository 메서드 설계 기준

Repository 메서드는
**데이터 접근 단위로 설계**하는 것이 원칙이다.

### 기본 원칙

* 하나의 조회/저장 동작 = 하나의 메서드
* 메서드 내부에 비즈니스 로직 없음
* 판단하지 않고 결과만 반환

Repository는
“무엇을 할지”가 아니라
“어떻게 가져올지”만 책임진다.

---

## 6. Repository 메서드 이름 규칙

Repository 메서드 이름에는
**비즈니스 의미를 담지 않는다.**

### 잘못된 예

```text
checkUserLogin()
processSignup()
validateUser()
```

### 올바른 예

```text
findById()
findByEmail()
countByEmail()
save()
```

---

## 7. 반환 타입 설계 기준

Repository는
**판단하지 않고 결과만 반환**한다.

| 조회 성격 | 반환 기준            |
| ----- | ---------------- |
| 개수 조회 | 숫자 타입            |
| 존재 여부 | boolean 또는 개수 결과 |
| 단건 조회 | 객체 또는 null       |
| 목록 조회 | List             |

---

## 8. Repository 클래스 생성 (구조 확인용)

이 단계에서는
실제 데이터 저장소와 연결하지 않고,
**Repository 클래스의 형태만 먼저 만들어 본다.**

### 8-1. Repository 패키지 생성

```text
src/main/java/com/koreanit/spring/repository
```

### 8-2. Repository 클래스 생성

```text
HelloRepository.java
```

```java
package com.koreanit.spring.repository;

import org.springframework.stereotype.Repository;

@Repository
public class HelloRepository {

    public String ping() {
        return "pong";
    }
}
```

---

## 9. Service ↔ Repository 연결

이 단계에서는
`HelloRepository`를 **Service가 주입받아 호출**하는 형태로 연결한다.

### 9-1. Service 클래스 생성

```text
src/main/java/com/koreanit/spring/service
```

```text
HelloService.java
```

### 9-2. HelloService 구현

```java
package com.koreanit.spring.service;

import com.koreanit.spring.repository.HelloRepository;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    private final HelloRepository helloRepository;

    public HelloService(HelloRepository helloRepository) {
        this.helloRepository = helloRepository;
    }

    public String ping() {
        return helloRepository.ping();
    }
}
```

### 9-3. Controller → Service → Repository 흐름

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

    @GetMapping("/ping")
    public String ping() {
        return helloService.ping();
    }
}
```

```text
Controller → Service → Repository
```

---

## 이 장의 핵심 정리

* Repository는 데이터 접근 전담 계층이다
* 비즈니스 판단은 Service의 책임이다
* Service와 Repository는 반드시 분리된다
* 지금은 구현이 아니라 **구조 이해가 목적**이다

---

## 다음 단계

다음 장에서는
Repository 계층이 **실제 데이터베이스와 연결되는 방식(JDBC 커넥션 풀)** 을 다룬다.

→ [**데이터베이스 연결(JDBC 커넥션 풀)**](09-jdbc_connection_pool.md)
