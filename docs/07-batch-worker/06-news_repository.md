# 뉴스 리포지토리 구현

이 문서에서는  
뉴스 서비스에서 호출하는 **repository 계층**을 구현한다.

repository는  
DB 접근과 SQL 실행만 책임지는 계층이며,  
**이 프로젝트에서 SQL이 존재할 수 있는 유일한 위치**다.


---

## 1. repository의 역할 정의

repository는 다음 책임만 가진다.

* SQL 작성 및 실행
* DB 결과를 JavaScript 객체로 반환
* DB 오류를 그대로 상위로 전달

repository는 다음을 **알지 못한다.**

* 업무 흐름
* 성공/실패 의미
* 프로세스 종료 방식

---

## 2. repository 파일 위치

리포지토리는 `src/repositories` 하위에 위치한다.

```text
src/
  repositories/
    news.repository.js
```

규칙:

* 테이블 단위로 파일 분리
* 파일명은 반드시 `.repository.js`
* 클래스 사용 ❌

---

## 3. DB 커넥션 풀 사용

repository는 직접 DB를 생성하지 않는다.  
이미 준비된 커넥션 풀을 가져다 쓴다.

```js
const pool = require("../db/pool");
```

이 방식으로:

* 워커 전체에서 하나의 풀 공유
* 커넥션 생성/해제 책임 분리

---

## 4. save 함수 구현 (단건)

가장 단순한 저장 함수부터 만든다.

```js
// src/repositories/news.repository.js
const pool = require("../db/pool");

async function save(item) {
  const sql = `
    INSERT INTO news
      (source_key, title, link, publisher, published_at)
    VALUES
      (?, ?, ?, ?, ?)
  `;

  const params = [
    item.sourceKey,
    item.title,
    item.link,
    item.publisher || null,
    item.publishedAt || null,
  ];

  await pool.query(sql, params);
}

module.exports = {
  save,
};
```

포인트:

* SQL은 문자열 그대로 유지
* 데이터 가공은 최소화
* 성공/실패 판단하지 않음

---

## 5. 중복 데이터 처리 전략

`news` 테이블에는 다음 제약이 있다.

```sql
UNIQUE KEY (source_key, link)
```

중복 데이터 처리 전략은 **service 책임**이지만,  
repository는 SQL 레벨에서 안전장치를 둔다.

예시 1: 무시

```sql
INSERT IGNORE INTO news (...)
```

예시 2: 업데이트

```sql
INSERT INTO news (...)
ON DUPLICATE KEY UPDATE
  published_at = VALUES(published_at);
```

이 문서에서는  
**기본 INSERT + DB 제약 위임** 방식만 사용한다.

---

## 6. 다건 저장 함수 (선택)

service에서 다건 저장이 필요하다면  
repository에 별도 함수를 추가한다.

```js
async function saveAll(items) {
  for (const item of items) {
    await save(item);
  }
}

module.exports = {
  save,
  saveAll,
};
```

> 성능 최적화는 목적이 아니다.  
> 구조 명확성이 우선이다.

---

## 7. 에러 처리 원칙

repository에서는:

* try-catch 사용 ❌
* DB 에러 그대로 throw ⭕

```js
await pool.query(sql, params);
```

에러는:

* service로 전달
* job → 엔트리포인트까지 전파
* 워커 실패로 연결

---

## 8. 이 문서에서 다루지 않는 것

* 트랜잭션 제어
* 대량 bulk insert 최적화
* DB 성능 튜닝

> 목적은 SQL 위치 고정과 책임 분리다.

---

## 다음 단계

이제 워커의 기본 구조는 완성되었다.

다음 문서에서는  
전체 흐름을 한 번에 정리하고,  
실행 시나리오를 점검한다.

→ [워커 전체 흐름 정리](07-worker_flow_summary.md)
