# JdbcTemplate 사용법

이 장에서는
Spring Boot에서 **JdbcTemplate을 사용해 SQL을 실행하는 기본 방법**을 익힌다.

이전 장에서 다룬 **DataSource / 커넥션 풀 구조**를 전제로,
"우리는 어디까지 신경 쓰고, 어디부터 프레임워크가 처리하는가"를 명확히 한다.

---

## 1. JdbcTemplate이란 무엇인가

JdbcTemplate은
JDBC 코드를 단순화하기 위한 **Spring 제공 유틸리티 클래스**다.

JdbcTemplate이 대신 처리해주는 작업은 다음과 같다.

* 커넥션 획득 / 반납
* PreparedStatement 생성
* 파라미터 바인딩
* 예외 변환 (SQLException → Spring 예외)
* 리소스 정리

즉,

> 우리는 **SQL과 결과 처리에만 집중**하면 된다.

---

## 2. JdbcTemplate은 누가 생성하는가

JdbcTemplate은
개발자가 `new`로 생성하지 않는다.

Spring Boot에서는:

* `DataSource` Bean이 존재하면
* Spring이 **JdbcTemplate Bean을 자동 생성**한다

즉,

```text
DataSource (커넥션 풀)  ← 우리가 설정
        ↓
JdbcTemplate            ← Spring이 생성
        ↓
Repository               ← 우리가 주입받아 사용
```

Repository는
**이미 준비된 JdbcTemplate을 주입받아 사용하는 입장**이다.

---

## 3. Repository에서 JdbcTemplate 사용 구조

JdbcTemplate은
**Repository 계층에서만 사용**한다.

Controller나 Service에서 직접 사용하지 않는다.

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
JdbcTemplate
   ↓
Database
```

이 구조의 핵심은:

* Service는 **SQL을 모른다**
* Repository는 **비즈니스 흐름을 모른다**
* JdbcTemplate은 **데이터 접근 도구일 뿐**이다

---

## 4. JdbcTemplate의 기본 사용 패턴

JdbcTemplate 사용은
항상 아래 패턴 중 하나로 귀결된다.

### 1) 조회 결과가 여러 건인 경우

```java
jdbcTemplate.query(...)
```

* SELECT
* 결과가 여러 행일 수 있음
* List 형태로 반환

---

### 2) 조회 결과가 한 건인 경우

```java
jdbcTemplate.queryForObject(...)
```

* SELECT
* 결과가 정확히 한 건
* 단일 객체 반환

---

### 3) 데이터 변경 (INSERT / UPDATE / DELETE)

```java
jdbcTemplate.update(...)
```

* INSERT
* UPDATE
* DELETE
* 영향받은 row 수 반환

---

## 5. JdbcTemplate을 쓰면서 우리가 책임지는 것

JdbcTemplate을 사용해도
**모든 것이 자동은 아니다**.

우리가 직접 책임지는 것은 다음뿐이다.

* SQL 작성
* SQL 파라미터 순서
* 결과를 어떤 객체로 받을지 결정

반대로,
**절대 신경 쓰지 않는 것**:

* 커넥션 생성/종료
* 트랜잭션 연결
* Statement/ResultSet close
* SQLException 처리 방식

---

## 6. 이 장의 핵심 정리

이 장에서 반드시 이해해야 할 한 줄은 이것이다.

> **JdbcTemplate은 DB 접근을 “쉽게” 만들어주지만
> 구조를 대신 설계해주지는 않는다.**

그래서:

* JdbcTemplate은 Repository에만 둔다
* Service는 JdbcTemplate을 모른다
* 구조가 먼저이고, SQL은 나중이다


## 다음 단계

→ [**09. Controller–Service–Repository–JdbcTemplate 연결 구조**](09-layer_connection_overview.md)