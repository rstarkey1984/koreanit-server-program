# 비밀번호 암호화 적용 (PasswordEncoder)

이 문서에서는 회원가입 시 **비밀번호를 평문으로 저장하지 않고**, `PasswordEncoder`를 사용해 **단방향 해시(암호화)** 값으로 저장한다.

---

## 1. 왜 비밀번호를 암호화해야 하는가

비밀번호를 그대로 저장하면 다음 문제가 발생한다.

* DB 유출 시 모든 계정이 즉시 위험해진다
* 운영자/개발자가 비밀번호를 볼 수 있다
* 보안 사고 시 책임 문제가 발생한다

그래서 서버에서는 항상 다음 원칙을 따른다.

> **비밀번호의 ‘원문(plain text)’은 저장하지 않는다.**
> 대신 단방향 해시 값만 저장하고,
> 로그인 시에는 `matches`로 비교(검증) 한다.

---

## 2. 사용할 방식: BCrypt

> BCrypt는 Salt와 반복 연산을 포함해 비밀번호를 느리게 해시하는 비밀번호 전용 알고리즘이다.

특징:

* 단방향 해시
* 같은 비밀번호라도 매번 다른 결과 (salt 자동 포함)
* 연산 비용(cost) 조절 가능

---

## 2-1. Gradle 의존성 추가 (필수)

`PasswordEncoder`, `BCryptPasswordEncoder`는 **Spring Security의 crypto 모듈**에 포함되어 있다.

### build.gradle

```gradle
dependencies {
    implementation 'org.springframework.security:spring-security-crypto'
}
```

---

## 3. `PasswordEncoder` Bean 등록

비밀번호 암호화를 위해 `PasswordEncoder`를 `Bean`으로 등록한다.

### 3-1. 전용 Configuration 클래스 생성 (권장)

권장 구조:

```text
config/
└── CryptoConfig.java
```

```java
package com.koreanit.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### `@Configuration` + `@Bean` 한 세트 의미

> **`@Configuration`은 “이 클래스는 설정용이다”라는 표시이고,**
> **`@Bean`은 “이 메서드를 실행해서 나온 객체를 Spring이 관리해라”라는 표시다.**

---

### 동작 흐름 요약

1. 애플리케이션 시작
2. Spring이 `@Configuration` 클래스를 발견
3. 클래스 내부의 `@Bean` 메서드를 **Spring이 직접 실행**
4. 반환된 객체를 **Spring 컨테이너에 등록**
5. 이후 필요한 곳에 **같은 객체를 주입**

---

### 핵심 포인트

* `@Bean` 메서드는 **개발자가 호출하지 않는다**
* 객체는 기본적으로 **한 번만 생성(싱글톤)**
* 클래스 안에 있지만 **로직이 아닌 설정 선언 역할**을 한다

---

## 4. PasswordEncoder 주입 방식

```java
private final PasswordEncoder passwordEncoder;

public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
}
```

---

## 5. Service에서 비밀번호 해시 적용

회원가입 Service 로직에서 **저장 직전에 해시**를 적용한다.

### 기존 코드 (문제)

```java
long userId = userRepository.insertUser(
    req.username,
    req.password,   // 평문
    req.nickname,
    req.email
);
```

### 수정 코드

```java
String encodedPassword = passwordEncoder.encode(req.password);

long userId = userRepository.insertUser(
    req.username,
    encodedPassword, // 해시값
    req.nickname,
    req.email
);
```

원칙:

* 암호화 책임은 **Service**에 있다
* Repository는 **서비스가 준 값을 그대로 저장**한다

---

## 6. (로그인 단계) 비밀번호 검증은 `matches`

로그인에서는 DB에 저장된 해시값과, 사용자가 입력한 비밀번호를 비교한다.

```java
boolean ok = passwordEncoder.matches(inputPassword, savedHash);

if (!ok) {
    throw new RuntimeException("비밀번호 불일치");
}
```

포인트:

* BCrypt는 같은 비밀번호라도 `encode` 결과가 매번 다르다
* 따라서 로그인에서 `encode(input)`로 문자열 비교를 하면 안 된다
* 반드시 `matches(평문, 해시)`로 검증한다

---

## 7. 테스트 API 페이지(개발/실습용)

목표:

* 서버가 정상 실행되는지 확인
* `PasswordEncoder` Bean 주입이 되는지 확인
* `encode / matches` 동작을 **눈으로 확인**

> 이 테스트 엔드포인트는 **개발/실습용**이다. 운영에서는 제거하거나 접근을 제한한다.

### 7-1. 테스트용 Controller 생성

```java
package com.koreanit.spring.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PasswordTestController {

    private final PasswordEncoder passwordEncoder;

    public PasswordTestController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 비밀번호 해시 테스트 API
     * 예: /test/password?password=1234
     */
    @GetMapping("/test/password")
    public Map<String, Object> test(@RequestParam String password) {

        String encoded = passwordEncoder.encode(password);

        Map<String, Object> result = new HashMap<>();
        result.put("input", password);
        result.put("encoded", encoded);
        result.put("match_self", passwordEncoder.matches(password, encoded));

        return result;
    }

    /**
     * 비밀번호 검증 테스트 API
     * 예: /test/password/match?input=1234&hash=$2a$10$...
     */
    @GetMapping("/test/password/match")
    public Map<String, Object> match(
        @RequestParam String input,
        @RequestParam String hash
    ) {
        boolean ok = passwordEncoder.matches(input, hash);

        Map<String, Object> result = new HashMap<>();
        result.put("input", input);
        result.put("hash", hash);
        result.put("match", ok);

        return result;
    }
}
```

### 7-2. 호출 방법

브라우저에서 직접 접속:

* `http://localhost:8080/test/password?password=1234`

예상 결과 예시:

```json
{
  "input": "1234",
  "encoded": "$2a$10$....",
  "match_self": true
}
```

포인트:

* `encoded` 값은 **매번 바뀐다**
* 그런데 `match_self`는 항상 `true`
* → BCrypt는 “문자열이 같아야 하는 방식”이 아니라 “검증 로직”이 핵심

---

## 정리

* 비밀번호는 **평문 저장 금지**
* 저장 전 `encode`
* 로그인 검증은 `matches`
* `spring-boot-starter-security`는 “필터체인/로그인 프레임워크”까지 켜므로
  **비밀번호 해시만 필요한 단계에서는 사용하지 않는다**
* 대신 `spring-security-crypto`만 추가한다

---

## 다음 단계

* 회원가입 + 로그인 흐름 완성(회원가입 encode / 로그인 matches)
* 예외 처리 표준(ApiException/GlobalExceptionHandler) 적용
