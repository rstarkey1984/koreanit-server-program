# Controller 요청 데이터 바인딩 실습 (TestController)

이 장에서는 Controller가 **HTTP 요청에서 데이터를 꺼내는 방법**을 실습한다.

목표는 비즈니스 로직 구현이 아니라,

* 요청 데이터가 어디로 들어오고(위치)
* 어떤 어노테이션으로 바인딩되고(방법)
* 어떤 응답으로 되돌아오는지(결과)

이 흐름을 확인하는 것이다.

---

## 0. 이 장의 범위

이 장에서는 아래 범위만 다룬다.

* Query String → `@RequestParam`
* URL 경로 값 → `@PathVariable`
* JSON Body → `@RequestBody`
* Header → `@RequestHeader`

하지 않는 것

* Service/Repository
* DB 접근
* 권한/정책 판단
* 트랜잭션

---

## 1. TestController 하나로 묶는 이유

이 장은 리소스 설계(users 등)를 다루는 단계가 아니라,
**요청 데이터 바인딩 메커니즘**을 확인하는 단계다.

따라서 엔드포인트 의미를 최소화하기 위해
`TestController` 하나에서 각각의 요청을 분리해 다룬다.

* 기능 의미보다 요청 데이터가 들어오는 위치에 집중한다

---

## 2. 파일 / 패키지

생성 경로

```text
src/main/java/com/koreanit/spring/controller
```

파일

```text
TestController.java
TestBodyRequest.java
```

---

## 3. TestController 단계별 구현

이 장에서는 Controller 전체 코드를 한 번에 제시하지 않는다.
요청 데이터가 **어디에서 어떻게 바인딩되는지**를 확인하기 위해,
메서드를 단계별로 추가한다.

---

### 3-1. Controller 뼈대 만들기

`TestController.java`

```java
package com.koreanit.spring.controller;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
}
```

요약

* `@RestController` : 반환값을 HTTP 응답 본문으로 보낸다
* 클래스 레벨 경로 매핑은 사용하지 않는다

---

### 3-2. Query String 받기: `@RequestParam`

```java
@GetMapping("/test/param")
public Map<String, Object> param(@RequestParam String msg) {
    Map<String, Object> result = new HashMap<>();
    result.put("msg", msg);
    return result;
}
```

요약

* 요청: `GET /test/param?msg=hello`
* Query String 값이 메서드 파라미터로 바인딩된다

REST Client

```http
GET http://localhost:8080/test/param?msg=hello
```

---

### 3-3. 기본값: `defaultValue`

```java
@GetMapping("/test/page")
public Map<String, Object> page(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
) {
    Map<String, Object> result = new HashMap<>();
    result.put("page", page);
    result.put("size", size);
    return result;
}
```

요약

* 요청 파라미터가 없으면 기본값이 사용된다
* 타입 변환은 Controller 바인딩 단계에서 처리된다

REST Client (기본값)

```http
GET http://localhost:8080/test/page
```

REST Client (값 지정)

```http
GET http://localhost:8080/test/page?page=2&size=20
```

---

### 3-4. 선택 값: `required=false`

```java
@GetMapping("/test/optional")
public Map<String, Object> optional(
        @RequestParam(required = false) String keyword
) {
    Map<String, Object> result = new HashMap<>();
    result.put("keyword", keyword);
    return result;
}
```

요약

* 파라미터가 없어도 요청은 정상 처리된다
* 값이 없으면 `null`로 바인딩된다

REST Client (없음)

```http
GET http://localhost:8080/test/optional
```

REST Client (있음)

```http
GET http://localhost:8080/test/optional?keyword=spring
```

---

### 3-5. URL 경로 값 받기: `@PathVariable`

```java
@GetMapping("/test/path/{value}")
public Map<String, Object> path(@PathVariable int value) {
    Map<String, Object> result = new HashMap<>();
    result.put("value", value);
    return result;
}
```

요약

* URL 경로의 일부가 메서드 파라미터로 바인딩된다
* 주로 식별자(id 등)에 사용된다

REST Client

```http
GET http://localhost:8080/test/path/10
```

---

### 3-6. Header 값 받기: `@RequestHeader`

```java
@GetMapping("/test/header")
public Map<String, Object> header(
        @RequestHeader(required = false, name = "User-Agent") String userAgent
) {
    Map<String, Object> result = new HashMap<>();
    result.put("userAgent", userAgent);
    return result;
}
```

요약

* HTTP Header 값을 직접 읽는다
* 없을 경우를 대비해 `required=false`로 처리한다

REST Client

```http
GET http://localhost:8080/test/header
```

---

### 3-7. JSON Body 받기(Map): `@RequestBody`

```java
@PostMapping("/test/body")
public Map<String, Object> body(@RequestBody Map<String, Object> body) {
    Map<String, Object> result = new HashMap<>();
    result.put("received", body);
    return result;
}
```

요약

* 요청 본문의 JSON 전체가 Map으로 바인딩된다
* 구조가 유동적인 테스트에 적합하다

REST Client

```http
POST http://localhost:8080/test/body
Content-Type: application/json

{
  "a": 1,
  "b": "text"
}
```

---

### 3-8. JSON Body 받기(DTO): `@RequestBody`

`TestBodyRequest.java`

```java
package com.koreanit.spring.controller;

public class TestBodyRequest {
    private String a;
    private int b;

    public String getA() { return a; }
    public int getB() { return b; }
}
```

```java
@PostMapping("/test/body-dto")
public Map<String, Object> bodyDto(@RequestBody TestBodyRequest req) {
    Map<String, Object> result = new HashMap<>();
    result.put("a", req.getA());
    result.put("b", req.getB());
    return result;
}
```

요약

* JSON 필드명이 DTO 필드에 매핑된다
* 실제 서비스 코드에서 가장 일반적인 방식이다

REST Client

```http
POST http://localhost:8080/test/body-dto
Content-Type: application/json

{
  "a": "hello",
  "b": 3
}
```

---

## 이 장의 핵심 정리

* 요청 데이터는 위치가 정해져 있다

  * Query String → `@RequestParam`
  * Path → `@PathVariable`
  * JSON Body → `@RequestBody`
  * Header → `@RequestHeader`

* Controller는 **요청을 받아 파라미터로 바인딩하는 단계**까지만 맡는다

---

## 다음 단계

[**Controller 계층 구현**](06-controller_layer.md)