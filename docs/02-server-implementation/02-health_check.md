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

import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return LocalDateTime.now().toString();
    }
}
```

* `@GetMapping("/health")` → URL과 메서드 연결됨

* `LocalDateTime.now()` → 요청 시점마다 값 변경됨

* `@RestController` → 반환 문자열이 HTTP 응답 본문으로 사용됨

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

```bash
http://localhost:8080/health
```

---

## 6. 응답 결과 해석

앞 단계에서 `/health` 요청을 보내면
다음과 같은 문자열이 응답으로 반환된다.

```text
2026-01-23T14:52:31.123
```

이 결과는 **정적 파일이 아니라 서버 코드 실행 결과**임

---

### 6-1. 요청이 들어왔을 때 일어난 일

이 요청이 처리되는 과정은 다음과 같다.

```text
1. 클라이언트가 GET /health 요청을 보낸다
2. Spring Boot 서버가 요청을 수신한다
3. URL(/health)에 매핑된 Controller 메서드를 찾는다
4. HealthController.health() 메서드를 실행한다
5. 실행 시점의 현재 시각을 계산한다
6. 계산된 값을 문자열로 반환한다
7. 반환값을 HTTP 응답으로 전달한다
```

요청이 들어올 때마다
**메서드 재실행됨**
**실행 시점의 결과가 응답으로 사용됨**

---

### 6-2. 왜 요청할 때마다 값이 달라지는가

```java
return LocalDateTime.now().toString();
```

이 코드 특성:

* 서버 실행 시 한 번 실행되는 코드가 아니라
* **요청이 들어올 때마다 실행되는 코드**다

따라서:

* 새로 요청하면
* 현재 시각이 다시 계산되고
* 다른 값이 응답으로 반환된다

**동적 응답에 해당**

---

### 6-3. 크롬 개발자 도구에서 보이는 DOM

![alt text](/img/automatic_dom.png)

* 서버 응답: 날짜·시간 문자열 1줄
* HTML 태그(`<html>`, `<head>`, `<body>`) 응답 없음
* 브라우저가 최소 HTML 문서 구조 자동 생성
* 응답 문자열은 `<body>` 내부 텍스트 노드로 배치

API 응답과 브라우저 DOM은 동일하지 않음

---

## 이 장의 핵심 포인트

* Spring Boot 서버는 **HTTP 요청을 처리할 수 있다**
* Controller는 **HTTP 요청의 진입점**이다
* Controller 객체는 **Spring이 자동으로 생성·관리한다**

---

## 실습 단계

### `@GetMapping` 으로 동작하는 새로운 메서드를 만들어서 브라우저에서 확인해본다.



## 다음 단계

다음 장에서는
이 객체를 생성하고 관리하는 주체인 **Spring Container와 Bean 개념**을 정리한다.

→ [**03. Spring Container와 Bean**](03-spring_bean.md)
