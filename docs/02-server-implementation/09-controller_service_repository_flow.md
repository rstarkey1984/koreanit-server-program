# Controller → Service → Repository 흐름 실습

이 장에서는  
하나의 HTTP 요청이 들어왔을 때  
**Controller → Service → Repository → DB** 로  
어떻게 흐르는지 직접 확인한다.

이 단계의 핵심은  
새로운 기술을 배우는 것이 아니라,  
**앞에서 설계한 계층 구조가 실제로 어떻게 동작하는지 체감하는 것**이다.

---

## 강의 목표

* 서버 요청 처리 전체 흐름을 설명할 수 있다
* Controller, Service, Repository의 역할을 다시 구분할 수 있다
* 각 계층이 언제 호출되는지 이해한다
* 이후 트랜잭션 학습을 위한 흐름 기반을 만든다

---

## 1. 실습 시나리오

다음과 같은 단순한 요청을 기준으로 흐름을 확인한다.

```text
GET /ping
```

이 요청은:

* DB에 실제 데이터를 조회하지 않고
* 가장 단순한 SQL(`SELECT 1`)만 실행한다

목적은 **기능 구현이 아니라 구조 검증**이다.

---

## 2. Controller 역할 확인

Controller는  
HTTP 요청을 받고,  
Service를 호출한 뒤 응답을 반환한다.

```java
@GetMapping("/ping")
public Integer ping() {
    return helloService.dbPing();
}
```

이 단계에서 Controller는:

* SQL을 모른다
* DB 존재 여부를 모른다
* 요청/응답 처리만 담당한다

---

## 3. Service 역할 확인

Service는  
요청 흐름을 제어하고  
Repository 호출을 위임한다.

```java
public Integer dbPing() {
    return userRepository.ping();
}
```

Service의 특징:

* SQL을 직접 실행하지 않는다
* Repository 결과를 그대로 전달한다
* 이후 단계에서는 여러 Repository 호출을 묶게 된다

---

## 4. Repository 역할 확인

Repository는  
데이터 접근만 전담한다.

```java
public Integer ping() {
    String sql = "SELECT 1";
    return jdbcTemplate.queryForObject(sql, Integer.class);
}
```

Repository에서 일어나는 일:

* SQL 실행
* DB 연결 사용
* 결과 반환

판단이나 흐름 제어는 하지 않는다.

---

## 5. 전체 호출 흐름 정리

요청 하나가 들어오면  
서버 내부에서는 다음 순서로 실행된다.

```text
HTTP 요청 (/ping)
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
JdbcTemplate
  ↓
MySQL
```

이 흐름은  
이후 모든 API에서 동일하게 반복된다.

---

## 6. 디버깅 관점에서 흐름 보기

실습 중에는  
다음 지점에 브레이크포인트를 찍어보는 것이 좋다.

* Controller 메서드 시작
* Service 메서드 시작
* Repository 메서드 시작

이렇게 하면  
요청 하나가 계층을 따라 이동하는 과정을  
눈으로 확인할 수 있다.

---

## 7. 이 구조가 중요한 이유

이 구조 덕분에:

* 각 계층의 책임이 명확해진다
* 코드 수정 범위가 줄어든다
* 테스트와 유지보수가 쉬워진다

특히 Service 계층이 중심이 되어야  
다음 단계인 **트랜잭션 관리**를 적용할 수 있다.

---

## 8. 이 장의 핵심 정리

* 요청은 Controller에서 시작된다
* Service는 흐름을 제어한다
* Repository는 데이터 접근만 담당한다
* 하나의 요청은 계층을 따라 순차적으로 흐른다

---

## 다음 단계

→ [**10. 트랜잭션 관리**](10-transaction_management.md)
