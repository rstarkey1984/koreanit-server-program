# JDBC 커넥션 풀

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

> 이 과정에서 중요한 것은
> "SQL을 어떻게 보내는가"가 아니라
> "DB와 연결을 어떻게 관리하는가"다.

---

## 2. 커넥션(connection)이란

커넥션은
서버 프로그램과 DB 사이의 "연결된 통신 채널"이다.

커넥션을 만들려면 비용이 든다.

* 네트워크 연결
* 인증
* 세션 생성

요청이 올 때마다 커넥션을 새로 만들면
성능이 크게 떨어지고 DB에 부담이 간다.

---

## 3. 커넥션 풀(connection pool)이 필요한 이유

커넥션 풀은
미리 일정 개수의 커넥션을 만들어 두고,
필요할 때 빌려주고 반납받는 방식이다.

핵심 효과:

* 커넥션 생성 비용 감소
* 동시에 처리 가능한 요청 수 제어
* DB 과부하 방지

> 서버 프로그램에서 DB 연결은
> 한 번 연결해두고 효율적으로 재사용하는 것이 기본이다.

---

## 4. 준비: MySQL DB 접속 정보 확인

아래 정보가 준비되어 있어야 한다.

* host (예: localhost)
* port (예: 3306)
* database (예: server_program)
* username / password (서비스 계정)

---

## 5. Spring Boot 의존성 추가

이 프로젝트는 ORM(JPA)을 사용하지 않고,
SQL을 직접 작성하는 방식으로 진행한다.

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

## 6. DB 접속 설정 (application.yml)

Spring Boot는 기본적으로
HikariCP 커넥션 풀을 사용한다.

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/server_program?serverTimezone=Asia/Seoul&characterEncoding=utf8
    username: server_user
    password: server_pass123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

## 7. 이 장의 핵심 정리

* JDBC는 DB 접근 표준 통로다
* 커넥션은 비용이 크다
* 커넥션 풀은 커넥션을 재사용하기 위한 구조다
* Spring Boot는 datasource 설정으로 커넥션 풀을 관리한다

---

## 다음 단계

→ [**08. Repository 계층 설계**](08-repository_layer_design.md)
