# RSS 수집 및 변환 구현

이 문서에서는  
Node.js 워커가 **외부 RSS를 수집하여 JSON 형태로 변환**하고,  
이를 기존 구조(job → service → repository)에 연결한다.

이 단계의 핵심은  
**외부 입력 소스를 내부 표준 데이터(JSON)로 정규화**하는 것이다.


---

## 1. 설계 원칙

이 문서에서 지키는 원칙은 다음과 같다.

* RSS 수집은 **job 내부 구현**
* service/repository 구조는 변경하지 않음
* 외부 라이브러리는 최소 사용
* 실패는 숨기지 않고 throw

---

## 2. 사용 라이브러리

RSS 파싱을 위해 `rss-parser`를 사용한다.

설치:

```bash
npm install rss-parser
```

---

## 3. RSS 소스 정의

여러 RSS 소스를 지원할 수 있도록  
소스 정의는 객체로 관리한다.

```js
// src/jobs/rss.sources.js
module.exports = {
  kr_it: "https://news.google.com/rss/search?q=IT&hl=ko&gl=KR&ceid=KR:ko",
};
```

---

## 4. RSS 수집 함수 구현

```js
// src/jobs/rss.fetcher.js
const Parser = require("rss-parser");
const parser = new Parser();

async function fetchRss(url) {
  const feed = await parser.parseURL(url);
  return feed.items || [];
}

module.exports = {
  fetchRss,
};
```

실패 시:

* 네트워크 오류
* 파싱 오류

모두 예외로 처리되어 상위로 전파된다.

---

## 5. RSS → JSON 변환 규칙

RSS item을 내부 표준 JSON으로 변환한다.

```js
function normalizeItem(sourceKey, item) {
  return {
    sourceKey,
    title: item.title,
    link: item.link,
    publisher: item.creator || item.publisher || null,
    publishedAt: item.isoDate ? new Date(item.isoDate) : null,
  };
}
```

이 형태는  
`news.repository`가 기대하는 구조와 일치한다.

---

## 6. job에 RSS 수집 연결

기존 `fetch_news.job.js`를 확장한다.

```js
// src/jobs/fetch_news.job.js
const sources = require("./rss.sources");
const { fetchRss } = require("./rss.fetcher");
const newsService = require("../services/news.service");

async function execute() {
  const items = [];

  for (const [sourceKey, url] of Object.entries(sources)) {
    const rssItems = await fetchRss(url);

    for (const item of rssItems) {
      items.push({
        sourceKey,
        title: item.title,
        link: item.link,
        publisher: item.creator || item.publisher || null,
        publishedAt: item.isoDate ? new Date(item.isoDate) : null,
      });
    }
  }

  await newsService.fetchAndSave(items);
}

module.exports = {
  execute,
};
```

---

## 7. service 수정 (입력 받기)

service는 이제  
외부에서 전달된 데이터를 저장만 담당한다.

```js
// src/services/news.service.js
const newsRepository = require("../repositories/news.repository");

async function fetchAndSave(items) {
  for (const item of items) {
    await newsRepository.save(item);
  }
}

module.exports = {
  fetchAndSave,
};
```

---

## 8. 전체 흐름 요약

```text
cron
  ↓
worker.js
  ↓
fetch_news.job
  ↓
RSS fetch & parse
  ↓
JSON normalize
  ↓
news.service
  ↓
news.repository
  ↓
DB
```

---

## 9. 실패 처리 원칙

* RSS 수집 실패 → 워커 실패
* 파싱 실패 → 워커 실패
* 저장 실패 → 워커 실패

> 배치는 "조용히 실패"하면 안 된다.

---

## 이 문서에서 다루지 않는 것

* 재시도(backoff)
* 일부 실패 허용 전략
* RSS 항목 필터링

---

## 다음 단계

다음 문서에서는  
실행 결과를 **로그로 남기고 운영 가시성을 높이는 방법**을 다룬다.

→ [실행 로그 및 모니터링](11-logging_and_monitoring.md)
