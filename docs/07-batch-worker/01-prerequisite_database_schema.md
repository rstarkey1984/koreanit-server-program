# 사전 준비 – 뉴스 테이블 스키마

이 문서는 `batch-worker` 파트에서 사용하는 **뉴스 수집 예제**를 위해  
사전에 준비되어 있어야 하는 **데이터베이스 테이블 스키마**를 정의한다.

이 스키마는  
특정 워커 전용이 아니라,  
**Spring Boot 서버와 Node.js 워커가 함께 사용하는 공용 데이터 구조**다.

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
CREATE TABLE `news` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_key` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '뉴스 출처 식별자',
  `link_hash` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '뉴스 링크 해시',
  `title` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '뉴스 제목',
  `link` varchar(2000) COLLATE utf8mb4_general_ci NOT NULL COMMENT '원문 링크',
  `publisher` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '발행처',
  `published_at` datetime DEFAULT NULL COMMENT '뉴스 발행 시각',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수집 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_news_source_link` (`source_key`,`link_hash`),
  KEY `idx_news_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```

---

## 컬럼 설명

| 컬럼명 | 설명 |
|------|----|
| id | 내부 식별용 PK |
| source_key | 뉴스 수집 출처 식별자 (예: kr_it) |
| link_hash | 원문 URL 해시 |
| title | 뉴스 제목 |
| link | 원문 URL |
| publisher | 발행처 |
| published_at | 뉴스 발행 시각 |
| created_at | 워커가 수집한 시각 |

---

## 설계 포인트

### 중복 수집 방지

```sql
UNIQUE KEY (source_key, link_hash)
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


## 다음 단계

Node.js 워커 구현을 진행한다.

→ [**워커 프로젝트 초기화**](01-worker_project_setup.md)
