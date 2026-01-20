# Repository 계층 설계

이 장에서는  
Spring Boot 서버에서 **Repository 계층을 어떻게 설계해야 하는지**를 다룬다.

이 단계의 핵심은  
SQL을 작성하는 것이 아니라,  
**“Repository는 어떤 책임을 가져야 하는가”를 명확히 정하는 것**이다.


---

## 1. Repository 계층은 왜 필요한가

서버 애플리케이션은  
다음과 같은 서로 다른 관심사를 가진 코드로 구성된다.

* HTTP 요청/응답 처리
* 비즈니스 로직 처리
* 데이터 조회 및 저장

이 중 **데이터 접근 로직을 분리하기 위해**  
Repository 계층이 존재한다.

Repository가 없으면:

* Service가 SQL을 직접 알게 되고
* 로직과 데이터 접근이 섞이며
* 구조가 빠르게 무너진다

---

## 2. Repository 계층의 책임

Repository는  
**데이터 접근만 전담하는 계층**이다.

### Repository가 하는 일

* SQL 실행
* DB 결과를 자바 객체로 변환
* 데이터 조회 / 저장

### Repository가 하지 않는 일

* 비즈니스 규칙 판단
* 조건 분기(if)
* 트랜잭션 제어
* HTTP 요청/응답 처리

> Repository는  
> **“데이터를 어떻게 가져오는지”만 알고,  
> “왜 가져오는지”는 모른다.**

---

## 3. Service와 Repository의 관계

Service와 Repository는  
**역할이 명확히 다른 계층**이다.

```text
Service
  - 흐름 제어
  - 조건 판단
  - 여러 Repository 호출
  - 트랜잭션 경계

Repository
  - SQL 실행
  - 데이터 반환
```

Service는  
Repository가 반환한 결과를 기반으로  
**의미 있는 판단을 수행**한다.

---

## 4. Repository 메서드 설계 기준

Repository 메서드는  
**SQL 단위로 설계하는 것이 원칙**이다.

### 기본 원칙

* SQL 1개 = 메서드 1개
* 메서드 내부에 로직 없음
* 결과만 반환

### 예시

```text
ping()
countUsers()
findById()
existsByEmail()
```

---

## 5. Repository 메서드 이름 규칙

Repository 메서드 이름에는  
**비즈니스 의미를 담지 않는다.**

### ❌ 잘못된 예

```text
checkUserLogin()
processSignup()
validateUser()
```

### ⭕ 올바른 예

```text
findByEmail()
insertUser()
countByEmail()
```

---

## 6. 반환 타입 설계 기준

Repository는  
**판단하지 않고 결과만 반환**한다.

| SQL 성격 | 반환 타입 예 |
|----|----|
| COUNT | int |
| 존재 여부 | boolean 또는 count 결과 |
| 단건 조회 | 객체 또는 null |
| 목록 조회 | List |

---

## 7. Repository 클래스 생성

`HelloRepository` 클래스를 생성한다.

```java
package com.koreanit.server.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HelloRepository {

    private final JdbcTemplate jdbcTemplate;

    public HelloRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer ping() {
        String sql = "SELECT 1";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
```

- JdbcTemplate는
DataSource 위에서 JDBC 반복 코드를 제거하고, Repository가 SQL 실행 책임만 가지도록 해주는 도구다.

## 8. 이 장의 핵심 정리

* Repository는 데이터 접근 전담 계층이다
* SQL은 반드시 Repository 안에만 존재한다
* Repository는 판단하지 않는다
* 메서드는 SQL 단위로 설계한다
* Service와 Repository는 계약 관계다

---

## 다음 단계

→ [**09. Controller → Service → Repository 흐름 실습**](09-controller_service_repository_flow.md)
