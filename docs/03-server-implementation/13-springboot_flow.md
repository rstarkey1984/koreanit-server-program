# Spring Boot 애플리케이션 전체 흐름

## 0. 이 챕터의 목적

이 챕터의 목표는 **Spring Boot 애플리케이션이 실제로 어떻게 동작하는지를 한 눈에 조망**하는 것이다.

지금까지 우리는 다음과 같은 구성 요소들을 분리하여 구현해 왔다.

* Controller
* Service
* Repository
* Entity / Domain / DTO

이제부터는 개별 코드 설명을 넘어서,
**이 코드들이 Spring Boot 내부의 요청 처리 흐름 속에서 어느 지점에서 실행되는지**를 기준으로 이해한다.

이 챕터 이후부터 코드를 볼 때 항상 다음 질문을 기준으로 판단한다.

> 이 코드는 Spring Boot 요청 흐름의 **어디에서 실행되는 코드인가?**

---

## 1. Spring Boot 요청 처리의 큰 흐름

Spring Boot 애플리케이션은 다음과 같은 **고정된 요청 처리 흐름** 위에서 동작한다.

```
Client
  ↓
Filter
  ↓
Spring Security Filter Chain
  ↓
DispatcherServlet
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

이 흐름 자체는 **개발자가 변경할 수 없다**.
Spring Boot와 Spring MVC, Spring Security가 이미 정해 둔 구조다.

개발자는 이 흐름을 바꾸는 대신,
**각 단계에 자신의 코드를 배치하여 동작하도록 만드는 방식**으로 애플리케이션을 설계한다.

---

## 2. Filter — 요청의 최전방

### 2-1. Filter의 위치

* Filter는 모든 요청의 **가장 앞단**에서 실행된다
* 아직 어떤 Controller가 처리할 요청인지 정해지지 않은 상태다
* HTTP 요청과 응답 객체(HttpServletRequest / Response)를 직접 다룬다

즉, Filter는 **MVC 영역 이전의 Servlet 계층**에 속한다.

---

### 2-2. Filter의 책임

Filter는 특정 기능이 아니라,
**모든 요청에 공통으로 적용되어야 하는 관심사**를 처리한다.

대표적인 예는 다음과 같다.

* 요청/응답 로깅
* 인증 정보 추출
* 공통 헤더 처리
* 요청 자체 차단

Filter 단계에서 요청을 차단하면,
그 요청은 **DispatcherServlet과 Controller까지 도달하지 않는다**.

---

## 3. Spring Security Filter Chain

Spring Security는 하나의 기능이 아니라,
**여러 개의 보안 전용 Filter들의 묶음(Filter Chain)** 으로 구성되어 있다.

이 단계에서 결정되는 핵심 사항은 다음과 같다.

* 로그인 여부
* 권한(Role) 보유 여부
* 인증 실패 → 401 UNAUTHORIZED
* 인가 실패 → 403 FORBIDDEN

중요한 기준은 다음 한 문장이다.

> Controller에 들어오기 전에
> 이미 **요청을 허용할지 말지가 결정된다**

그래서 설계 원칙이 명확해진다.

* Controller에서 로그인 체크 ❌
* Service에서 권한 판단 ❌

인증과 인가는 **Security Filter Chain의 책임**이다.

---

## 4. DispatcherServlet — MVC 세계의 관문

DispatcherServlet은 Spring MVC의 핵심 구성 요소이며,
**모든 MVC 요청의 단일 진입점(Front Controller)** 이다.

역할을 한 문장으로 정리하면 다음과 같다.

> 들어온 HTTP 요청을
> **어떤 Controller의 어떤 메서드로 전달할지 결정한다**

---

### 4-1. DispatcherServlet이 하는 일

DispatcherServlet 내부에서는 다음 작업들이 순서대로 수행된다.

1. 요청 URL / HTTP Method 분석
2. 매핑된 Controller 메서드 탐색
3. 파라미터 바인딩
4. Controller 메서드 호출
5. 반환값을 HTTP 응답(JSON)으로 변환

Controller는 요청을 직접 받는 주체가 아니다.

> Controller는 DispatcherServlet이 **선택하여 호출하는 대상**이다.

DispatcherServlet 자체는 개발자가 직접 제어하지 않지만,
Controller, Argument Resolver, MessageConverter, ExceptionHandler 같은
**확장 지점을 통해 동작 결과를 설계한다**.

---

## 5. Controller → Service → Repository

DispatcherServlet 이후부터는
**우리가 직접 작성한 애플리케이션 코드 영역**이다.

---

### 5-1. Controller의 역할

Controller는 HTTP 계층의 책임만 가진다.

* HTTP 요청 수신
* 요청 DTO로 변환
* Service 호출
* 응답 반환

Controller는 비즈니스 규칙을 판단하지 않는다.

---

### 5-2. Service의 역할

Service는 비즈니스 로직의 중심이다.

* 업무 규칙 처리
* 흐름 제어
* 예외 발생 지점
* 트랜잭션의 경계

Service는 시스템이 제공하는 **업무 단위 자체**를 표현한다.

---

### 5-3. Repository의 역할

Repository는 데이터 접근만 담당한다.

* SQL 실행
* 데이터 조회 / 저장
* Entity 반환

Repository는 비즈니스 판단을 하지 않는다.

---

## 6. Entity / Domain / DTO의 위치

지금까지 분리한 객체들을
요청 흐름 위에 배치하면 다음과 같다.

| 구분     | 사용 위치      | 의미          |
| ------ | ---------- | ----------- |
| DTO    | Controller | 외부 요청/응답 계약 |
| Domain | Service    | 비즈니스 모델     |
| Entity | Repository | DB 매핑 객체    |

이 분리는 단순한 코드 정리가 목적이 아니다.

> 변경이 한 계층에 머물도록 하고,
> **다른 영역으로 전파되지 않게 막기 위한 구조**다.

---

## 7. 예외 처리 흐름

Service에서 예외가 발생하면,
요청 흐름은 다음과 같이 역방향으로 올라온다.

```
Service
  ↓ throw
DispatcherServlet
  ↓
@ControllerAdvice
  ↓
ApiResponse(JSON)
```

이 구조로 인해 다음 규칙이 성립한다.

* Service에서는 예외를 던진다
* Controller에서 try-catch 하지 않는다
* 응답 포맷은 항상 동일하게 유지된다

---

## 이 챕터의 핵심 정리

이 챕터에서 반드시 기억해야 할 핵심 문장들이다.

* Spring Boot는 정해진 요청 처리 흐름 위에서 동작한다
* 우리는 그 흐름을 바꾸는 것이 아니라, 그 위에 코드를 배치한다
* 각 레이어는 책임이 명확히 다르다
* 어디서 처리해야 하는지가 설계의 핵심이다

---

## 다음 단계

다음 단계에서는 `ApiResponse`를 도입하여
응답과 예외를 **하나의 규격으로 통일**한다.

→ [**공통 응답 포맷**](/docs/04-common-modules/01-common_response_format.md)