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
* 파일명 규칙은 `.repository.js`

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

## 4. save 함수 구현 (단건, UPSERT)

뉴스 수집은 동일 데이터가 반복 유입될 수 있으므로,
저장 시점에 **UPSERT(있으면 업데이트, 없으면 INSERT)** 를 사용한다.

```js
const pool = require("../db/pool");

async function save(item) {

  const sql = `
    INSERT INTO news
      (source_key, link_hash, title, link, publisher, published_at)
    VALUES
      (?, ?, ?, ?, ?, ?)
    ON DUPLICATE KEY UPDATE
      title = VALUES(title),
      publisher = VALUES(publisher),
      published_at = VALUES(published_at)
  `;

  const params = [
    item.sourceKey,
    item.linkHash,
    item.title,
    item.link,
    item.publisher || null,
    item.publishedAt || null,
  ];

  await pool.query(sql, params);
}

module.exports = {
  save,
  saveAll,
};
```

포인트:

* SQL은 문자열 그대로 유지
* 데이터 가공은 최소화
* INSERT/UPDATE 성공 여부를 해석하지 않음
* 중복 데이터 처리는 **DB 제약 + UPSERT SQL** 로 해결

---

## 5. 에러 처리 원칙

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

## 다음 단계

다음 문서에서는  
cron으로 실행되는 워커가 실제로 수행할  
**RSS 수집 → 변환 → 저장** 흐름을 구현한다.

→ [**RSS 수집 및 변환 구현**](07-rss_fetch_and_parse.md)