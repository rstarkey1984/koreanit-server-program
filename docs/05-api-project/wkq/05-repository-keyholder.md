# Repository – 생성된 PK 반환 (KeyHolder)

이 문서에서는 `insertUser()`가 **임시값(1L)** 을 반환하던 것을 제거하고,
DB가 생성한 **AUTO_INCREMENT PK(id)** 를 실제로 받아 반환하도록 개선한다.

핵심 목표:

* `JdbcTemplate.update()` 실행 결과로 **생성된 PK** 를 얻는다
* Repository는 여전히 **SQL 실행 + 값 반환만** 한다
* Service/Controller는 구현 변경 없이 그대로 동작한다

---

## 1. 전제

* `users.id` 는 `AUTO_INCREMENT` PK
* MySQL 사용
* `JdbcTemplate` 사용

---

## 2. 왜 PK 반환이 필요한가

회원가입 성공 응답에 다음처럼 `id`를 포함시키려면,
INSERT 후 생성된 PK를 알아야 한다.

```json
{
  "success": true,
  "message": "회원가입 완료",
  "data": {
    "id": 1,
    "username": "test01",
    "nickname": "테스트"
  },
  "code": null
}
```

---

## 3. insertUser() 구현 변경 (메서드만)

### 기존 문제

* `jdbcTemplate.update()`는 기본적으로 **영향받은 row 수(int)** 만 반환한다
* 그래서 `return 1L;` 같은 임시 반환을 쓰면 응답의 `id`가 의미가 없어짐

---

### 개선: KeyHolder로 생성된 키 받기

아래는 **Repository 메서드만** 기준 코드다.

```java
public long insertUser(String username,
                       String password,
                       String nickname,
                       String email) {

  KeyHolder keyHolder = new GeneratedKeyHolder();

  jdbcTemplate.update(con -> {
    PreparedStatement ps = con.prepareStatement(
        "INSERT INTO users (username, password, nickname, email) VALUES (?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS
    );

    ps.setString(1, username);
    ps.setString(2, password);
    ps.setString(3, nickname);
    ps.setString(4, email);

    return ps;
  }, keyHolder);

  Number key = keyHolder.getKey();
  if (key == null) {
    // PK를 못 받는 건 서버 내부 오류로 간주
    throw new IllegalStateException("생성된 사용자 ID를 가져올 수 없습니다");
  }

  return key.longValue();
}
```

필요 import

```java
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
```

* `PreparedStatement`를 직접 만들어 `RETURN_GENERATED_KEYS`를 지정한다
* `KeyHolder`에서 생성된 키를 꺼내 `long`으로 반환한다
* Repository는 여전히 **저장 + 결과값 반환**만 한다

---

## 체크 포인트

* Repository에 비즈니스 예외가 없는가
* Service에서 null 결과를 예외로 변환하는가
* 모든 실패가 `ApiResponse` JSON으로 내려오는가
* Service/Controller 코드를 바꾸지 않아도 응답의 `id`가 정상으로 내려오는가
* 중복/검증 실패는 여전히 Service에서 `ApiException`으로 처리되는가

---

## 다음 단계

[**비밀번호 해시 적용 (PasswordEncoder)**](06-password_encorder.md)