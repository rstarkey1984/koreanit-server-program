# 06. Authorization (403) – DB Role 테이블 기반

이 문서는 05단계(JWT Authentication, 401) 이후,
Users CRUD API에 **권한(Authorization)** 을 적용하는 기준 문서다.

이 단계의 목표는 다음 한 줄이다.

> **토큰이 유효해도, 권한이 없으면 403으로 막는다.**

---

## 1. 401 vs 403 기준 정리

* **401 UNAUTHORIZED**

  * 토큰 없음 / 무효 / 만료
  * 인증(Authentication) 실패

* **403 FORBIDDEN**

  * 토큰은 유효함
  * 권한(Authorization) 부족

이 문서에서는 **403 처리 기준**을 확정한다.

---

## 2. 권한 설계 원칙

### 2.1 역할(Role)은 DB에서 관리한다

역할 모델은 확장성을 고려해 **N:M 구조**로 설계한다.

* `users` : 사용자
* `roles` : 역할 (ROLE_USER, ROLE_ADMIN)
* `user_roles` : 사용자–역할 매핑

---

### 2.2 인가 책임 분리

* **SecurityConfig**

  * URL 단위 인가 규칙 (1차 차단)

* **Controller / Method**

  * 관리자 전용 행위 제어 (`@PreAuthorize`)

---

## 3. DB 스키마

### 3.1 roles 테이블

```sql
CREATE TABLE roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL COMMENT 'ROLE_USER, ROLE_ADMIN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 user_roles 테이블

```sql
CREATE TABLE user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4. 초기 데이터 (실습용)

아래 SQL은 **관리자 계정(admin / 1234)** 과 역할 데이터를 생성한다.

```sql
-- roles 기본 데이터
INSERT INTO roles(name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- 관리자 계정 (username: admin / password: 1234)
-- BCrypt("1234")
INSERT INTO users (username, email, password, nickname)
VALUES (
  'admin',
  'admin@test.com',
  '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFXz5v1p3m6HfK8rQk5pY9u1kzZ1Z7S6',
  '관리자'
);

-- ROLE_ADMIN 부여
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin';
```

---

## 5. CustomUserDetailsService – 역할 로딩

* username으로 사용자 조회
* user_id로 role 목록 조회
* role → `GrantedAuthority` 변환

```java
List<String> roleNames = userRepository.findRoleNamesByUserId(e.getId());
List<GrantedAuthority> authorities = roleNames.stream()
    .map(SimpleGrantedAuthority::new)
    .toList();
```

주의:

* `.hasRole("ADMIN")` 은 내부적으로 `ROLE_ADMIN`을 기대한다.
* DB에는 반드시 `ROLE_` 접두사가 포함되어야 한다.

---

## 6. 인가 정책 (확정)

### 6.1 공개 API

* `POST /api/users` (회원가입)
* `POST /api/login`

### 6.2 인증만 필요한 API

* `/api/users/me/**`

### 6.3 관리자만 가능한 API

* `GET /api/users`
* `GET /api/users/{id}`
* `PUT /api/users/{id}/nickname`
* `PUT /api/users/{id}/password`
* `DELETE /api/users/{id}`

---

## 7. SecurityConfig – URL 단위 인가

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/login").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

    // 본인 전용 (/me)
    .requestMatchers("/api/users/me/**").authenticated()

    // 관리자 전용 
    .requestMatchers("/api/**").hasRole("ADMIN")

    // 그 외
    .anyRequest().authenticated()
)
```

---

## 8. Controller 정책 요약

* 사용자 본인 수정/삭제

  * `/api/users/me/**`

* 관리자만 가능한 행위

  * `{id}` 기반 수정/삭제

이 정책으로 Controller는 **역할 판단 로직을 가지지 않는다**.

---

## 9. REST Client 실습 체크

* 토큰 없음 → 401
* `/api/users/me/**`

  * ROLE_USER / ROLE_ADMIN 모두 가능
* `{id}` 기반 수정/삭제

  * ROLE_USER → 403
  * ROLE_ADMIN → 200

---

## 10. 이 단계가 끝나면

* 인증(401)과 인가(403)가 명확히 분리된다
* 역할은 DB에서 관리된다
* 이후 단계에서 다음을 안정적으로 추가할 수 있다

  * Refresh Token
  * 권한 세분화
  * 관리자 기능 확장

---

## 다음 단계

* Refresh Token – 재발급 / 로그아웃 / 회수 전략
