# 정적 페이지와 동적 페이지

이 장에서는 웹 서버와 WAS에서 가장 기본이 되는 개념인 **정적 페이지**와 **동적 페이지**의 차이를 정리한다.
Spring Boot 서버를 이해하기 위해 반드시 먼저 구분해야 하는 개념이다.

---

## 1. 정적 페이지 (Static Page)

### 1-1. 정의

정적 페이지란 **서버가 파일을 그대로 전달하는 페이지**를 의미한다.

* 요청이 와도 서버에서 로직을 실행하지 않는다
* 파일 내용은 항상 동일하다
* 서버는 파일을 찾아 그대로 응답한다

---

### 1-2. 동작 방식

```text
브라우저
  ↓  GET /index.html
서버
  ↓
파일 그대로 응답
```

서버는 다음 작업만 수행한다.

* 요청 경로에 해당하는 파일 탐색
* 파일 내용을 그대로 전송

---

### 1-3. 정적 파일 예시

* HTML
* CSS
* JavaScript
* 이미지 파일

```text
index.html
style.css
app.js
logo.png
```

---

### 1-4. Spring Boot에서의 위치

```text
src/main/resources/static/
```

특징:

* Controller 없이 바로 응답된다
* `index.html`이 존재하면 `GET /` 요청에 자동 응답
* 서버 로직을 거치지 않는다

---

### 1-5. 정적 페이지 요약

* 서버 로직 없음
* 빠른 응답
* 파일 서버 역할
* 항상 같은 결과

---

## 2. 동적 페이지 (Dynamic Page)

### 2-1. 정의

동적 페이지란 **요청이 올 때마다 서버에서 로직을 실행해 결과를 만들어 응답하는 페이지**를 의미한다.

* 요청마다 결과가 달라질 수 있다
* 서버 코드 실행이 필수
* 비즈니스 로직이 개입된다

---

### 2-2. 동작 방식

```text
브라우저
  ↓  GET /users/1
서버
  ↓
Controller 실행
  ↓
Service 로직 처리
  ↓
(필요 시 DB 조회)
  ↓
결과 생성 후 응답
```

---

### 2-3. 동적 처리 예시

* 사용자 정보 조회
* 게시글 목록
* 로그인 처리
* API(JSON) 응답

---

### 2-4. Spring Boot에서의 위치

```text
src/main/java/
 └── controller/
 └── service/
 └── repository/
```

Controller 예시:

```java
@GetMapping("/hello")
public String hello() {
    return LocalDateTime.now().toString(); 
}
```

---

### 2-5. 동적 페이지 요약

* 서버 로직 실행
* 요청마다 결과 변경 가능
* DB, 인증, 계산 처리 가능
* 애플리케이션 서버 역할

---

## 3. 정적 페이지 vs 동적 페이지 비교

| 구분             | 정적 페이지           | 동적 페이지               |
| -------------- | ---------------- | -------------------- |
| 서버 로직          | 없음               | 있음                   |
| 응답 내용          | 항상 동일            | 요청마다 달라질 수 있음        |
| 처리 주체          | 파일 시스템           | Controller / Service |
| Spring Boot 위치 | resources/static | java/controller      |
| Controller 필요  | X                | O                    |
| 사용 목적          | 안내, 리소스 제공       | API, 비즈니스 처리         |

---

## 4. Spring Boot 처리 우선순위

중요한 규칙 하나:

> **같은 URL 경로가 있을 경우 정적 리소스가 Controller보다 먼저 처리된다**

동작 순서:

1. `static/` 경로에서 파일 탐색
2. 파일이 존재하면 즉시 응답
3. 없을 경우 Controller 탐색


---

## 한 줄 요약

> 정적 페이지는 **파일을 그대로 응답**하는 것이고,
> 동적 페이지는 **서버 코드를 실행한 결과를 응답**하는 것이다.

---

## 다음 단계

→ [**자바 → 스프링부트 개념 맵**](/docs/02-server-implementation/00-java_springboot.md)