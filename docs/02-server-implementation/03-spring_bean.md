# Spring Container와 Bean

이 문서는 Spring 학습 초반에 반드시 이해해야 할 **Spring Container** 와 **Bean** 개념을 하나의 흐름으로 정리한 강의용 문서다.

목표는 다음 한 문장을 정확히 이해하는 것이다.

> **Spring 애플리케이션에서 객체 생성과 관리는 개발자가 아니라 Spring Container가 담당한다.**

---

## 1. Spring Container란 무엇인가

Spring Container는
**애플리케이션 내부에서 객체를 대신 관리하는 시스템**이다.

주요 역할:

* Bean 생성
* Bean 보관 및 유지
* Bean 간 의존성 주입
* Bean 생명주기 관리

즉,

> Spring Container는 **객체 관리자**다.

---

## 2. Bean이란 무엇인가

**Bean**이란 다음을 의미한다.

> **Spring Container가 생성하고 관리하는 객체**

정리하면:

* Bean은 객체다
* 개발자가 `new`로 만들지 않는다
* Spring Container가 생성·관리한다

모든 Bean은 객체지만, 모든 객체가 Bean은 아니다.

---

## 3. 일반 객체 vs Bean

| 구분     | 일반 객체       | Bean             |
| ------ | ----------- | ---------------- |
| 생성 주체  | 개발자 (`new`) | Spring Container |
| 관리 책임  | 개발자         | Spring           |
| 의존성 연결 | 수동          | 자동(DI)           |
| 생명주기   | 코드에 의존      | 컨테이너가 관리         |

---

## 4. Controller는 왜 Bean이 되는가

다음 코드를 보자.

```java
@RestController
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

핵심은 `@RestController`다.

이 어노테이션이 붙은 클래스는:

* HTTP 요청 처리 역할을 가지며
* Spring Container에 의해 **자동으로 Bean 등록**된다

따라서 Controller는:

> **Spring Container가 생성한 Bean 객체**다.

---

## 5. Bean 등록 방식

### 5-1. 컴포넌트 스캔 방식 (기본)

다음 어노테이션이 붙은 클래스는 자동으로 Bean이 된다.

* `@RestController`
* `@Service`
* `@Repository`
* `@Component`

실무와 강의에서 가장 많이 사용하는 방식이다.

---

### 5-2. @Bean 수동 등록 방식

설정 클래스에서 메서드 반환값을 Bean으로 등록한다.

* 객체 생성 로직을 직접 제어할 때
* 라이브러리 객체, 설정 객체 등록 시 사용

---

## 6. 객체 생성 흐름 

Spring Boot 서버 시작 시 내부 흐름은 다음과 같다.

```text
서버 시작
→ Spring Container 생성
→ Bean 대상 클래스 탐색
→ Bean 객체 생성
→ 의존성 주입
→ 요청 대기
```

개발자가 객체 생성을 직접 호출하지 않는다.

---

## 7. Bean과 의존성 주입(DI)

의존성 주입은 **Bean과 Bean 사이에서만 가능**하다.

조건:

* 주입받는 대상이 Bean
* 주입되는 대상도 Bean

객체 생성과 연결을 Spring이 대신 처리한다.

---

## 8. Bean Scope

Bean의 기본 Scope는 **singleton**이다.

* 애플리케이션 전체에서 하나의 객체만 생성
* 여러 요청이 같은 Bean을 공유

| Scope     | 의미            |
| --------- | ------------- |
| singleton | 기본값, 1개 객체 공유 |
| prototype | 요청 시마다 새 객체   |
| request   | HTTP 요청당      |
| session   | 세션당           |

Controller, Service, Repository는 기본적으로 singleton이다.

---

## 9. Controller에 상태값을 두면 안 되는 이유

singleton Bean은 여러 요청이 동시에 접근한다.

따라서 Controller나 Service에 **상태를 저장하는 필드**를 두면:

* 동시성 문제 발생
* 요청 간 데이터 섞임 위험

---

## 10. 왜 Bean 구조를 사용하는가

Bean을 사용하는 목적은 다음 한 문장으로 정리된다.

> **객체 생성과 연결을 코드에서 제거하고,
> 애플리케이션 구조를 Spring Container가 관리하게 하기 위해서다.**

---

## 최종 핵심 요약

* Spring Container가 객체를 생성·관리한다
* Spring이 관리하는 객체를 Bean이라 부른다
* Controller / Service / Repository는 모두 Bean이다
* Bean이 있어야 DI, 트랜잭션이 동작한다


## 다음 단계

→ [**04. Controller 계층 구현**](04-controller_layer.md)
