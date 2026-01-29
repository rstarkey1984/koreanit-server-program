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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return LocalDateTime.now().toString();
    }

    @GetMapping("/health/check")
    public String check(
            @RequestParam String name,
            @RequestParam int count
    ) {
        System.out.println("[Controller] name = " + name);
        System.out.println("[Controller] count = " + count);

        return "name=" + name + ", count=" + count;
    }
}
```

* `@GetMapping("/health")` → URL과 메서드 연결됨
* `@GetMapping("/health/check")` → 요청 파라미터 테스트용 엔드포인트
* `@RequestParam` → URL 쿼리 스트링 값을 메서드 인자로 전달

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

브라우저에서 아래 주소로 접속한다.

```text
http://localhost:8080/health
```

```text
http://localhost:8080/health/check?name=test&count=3
```

---

## 6. 응답 결과 해석

### 6-1. 브라우저 응답

```text
name=test, count=3
```

### 6-2. 서버 콘솔 출력

```text
[Controller] name = test
[Controller] count = 3
```

---

## 7. 이 요청이 처리되는 흐름

```text
1. 클라이언트가 GET /health/check 요청을 보낸다
2. Tomcat이 HTTP 요청을 받는다
3. 요청을 DispatcherServlet에게 전달한다
4. DispatcherServlet이 URL(/health/check)을 기준으로
   매핑된 Controller 메서드를 찾는다
5. RequestParam 값을 메서드 인자로 변환한다
6. HealthController.check()를 호출한다
7. 반환값을 HTTP 응답으로 변환한다

```

요청이 들어올 때마다
메서드는 다시 실행되고,
요청에 포함된 값이 매번 새로 바인딩된다.

---

# 실습

### 1. 요청 파라미터가 없을 때의 동작 확인

다음 요청을 보낸다.

```text
http://localhost:8080/health/check?name=test
```

브라우저에 표시되는 결과와 서버 콘솔 로그를 확인한다.

* Controller 메서드가 실행되었는지 여부
* 에러가 발생한 시점이 어디인지

---

### 2. 기본값(defaultValue) 적용

`count` 파라미터가 없을 때의 동작을 변경한다.

기존 코드:

```java
@RequestParam int count
```

수정 코드:

```java
@RequestParam(defaultValue = "0") int count
```

이후 동일한 요청을 다시 보낸다.

```text
http://localhost:8080/health/check?name=test
```

응답 결과와 콘솔 출력을 비교한다.

---

### 3. 기본값이 적용되는 시점 관찰

다음 사실을 정리한다.

* 기본값 설정은 Controller 메서드 내부 코드가 아니다
* 메서드 실행 전에 값이 이미 결정되어 전달된다

---

## 이 장의 핵심 포인트

* Spring Boot 서버는 **HTTP 요청에 응답할 수 있다**
* Controller는 **HTTP 요청의 진입점**이다
* `@RequestParam`은 **요청 값을 메서드 입력으로 연결한다**
* 요청 값이 없거나 타입이 맞지 않으면 **Controller 진입 전에 에러가 발생할 수 있다**
* Controller 객체와 호출 시점은 **Spring이 관리한다**

---

## 다음 단계

다음 장에서는
이 Controller 객체를 생성하고 관리하는 주체인
**Spring Container와 Bean 개념**을 정리한다.

→ [**VS Code REST Client 사용법**](04-vscode_rest_client.md)