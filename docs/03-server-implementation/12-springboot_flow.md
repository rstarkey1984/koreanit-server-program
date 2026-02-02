# Spring Boot 애플리케이션 전체 흐름

## 0. 이 챕터의 목적

이 챕터의 목표는 **Spring Boot 애플리케이션이 어떻게 동작하는지를 한 번에 조망**하는 것이다.

지금까지 우리는 Controller, Service, Repository, Entity, Domain, DTO를 나누어 구현했다.
이제 이 코드들이 **Spring Boot 내부의 요청 처리 흐름 속에서 어떤 위치와 역할을 가지는지**를 정리한다.

이 챕터 이후부터는 코드를 볼 때 항상 다음 질문을 기준으로 판단한다.

> 이 코드는 Spring Boot 요청 흐름의 **어디에서 실행되는 코드인가?**

---

## 1. Spring Boot 요청 처리의 큰 흐름

Spring Boot 애플리케이션은 다음과 같은 고정된 흐름 위에서 동작한다.

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

이 흐름은 개발자가 바꿀 수 있는 것이 아니다.
우리는 이 흐름 중간중간에 **우리 코드를 배치하여 동작하게 할 뿐**이다.

---

## 2. Filter — 요청의 최전방

### 2-1. Filter의 위치

* Filter는 Controller보다 **앞단**에서 실행된다
* 아직 어떤 Controller가 처리할 요청인지 결정되지 않은 상태
* HTTP 요청/응답 자체를 다룬다

### 2-2. Filter의 책임

Filter는 다음과 같은 공통 관심사를 처리한다.

* 인증 정보 확인
* 요청/응답 로깅
* 공통 헤더 처리
* 요청 차단

Filter 단계에서 요청을 막으면 Controller까지 도달하지 않는다.

---

## 3. Spring Security Filter Chain

Spring Security는 여러 개의 보안 전용 Filter 묶음으로 구성된다.

이 단계에서 결정되는 것:

* 로그인 여부
* 권한(Role) 여부
* 인증 실패 (401)
* 인가 실패 (403)

중요한 점은 다음이다.

> Controller에 들어오기 전에
> 이미 **요청을 허용할지 말지 결정된다**

따라서:

* Controller에서 로그인 체크 ❌
* Service에서 권한 판단 ❌

---

## 4. DispatcherServlet — 모든 요청의 관문

DispatcherServlet은 Spring MVC의 핵심 구성 요소다.

역할을 한 줄로 정리하면 다음과 같다.

> 들어온 HTTP 요청을
> **어떤 Controller의 어떤 메서드로 전달할지 결정한다**

DispatcherServlet 내부에서는 다음 작업이 수행된다.

1. 요청 URL / HTTP Method 분석
2. 매핑된 Controller 메서드 탐색
3. 파라미터 바인딩
4. Controller 메서드 호출
5. 응답 데이터 변환 (JSON)

Controller는 요청의 시작점이 아니라,
DispatcherServlet이 선택하여 호출하는 대상이다.

---

## 5. Controller → Service → Repository

### 5-1. Controller의 역할

Controller는 HTTP 계층의 책임만 가진다.

* HTTP 요청 수신
* 요청 DTO 변환
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

Service는 시스템의 **업무 단위**를 표현한다.

---

### 5-3. Repository의 역할

Repository는 데이터 접근만 담당한다.

* SQL 실행
* 데이터 조회 / 저장
* Entity 반환

Repository는 비즈니스 판단을 하지 않는다.

---

## 6. Entity / Domain / DTO의 위치

지금까지 분리한 객체들을 요청 흐름 위에 배치하면 다음과 같다.

| 구분     | 사용 위치      | 의미          |
| ------ | ---------- | ----------- |
| DTO    | Controller | 외부 요청/응답 계약 |
| Domain | Service    | 비즈니스 모델     |
| Entity | Repository | DB 매핑 객체    |

이 분리는 코드 정리가 목적이 아니다.
**변경이 다른 영역으로 전파되지 않도록 막기 위한 구조**다.

---

## 7. 예외 처리 흐름

Service에서 예외가 발생하면 다음 흐름으로 처리된다.

```
Service
  ↓ throw
DispatcherServlet
  ↓
@ControllerAdvice
  ↓
ApiResponse(JSON)
```

그래서:

* Service에서는 예외를 던진다
* Controller에서 try-catch 하지 않는다
* 응답 포맷은 항상 동일하게 유지된다

---

## 이 챕터의 핵심 정리

이 챕터에서 반드시 기억해야 할 문장들이다.

* Spring Boot는 정해진 요청 처리 흐름 위에서 동작한다
* 우리는 그 흐름에 코드를 배치할 뿐이다
* 각 레이어는 책임이 명확히 다르다
* 어디서 처리해야 하는지가 설계의 핵심이다

---

## 다음 단계

다음 단계에서 ApiResponse 를 도입해 예외/응답을 규격화한다.

→ [**공통 응답 포맷**](/docs/04-common-modules/01-common_response_format.md)