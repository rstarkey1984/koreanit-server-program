# API 서버 시작 – 유저 조회 API (GET 리스트 / 단건)

이 장에서 목표는 **Controller → Service → Repository** 흐름을 한 번에 연결하는 것이다.

---

## 1. 구현할 API

```http
GET /users
GET /users/{id}
```

---

## 2. 조회 전용 DTO

조회 응답은 테이블 구조를 그대로 내리지 않는다.
**응답 계약을 고정하고, 민감 정보가 내려가는 실수를 막기 위해서**다.

```java
package com.koreanit.spring.dto.user;

public class UserResponse {

    private final int id;
    private final String username;
    private final String nickname;

    public UserResponse(int id, String username, String nickname) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
}
```

---

## 3. Repository

Repository는 DB 접근 전용 계층이다.

### Repository가 하는 일

* SQL 실행
* 조회 결과를 DTO로 매핑

### Repository가 하면 안 되는 일

* HTTP 응답 형식 결정
* 비즈니스 규칙 판단
* 요청 파라미터 검증

---

### 3-1. findAll()

```java
package com.koreanit.spring.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.koreanit.spring.dto.user.UserResponse;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserResponse> findAll() {
        String sql = "SELECT id, username, nickname FROM users ORDER BY id DESC limit 100";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new UserResponse(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("nickname")
            )
        );
    }
}
```

### RowMapper 란?
```java
(rs, rowNum) ->
  new UserResponse(
      rs.getInt("id"),
      rs.getString("username"),
      rs.getString("nickname")
  )
```

> RowMapper는 **DB 조회 결과 한 줄(row)을 자바 객체 하나로 변환하는 규칙**이다.

`jdbcTemplate.query` 실행시 내부 흐름은 다음과 같다.

1. SQL 실행 → ResultSet 생성
2. ResultSet을 한 줄씩 이동
3. 각 줄마다 RowMapper 호출
4. RowMapper가 반환한 객체를 List로 수집

---

### ResultSet을 엑셀 표로 보면

DB 조회 결과(ResultSet)는 개념적으로 아래와 같은 **엑셀 표 형태**라고 생각하면 된다.

```text
| row | id | username | nickname |
|-----|----|----------|----------|
|  1  |  1 | hong     | 홍길동   |
|  2  |  2 | kim      | 김철수   |
|  3  |  3 | lee      | 이영희   |
```

* 컬럼명: `id`, `username`, `nickname`
* 한 줄(row)이 사용자 한 명의 데이터

ResultSet은 이 표를 **위에서 아래로 한 줄씩 읽는 구조**다.

```java
rs.getInt("id");
rs.getString("username");
rs.getString("nickname");
```

이 시점에서는 아직 자바 객체가 아니다.
단순히 DB 값을 읽고 있는 단계다.

---

### RowMapper 구조

RowMapper 인터페이스 구조는 아래와 같다.

```java
public interface RowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
```

* `rs` : 현재 줄(row)의 DB 값 묶음
* `rowNum` : 몇 번째 줄인지 나타내는 번호

RowMapper의 역할은 딱 하나다.

> **현재 줄(row) 하나를 자바 객체 하나로 바꿔서 반환**

그래서 Repository에서는 람다식으로 줄인 아래 코드가 가능해진다.

> 람다식이란? 메서드 하나짜리 인터페이스 구현을 짧게 쓰는 문법

```java
jdbcTemplate.query(sql, (rs, rowNum) ->
    new UserResponse(
        rs.getInt("id"),
        rs.getString("username"),
        rs.getString("nickname")
    )
);
```

JdbcTemplate이 여러 줄을 돌면서
이 RowMapper를 반복 호출하고,
그 결과를 `List<UserResponse>`로 만들어 준다.



---

### 3-2. findById()

```java
public UserResponse findById(int id) {
    String sql = "SELECT id, username, nickname FROM users WHERE id = ?";

    return jdbcTemplate.queryForObject(
        sql,
        (rs, rowNum) -> new UserResponse(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("nickname")
        ),
        id
    );
}
```

* `queryForObject()`는 단건 조회용이다.
* 결과가 0건이면 예외가 발생할 수 있다.
* 이 예외는 다음 단계에서 공통 예외 처리로 묶는다.

---

## 4. Service

Service는 조회 흐름을 제어한다.

```java
public List<UserResponse> getUsers() {
    return userRepository.findAll();
}

public UserResponse getUser(int id) {
    return userRepository.findById(id);
}
```

---

## 5. Controller

```java
@GetMapping("/api/v1/users")
public List<UserResponse> getUsers() {
    return userService.getUsers();
}

@GetMapping("/api/v1/users/{id}")
public UserResponse getUser(@PathVariable int id) {
    return userService.getUser(id);
}
```

---

## 요청 테스트

```
http://localhost:8080/api/v1/users

http://localhost:8080/api/v1/users/1
```

---

## 체크 포인트

* GET 요청만 사용했는가
* SELECT SQL만 사용했는가
* 민감 정보가 내려가지 않는가
* 계층 간 역할이 섞이지 않았는가

---

## 다음 단계

→ [**회원가입 API (개요 & DTO)**](01-api-overview-dto.md)