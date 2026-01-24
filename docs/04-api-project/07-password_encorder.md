# 비밀번호 암호화 적용 (PasswordEncoder)

이 문서에서는 회원가입 시 **비밀번호를 평문으로 저장하지 않고**,
Spring Security의 `PasswordEncoder`를 사용해 **암호화(해시)** 하여 저장한다.

> 목표는 구현보다 **원칙을 정확히 잡는 것**이다.

---

## 1. 왜 비밀번호를 암호화해야 하는가

비밀번호를 그대로 저장하면 다음 문제가 발생한다.

* DB 유출 시 모든 계정이 즉시 위험해진다
* 운영자/개발자가 비밀번호를 볼 수 있다
* 보안 사고 시 책임 문제가 발생한다

그래서 서버에서는 항상 다음 원칙을 따른다.

> **비밀번호의 ‘원문(plain text)’은 저장하지 않는다.
> 대신 단방향 해시 값만 저장하고,
> 로그인 시에는 같은 방식으로 해시해 비교(검증) 한다.**

---

## 2. 사용할 방식: BCrypt

> BCrypt는 Salt와 반복 연산을 포함해 비밀번호를 느리게 해시하는 비밀번호 전용 알고리즘이다.

Spring Security에서 기본으로 사용하는 방식은 **BCrypt**다.

특징:

* 단방향 해시
* 같은 비밀번호라도 매번 다른 결과 (salt 자동 포함)
* 연산 비용(cost) 조절 가능

---

## 2-1. Gradle 의존성 추가 (필수)

`PasswordEncoder`와 `BCryptPasswordEncoder`는
**Spring Security 모듈에 포함된 클래스**다.

따라서 Gradle 설정에
Spring Security starter를 추가해야 한다.

### build.gradle

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
}
```

#### ⚠️ 반드시 알아야 할 점

spring-boot-starter-security를 추가하는 순간, 

- 서버 전체에 Security 필터 체인이 자동 적용된다
- 모든 요청이 기본적으로 인증 대상이 된다
- 별도 설정이 없으면

    - 기본 로그인 페이지가 생성되고
    - 임시 계정(user)이 자동으로 만들어진다

즉,

> spring-boot-starter-security는 보안을 “옵션으로 더하는” 의존성이 아니라, 서버를 “기본 차단 상태”로 바꾸는 의존성이다.

이 때문에 단순히 `PasswordEncoder` 를 사용하려고 의존성을 추가했더라도,    
보안 설정(`SecurityConfig`)을 반드시 함께 정의해야 한다.

---

## 3. `PasswordEncoder`, `Bean` 등록

비밀번호 암호화를 위해 `PasswordEncoder`를 `Bean`으로 등록한다.

### 3-1. 전용 Configuration 클래스 생성 (권장)

현재 프로젝트에는 **명시적인 `@Configuration` 클래스가 없다**.
지금까지는 Spring Boot의 **자동 설정**만으로 동작해왔다.

직접 `Bean` 을 정의해야 하는 시점부터
전용 설정 클래스를 하나 만든다.

권장 구조:

```text
config/
└── SecurityConfig.java
```

```java
package com.koreanit.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
            // CSRF 보호 기능 비활성화
            // - 세션/폼 기반 인증을 사용하지 않는 API 서버 단계
            // - POST, PUT 요청이 403으로 막히는 것을 방지
            .csrf(csrf -> csrf.disable())

            // 요청에 대한 인가(권한) 규칙 설정
            .authorizeHttpRequests(auth -> auth
                // 모든 요청(URL, HTTP 메서드)을 인증 없이 허용
                // - 로그인 필요 없음
                // - 권한 검사 없음
                .anyRequest().permitAll()
            )

            // 위에서 정의한 설정을 기반으로
            // SecurityFilterChain 객체를 생성하여 Spring에 등록
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### `@Configuration` + `@Bean` 한 세트 의미

> **`@Configuration`은 “이 클래스는 설정용이다”라는 표시이고,
> `@Bean`은 “이 메서드를 실행해서 나온 객체를 Spring이 관리해라”라는 표시다.**

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

회원가입 Service 로직에서 **저장 직전에 해시** 를 사용한다.

### 기존 코드 (문제)

```java
long userId = userRepository.insertUser(
    req.username,
    req.password,   // 평문
    req.nickname,
    req.email
);
```

---

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

* 암호화 책임은 **Service**에 있다
* Repository는 서비스가 보내는 값만 믿고 쓴다.

---

## 다음 단계

* 로그인 시 비밀번호 검증 (`matches`)
* 트랜잭션 경계 설정 (`@Transactional`)
* 회원가입 + 로그인 흐름 완성
