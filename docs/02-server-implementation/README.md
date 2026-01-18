# 서버 프로그램 구현하기

이 파트에서는
앞에서 준비한 개발 환경을 기반으로
**서버 프로그램의 내부 구조와 구현 흐름을 단계적으로 완성**한다.

이 파트의 목표는
"기능을 많이 만드는 것"이 아니라,
**서버가 어떻게 동작하는지 스스로 설명할 수 있는 구조 감각**을 만드는 것이다.

---

## 이 파트에서 다루는 것

* Web Server와 WAS의 역할 구분
* Spring Boot 기반 서버 실행 구조
* Controller / Service / Repository 계층 분리
* DB 연동을 위한 필수 개념(MySQL, JDBC, 트랜잭션)
* 요청 → 처리 → 응답까지의 전체 흐름 이해
* 기본적인 CRUD API 설계 기준

---

## 문서 구성 (폴더 내부 번호 기준)

1. [웹 서버와 WAS 개념 이해](01-webserver_vs_was.md)
2. [Spring Boot 프로젝트 생성 및 실행](02-spring_boot_project_setup.md)
3. [프로젝트 구조 개요](03-project_structure_overview.md)
4. [Controller 계층](04-controller_layer.md)
5. [Service 계층](05-service_layer.md)
6. [MySQL 설치 및 기본 구성](06-mysql_install_setup.md)
7. [Repository 계층과 SQL](07-repository_layer_sql.md)
8. [데이터베이스 스키마 설계](08-database_schema_design.md)
9. [JDBC 커넥션 풀](09-jdbc_connection_pool.md)
10. [트랜잭션 관리](10-transaction_management.md)
11. [요청 / 응답 흐름 정리](11-request_response_flow.md)
12. [CRUD API 설계](12-crud_api_design.md)
13. [에러 처리 전략](13-error_handling_strategy.md)
14. [로깅 전략](14-logging_strategy.md)
15. [기본 리팩토링](15-basic_refactoring.md)

---

## 이 파트의 학습 결과

이 파트를 마치면 학생은 다음을 설명할 수 있어야 한다.

* 웹 서버와 WAS의 역할 차이
* Spring 서버 내부 계층 흐름
* DB 연동이 서버 구조에 포함되는 방식
* 하나의 요청이 서버를 통과하는 전체 과정
* 기능 추가 전에 구조를 먼저 설계해야 하는 이유

---

## 학습 포인트

이 파트에서 가장 중요한 학습 포인트는 다음과 같다.

* 문법보다 구조
* 구현보다 흐름
* 성능보다 책임 분리

> 서버는 "돌아가는 코드"보다
> "설명 가능한 구조"가 먼저다.

---

## 강의 시작

→ [**웹 서버와 WAS 개념 이해**](01-webserver_vs_was.md)
