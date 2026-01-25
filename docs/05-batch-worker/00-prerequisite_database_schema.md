# 사전 준비 – 뉴스 테이블 스키마

이 문서는  
04-batch-worker 파트에서 사용하는 **뉴스 수집 예제**를 위해  
사전에 준비되어 있어야 하는 **데이터베이스 테이블 스키마**를 정의한다.

이 스키마는  
특정 워커 전용이 아니라,  
**Spring Boot 서버와 Node.js 워커가 함께 사용하는 공용 데이터 구조**다.

---

## 이 문서의 위치와 역할

이 문서는 다음 이유로  
`04-batch-worker` 파트의 **사전 준비 문서**로 분리되어 있다.

* 워커 구현 문서에 DB 설계를 섞지 않기 위함
* 테이블 구조를 "이미 존재하는 전제"로 다루기 위함
* 구현 책임(job / service / repository)과 설계 책임(DB)을 분리하기 위함

---

## 대상 데이터베이스

* DB 이름: koreanit_service
* DBMS: MySQL 8.x
* 문자셋: utf8mb4
* Collation: utf8mb4_general_ci

---

## news 테이블

### 용도

* 외부 뉴스(RSS 등) 수집 결과 저장
* Spring Boot 서버 API / Node.js 워커에서 공통 사용

---

### 테이블 생성 DDL

```sql
USE koreanit_service;

CREATE TABLE news (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_key VARCHAR(50) NOT NULL COMMENT '뉴스 출처 식별자',
  title VARCHAR(255) NOT NULL COMMENT '뉴스 제목',
  link VARCHAR(500) NOT NULL COMMENT '원문 링크',
  publisher VARCHAR(100) NULL COMMENT '발행처',
  published_at DATETIME NULL COMMENT '뉴스 발행 시각',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수집 시각',

  PRIMARY KEY (id),
  UNIQUE KEY uk_news_source_link (source_key, link),
  KEY idx_news_published_at (published_at)
);
```

---

## 컬럼 설명

| 컬럼명 | 설명 |
|------|----|
| id | 내부 식별용 PK |
| source_key | 뉴스 수집 출처 식별자 (예: kr_it) |
| title | 뉴스 제목 |
| link | 원문 URL |
| publisher | 발행처 |
| published_at | 뉴스 발행 시각 |
| created_at | 워커가 수집한 시각 |

---

## 설계 포인트

### 중복 수집 방지

```sql
UNIQUE KEY (source_key, link)
```

* 같은 출처에서 같은 링크는 한 번만 저장
* 워커 재실행 / 중복 실행 시 안전

---

### 조회 성능 고려

```sql
KEY idx_news_published_at (published_at)
```

* 최신 뉴스 조회
* 서버 API 정렬 / 페이지네이션 용도

---

## 이 문서에서 다루지 않는 것

* 뉴스 본문(content) 저장
* 태그 / 카테고리 구조
* 전문 검색 인덱스
* 파티셔닝 / 아카이빙 전략

---

## 다음 단계

다음 문서부터는  
이 스키마가 **이미 존재한다는 전제** 하에  
Node.js 워커 구현을 진행한다.

→ [워커 프로젝트 초기화](01-worker_project_setup.md)
