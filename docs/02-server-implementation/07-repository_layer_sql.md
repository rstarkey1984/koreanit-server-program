# Repository 계층과 SQL

이 장에서는
Spring Boot 서버에서 **데이터 접근을 담당하는 Repository 계층의 역할**을 정의한다.

이 단계의 핵심은
SQL 문법을 배우는 것이 아니라,
**"SQL은 어디에 있어야 하는가"를 명확히 구분하는 것**이다.

---

## 강의 목표

* Repository 계층의 책임을 설명할 수 있다
* Service와 Repository의 역할 차이를 이해한다
* SQL이 서버 코드에서 위치해야 할 지점을 인식한다
* JDBC 기반 데이터 접근 흐름을 개념적으로 이해한다

---

## 1. 왜 Repository 계층이 필요한가

현재 구조는 다음과 같다.

```text
Controller → Service
```

여기서 Service가
DB에 직접 접근하기 시작하면 문제가 생긴다.

* Service가 SQL을 알게 된다
* 비즈니스 로직과 데이터 접근이 섞인다
* 테스트와 유지보수가 어려워진다

그래서 **데이터 접근만 전담하는 계층**이 필요하다.

---

## 2. Repository 계층의 역할

Repository는
**데이터를 조회하고 저장하는 책임만 가진다.**

Repository가 하는 일:

* SQL 실행
* DB 결과를 객체 형태로 반환

Repository가 하지 않는 일:

* 비즈니스 규칙 판단
* HTTP 요청/응답 처리

> SQL은 반드시 Repository 안에만 존재해야 한다.

---

## 3. Repository 패키지 생성

다음 패키지를 생성한다.

```text
src/main/java/com/koreanit/server/repository
```

---

## 4. Repository 클래스 뼈대 만들기

예제로 사용할 Repository 클래스를 생성한다.

```text
UserRepository.java
```

```java
package com.koreanit.server.repository;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

}
```

* `@Repository`:
  Spring이 이 클래스를 데이터 접근 컴포넌트로 인식

---

## 5. SQL은 "문자열"로 존재한다

Repository에서 사용하는 SQL은
자바 코드 안에서 **문자열 형태**로 존재한다.

```java
String sql = "SELECT 1";
```

이 단계에서는
이 SQL이 **무엇을 의미하는지**보다

> "아, 이 문자열이 DB로 전달되는구나"

라는 인식이 중요하다.

---

## 6. JDBC 흐름 맛보기 (개념)

Repository 내부에서 일어나는 흐름은 다음과 같다.

```text
Java 코드
  ↓
SQL 문자열
  ↓
JDBC
  ↓
MySQL
```

아직 실제로
DB에서 데이터를 조회하지는 않는다.

다음 단계에서
이 흐름을 코드로 연결한다.

---

## 7. Service ↔ Repository 관계 정리

Service는
Repository의 내부 구현을 모른다.

```text
Service → Repository → DB
```

Service가 아는 것은 오직 이것이다.

* "데이터를 요청하면 결과를 준다"

SQL이 어떻게 실행되는지는
Repository의 책임이다.

---

## 이 장의 핵심 정리

* Repository는 데이터 접근 전담 계층이다
* SQL은 Repository 안에만 존재한다
* Service는 SQL을 몰라야 한다
* 지금은 문법이 아니라 위치를 이해하는 단계다

---

## 다음 단계

→ [**데이터베이스 스키마 설계**](08-database_schema_design.md)
