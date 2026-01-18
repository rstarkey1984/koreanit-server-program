# 요청 / 응답 흐름 정리

이 장에서는
지금까지 나눠서 배운 모든 개념을 **하나의 흐름으로 연결**한다.

기능을 새로 만드는 장이 아니라,
**요청 하나가 어디서 시작해서 어디로 갔다가 어떻게 돌아오는지**를
처음부터 끝까지 따라가 보는 것이 목적이다.

---

## 강의 목표

* 브라우저 요청이 서버로 들어오는 흐름을 설명할 수 있다
* 서버 내부 계층(Controller / Service / Repository)의 역할을 연결해서 이해한다
* DB 접근과 트랜잭션이 언제 시작되고 끝나는지 인식한다
* 하나의 요청/응답 전체 여정을 그림으로 설명할 수 있다

---

## 1. 요청은 어디서 시작되는가

모든 요청은 **브라우저**에서 시작된다.

```text
브라우저
  ↓ HTTP 요청
```

사용자가 주소를 입력하거나
버튼을 클릭하는 순간,
HTTP 요청이 서버로 전송된다.

---

## 2. 웹 서버를 거쳐 WAS로 전달

요청은 가장 먼저 **웹 서버(Nginx)** 에 도착한다.

```text
브라우저
  ↓
Nginx (Web Server)
```

웹 서버의 역할:

* 요청 수신
* 정적 파일 처리
* WAS로 요청 전달

동적 요청은
**Spring Boot(WAS)** 로 전달된다.

---

## 3. Controller: 서버의 입구

Spring Boot로 전달된 요청은
**Controller**에서 처음으로 처리된다.

```text
Controller
```

Controller의 역할:

* HTTP 요청 수신
* 요청 데이터 파싱
* Service 호출

> Controller는 비즈니스 로직을 처리하지 않는다.

---

## 4. Service: 흐름과 트랜잭션의 중심

Controller는 요청을
**Service**로 전달한다.

```text
Service
```

Service의 역할:

* 비즈니스 로직 처리
* 여러 Repository 호출 조합
* 트랜잭션 시작과 종료

이 시점에서
트랜잭션이 시작된다.

---

## 5. Repository: 데이터 접근 전담

Service는
데이터가 필요하면 **Repository**를 호출한다.

```text
Repository
```

Repository의 역할:

* SQL 실행
* DB 결과 조회

Repository는
SQL만 알고,
비즈니스 의미는 알지 못한다.

---

## 6. DB와의 실제 통신

Repository의 SQL은
JDBC를 통해 DB로 전달된다.

```text
Repository
  ↓
JDBC
  ↓
Connection Pool
  ↓
MySQL
```

DB는
SQL을 실행하고 결과를 반환한다.

---

## 7. 결과 반환과 트랜잭션 종료

DB 결과는
다음 순서로 되돌아온다.

```text
DB
 ↑
Repository
 ↑
Service
```

Service에서:

* 모든 작업이 성공하면 → Commit
* 하나라도 실패하면 → Rollback

---

## 8. 응답 생성과 반환

Service의 결과는
Controller로 전달된다.

Controller는
이를 HTTP 응답으로 변환한다.

```text
Controller
  ↓ HTTP 응답
브라우저
```

사용자는
이 시점에서 결과를 보게 된다.

---

## 9. 전체 흐름 한 번에 보기

```text
브라우저
  ↓
Nginx
  ↓
Controller
  ↓
Service (트랜잭션 시작)
  ↓
Repository
  ↓
DB
  ↑
Repository
  ↑
Service (Commit / Rollback)
  ↑
Controller
  ↑
브라우저
```

---

## 이 장의 핵심 정리

* 요청은 브라우저에서 시작한다
* 서버 내부에는 명확한 계층 흐름이 있다
* DB 접근은 Repository에서만 이루어진다
* 트랜잭션은 Service에서 관리된다
* 응답은 다시 브라우저로 돌아간다

---

## 다음 단계

→ [**CRUD API 설계 및 구현**](12-crud_api_design.md)

