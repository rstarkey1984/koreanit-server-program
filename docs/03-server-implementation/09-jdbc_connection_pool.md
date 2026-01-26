# 데이터베이스 연결(JDBC 커넥션 풀)

이 장에서는
Spring Boot 서버가 MySQL DB에 접속하기 위한 **기본 연결 설정**을 구성한다.

중요한 목표는
"DB 연결이 된다"가 아니라,
서버가 DB와 통신할 때 왜 **커넥션 풀(connection pool)** 이 필요한지 이해하는 것이다.

---

## 1. JDBC란 무엇인가

JDBC(Java Database Connectivity)는
Java에서 DB에 접근하기 위한 표준 인터페이스다.

서버 프로그램 입장에서는

* SQL을 DB에 전달하고
* 결과를 받아서
* 애플리케이션 로직에 활용하는

기본 통로가 된다.

---

## 2. 커넥션(connection)이란

커넥션은
서버 프로그램과 DB 사이의 **연결된 통신 채널**이다.

커넥션을 생성하는 과정에는 비용이 든다.

* 네트워크 연결
* 사용자 인증
* DB 세션 생성

요청이 올 때마다 커넥션을 새로 만들면
성능이 급격히 저하되고 DB에 과부하가 걸린다.

---

## 3. 커넥션 풀(connection pool)이 필요한 이유

커넥션 풀은
미리 일정 개수의 커넥션을 생성해 두고,
필요할 때 빌려주고 사용이 끝나면 다시 돌려받는 구조다.

이 구조를 사용하는 이유는 다음과 같다.

* 커넥션 생성 비용 감소
* 동시에 처리 가능한 요청 수 제어
* DB 서버 과부하 방지

---

## 4. 커넥션 풀은 누가 관리하는가

결론부터 정리하면,

* **Spring Boot는 커넥션 풀을 직접 관리하지 않는다**
* Spring Boot는 커넥션 풀 구현체를 선택하고 설정만 해준다

실제 커넥션의 생성·대여·반납·정리는
**커넥션 풀 라이브러리**가 담당한다.

---

### 4-1. Spring Boot의 역할

Spring Boot의 역할은 다음으로 한정된다.

* application.yml 설정 로딩
* DataSource 객체 생성
* 커넥션 풀 라이브러리 초기화
* DataSource를 Bean으로 등록하여 전역 주입

즉,

> 커넥션을 직접 관리하는 주체가 아니라
> **커넥션 풀을 사용할 수 있게 만드는 관리자**다.

---

### 4-2. DataSource의 역할

`DataSource`는
DB 연결을 제공하기 위한 **표준 인터페이스**다.

```java
public interface DataSource {
    Connection getConnection();
}
```

중요한 특징:

* Controller, Service, Repository는
  커넥션 풀이 있는지 알 필요가 없다
* 단순히 `getConnection()`만 호출한다

구현체가 무엇인지(HikariCP 등)는
애플리케이션 코드에서 완전히 숨겨진다.

---

### 4-3. 실제 커넥션 풀 관리자: HikariCP

Spring Boot는 기본적으로 **HikariCP**를 사용한다.

HikariCP가 담당하는 작업은 다음과 같다.

* 서버 시작 시 커넥션 미리 생성
* 내부 풀에 커넥션 보관
* 요청 시 커넥션 대여
* 사용 종료 시 커넥션 회수
* 유효하지 않은 커넥션 정리
* 최대 커넥션 수 제한

즉,

> **커넥션의 생명주기 전체를 관리하는 주체**가 HikariCP다.

---

### 4-4. 요청 하나의 실제 흐름

```text
HTTP 요청
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
DataSource.getConnection()
 ↓
(HikariCP에서 커넥션 하나 대여)
 ↓
SQL 실행
 ↓
Connection.close()
 ↓
(HikariCP로 커넥션 반납)
```

중요한 점:

* `Connection.close()`는 연결 종료가 아니다
* 실제로는 **커넥션을 풀로 되돌려주는 동작**이다

---

## 5. 준비: MySQL DB 접속 정보 확인

아래 정보가 준비되어 있어야 한다.

* host (예: localhost)
* port (예: 3306)
* database (예: koreanit_service)
* username / password (서비스 계정)

---

## 6. Spring Boot 의존성 추가

필요한 의존성은 다음과 같다.

* JDBC
* MySQL Driver

### Gradle

```gradle
dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-jdbc'
  runtimeOnly 'com.mysql:mysql-connector-j'
}
```

---

## 7. DB 접속 설정 (application.yml)

Spring Boot는 기본적으로
HikariCP 커넥션 풀을 사용한다.

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/koreanit_service?serverTimezone=Asia/Seoul&characterEncoding=utf8
    username: koreanit_app
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

## 이 장의 핵심 정리

* JDBC는 DB 접근을 위한 표준 통로다
* 커넥션 생성 비용은 매우 크다
* 커넥션 풀은 커넥션을 재사용하기 위한 구조다
* Spring Boot는 커넥션 풀을 직접 관리하지 않는다
* 실제 커넥션 관리는 HikariCP가 담당한다
* `close()`는 종료가 아니라 반납이다

---

## 다음 단계

→ [**jdbcTemplate 사용법**](10-jdbc_template.md)
