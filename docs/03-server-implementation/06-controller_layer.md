# Controller 계층 구현

이 장에서는
Spring Boot 서버에서 가장 바깥에서 요청을 받는 계층인 Controller를 구현한다.

이 단계의 목적은
비즈니스 로직을 완성하는 것이 아니라,
HTTP 요청이 서버 코드에 도달하고 응답이 돌아가는 흐름을 직접 체감하는 것이다.

---

## 1. Controller 계층의 역할

Controller는
외부 요청과 서버 내부 코드의 경계선이다.

역할은 명확하다.

* HTTP 요청 수신
* 요청 데이터 파싱
* Service 호출
* HTTP 응답 반환

Controller는 비즈니스 로직을 처리하지 않는다.

---

## 2. Controller 패키지 생성

`com.koreanit.spring` 아래에
controller 패키지를 생성한다.

```text
src/main/java/com/koreanit/spring/controller
```

---

## 3. HelloController 클래스 생성

다음 클래스를 생성한다.

```text
HelloController.java
```

```java
package com.koreanit.spring.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Server : " + LocalDateTime.now().toString();
    }
}
```

이 코드는 Spring Boot 서버에서 가장 단순한 Controller 구현이다.

* `/hello` 라는 주소로 요청이 들어오면
* `hello()` 메서드가 실행되고
* 문자열을 HTTP 응답으로 그대로 반환한다

---

#### `@RestController`

* 이 클래스가 HTTP 요청을 처리하는 Controller임을 의미한다
* 메서드의 반환값을 View(html)가 아닌 응답 데이터로 처리한다
* 문자열, 객체 모두 HTTP Response Body로 바로 전달된다

즉, `@RestController`에서는 `return = 응답 데이터`다.

---

#### `@GetMapping("/hello")`

* HTTP GET 요청을 처리한다
* 요청 경로는 `/hello`
* 이 요청이 들어오면 아래 메서드가 실행된다

Spring Boot는 이 URL ↔ 메서드 연결을 자동으로 처리한다.

---

#### `public String hello()`

* Controller 메서드는 반드시 `public`
* 반환 타입 `String`은 응답 데이터의 타입이다
* 메서드 이름은 중요하지 않다

  * URL과의 연결은 어노테이션이 담당한다

---

#### `return "Hello Spring Server";`

* 클라이언트에게 전달될 실제 응답 내용
* 브라우저에서는 화면에 그대로 출력된다

이 시점에서 중요한 것은 비즈니스 로직이 전혀 없다는 점이다.

Controller의 목적은 요청이 들어오고 응답이 나가는 흐름을 확인하는 것이다.

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

이 응답이 보이면 Controller가 정상적으로 동작하고 있다.

---

## 5. HTTP 요청과 메서드의 연결

```java
@GetMapping("/hello")
```

의미:

* HTTP GET 요청
* 경로: `/hello`
* 이 요청이 들어오면 `hello()` 메서드가 실행된다

---

## 6. JSON 응답 방식

문자열 대신 객체를 반환하는 메서드를 추가한다.

```java
import java.util.HashMap;
import java.util.Map;

@GetMapping("/hello/json")
public Map<String, String> helloJson() {
    Map<String, String> result = new HashMap<>();
    result.put("message", "Hello JSON");
    result.put("date", LocalDateTime.now().toString());
    return result;
}
```

응답:

```json
{
  "message": "Hello JSON"
}
```

`@RestController`는 반환값을 자동으로 JSON으로 변환한다.

---

## 7. 객체를 반환하면 `@RestController`가 자동으로 처리하는 일

`@RestController`가 붙은 Controller에서
메서드가 객체를 return 하면,
Spring Boot는 이 객체를 HTTP 응답(JSON)으로 자동 변환한다.

```java
return result;
```

이 한 줄 뒤에서 Spring이 다음 작업을 수행한다.

---

### 7-1. return 값을 View로 해석하지 않는다

일반적인 `@Controller`는 return 값을 뷰 이름으로 해석한다.

하지만 `@RestController`는 다르다.

* return 값 = HTTP Response Body
* 화면(View)을 찾지 않는다
* 문자열, 객체 모두 응답 데이터로 처리한다

---

### 7-2. 객체를 JSON으로 변환한다

```java
public Map<String, String> helloJson()
```

여기서 반환된 `Map` 객체는
Spring 내부에서 JSON 변환 과정을 거친다.

사용되는 구성 요소:

* HttpMessageConverter
* 기본 JSON 라이브러리: Jackson

처리 흐름:

```text
Controller return (객체)
        ↓
HttpMessageConverter 선택
        ↓
Jackson으로 JSON 직렬화
        ↓
HTTP Response Body에 작성
```

---

### 7-3. Content-Type 자동 설정

응답 헤더에 자동 포함된다.

```http
Content-Type: application/json;charset=UTF-8
```

Controller 코드에서 직접 설정하지 않았다.

---

### 7-4. 이 구조의 핵심 의미

* Controller는 객체만 생성해서 반환
* Spring Boot는 응답 형식(JSON)과 직렬화 책임을 전부 처리

그래서 Controller 코드는 단순하게 유지된다.

---

## 이 장의 핵심 정리

* Controller는 요청/응답 담당
* `@RestController`는 JSON 응답용
* Controller는 가볍게 유지한다
* 비즈니스 로직은 다음 단계(Service)로 이동한다


---

## 다음 단계

→ [Service 계층 구현](07-service_layer.md)
