# Controller–Service–Repository–JdbcTemplate 연결 구조

이 장에서는
이전 장에서 준비한 **JdbcTemplate**을
Repository 계층에서 실제로 주입받아 사용하는 구조를 완성한다.

이 단계의 목표는 SQL을 많이 작성하는 것이 아니라,

> **JdbcTemplate이 어디에 위치해야 하는지와
> 어떤 방식으로 사용되는지를 구조적으로 이해하는 것**이다.

---

## 1. 이 장의 전제

다음 조건이 이미 준비되어 있어야 한다.

* DataSource 설정 완료
* JDBC 커넥션 풀 동작 이해
* JdbcTemplate Bean 자동 생성 이해

즉,

> JdbcTemplate은 이미 **Spring이 만들어 둔 상태**이며,
> 우리는 그것을 **주입받아 사용**하기만 하면 된다.

---

## 2. Repository 계층의 위치 다시 확인

JdbcTemplate은
**Repository 계층에서만 사용**한다.

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

이 규칙은 이후 모든 실습에서 유지된다.

---

## 3. Repository 패키지 생성

다음 패키지를 생성한다.

```text
src/main/java/com/koreanit/spring/repository
```

---

## 4. Repository 클래스 생성

다음 클래스를 생성한다.

```text
UserRepository.java
```

```java
package com.koreanit.spring.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

### 핵심 포인트

* `@Repository` → Spring Bean 등록
* `JdbcTemplate`을 `new`로 생성하지 않는다
* 생성자 주입으로 JdbcTemplate을 전달받는다

즉,

> Repository는 **이미 준비된 도구를 받아 사용하는 입장**이다.

---

## 5. 가장 단순한 SQL 실행 예제

DB 연결이 정상인지 확인하기 위해
가장 단순한 쿼리를 실행해본다.

```java
public Integer selectOne() {
  return jdbcTemplate.queryForObject("SELECT id from users limit 1", Integer.class);
}
```

이 메서드는

* DB에 SQL이 전달되고
* 결과가 정상적으로 반환되는지

를 확인하는 용도다.

---

## 6. Service에서 Repository 사용하기

이제 Repository를
Service에서 주입받아 호출한다.

```java
package com.koreanit.spring.service;

import com.koreanit.spring.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Integer checkConnection() {
        return userRepository.selectOne();
    }
}
```

Service의 역할은 변하지 않는다.

* SQL을 모른다
* JdbcTemplate을 모른다
* Repository 결과를 받아 의미 있는 흐름을 만든다

---

## 7. Controller에서 Service 호출

```java
package com.koreanit.spring.controller;

import com.koreanit.spring.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/db-check")
    public Integer dbCheck() {
        return userService.checkConnection();
    }
}
```

---

## 전체 흐름 정리

요청 하나의 흐름은 다음과 같다.

```text
HTTP 요청
 → Controller
 → Service
 → Repository
 → JdbcTemplate
 → Database
```

각 계층은
**자기 역할만 수행**한다.

---

## 이 장의 핵심 정리

* JdbcTemplate은 Repository에서만 사용한다
* JdbcTemplate은 Spring이 생성한다
* Repository는 데이터 접근만 담당한다
* 구조가 먼저이고 SQL은 나중이다

---

## 다음 단계

→ [**01. 공통 응답 포맷**](/docs/03-common-modules/01-common_response_format.md)