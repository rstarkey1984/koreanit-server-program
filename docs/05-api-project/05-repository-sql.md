# 05. Repository SQL 구현 (count / insert)

이 문서에서는 `UserRepository`의 메서드 중
회원가입에 필요한 **조회(count)** 와 **저장(insert)** SQL을 구현한다.

핵심 목표:

* Repository는 **SQL 실행과 결과 반환만** 담당한다
* 비즈니스 판단(if, 예외)은 **절대 넣지 않는다**
* `JdbcTemplate` 사용 패턴을 명확히 고정한다

---

## 1. 전제 조건

이미 준비된 것:

* `JdbcTemplate` Bean 자동 생성
* MySQL 연결 설정 완료
* Service 계층에서 다음 메서드를 호출 중

  * `countByUsername(String username)`
  * `insertUser(String username, String password, String nickname, String email)`

---

## 2. Repository 책임 재확인

### Repository는 다음만 한다.

* SQL 작성
* 파라미터 바인딩
* DB 결과를 **값으로 반환**

### Repository가 하지 않는 것:

* 중복 여부 판단
* 예외를 비즈니스 의미로 변환
* HTTP / ApiResponse 처리

---

## 3. username 중복 개수 조회

### 메서드 시그니처

```java
public int countByUsername(String username)
```

### SQL

```sql
SELECT COUNT(*)
FROM users
WHERE username = ?
```

### 구현 코드 (메서드)

```java
public int countByUsername(String username) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM users WHERE username = ?",
        Integer.class,
        username
    );

    return count == null ? 0 : count;
}
```

* `queryForObject` 사용 (결과 1건 보장)
* `null` 방어 처리 (IDE 경고 및 안정성)
* 판단 로직 없음 (단순 반환)

---

## 4. 사용자 저장 (INSERT)

### 메서드 시그니처

```java
public long insertUser(String username,
                       String password,
                       String nickname,
                       String email)
```

### SQL

```sql
INSERT INTO users (username, password, nickname, email)
VALUES (?, ?, ?, ?)
```

### 구현 코드 (메서드)

```java
public long insertUser(String username,
                       String password,
                       String nickname,
                       String email) {

    jdbcTemplate.update(
        "INSERT INTO users (username, password, nickname, email) VALUES (?, ?, ?, ?)",
        username,
        password,
        nickname,
        email
    );

    // 다음 단계에서 생성된 PK 반환 방식으로 개선
    return 1L;
}
```

* `update()` 사용 (INSERT / UPDATE / DELETE 공통)
* 영향받은 row 수 대신, 지금은 흐름 연결이 목적
* 생성된 PK 반환은 다음 단계에서 다룸

---

## 체크 포인트

* SQL이 Repository에만 존재하는가
* Service에 SQL 문자열이 없는가
* if / 예외 / 비즈니스 판단이 없는가

---

## 다음 단계

[**06. Repository – 생성된 PK 반환**](06-repository-keyholder.md)
