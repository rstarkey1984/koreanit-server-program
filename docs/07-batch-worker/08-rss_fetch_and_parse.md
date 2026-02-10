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

RSS 파싱을 위해 `axios`, `fast-xml-parser`를 사용한다.

설치:

```bash
npm install axios fast-xml-parser
```

---

## 3. RSS 소스 및 정규화

각 RSS 소스마다 XML 데이터에 매핑되는 테이블 컬럼으로 변환하기 위해 소스 주소와 매핑해서 정규화하는 함수를 만든다.

`src/jobs/rss.sources.js`
```js
const crypto = require("crypto");

function hashLink(link) {
  if (!link) return null;
  return crypto.createHash("sha256").update(link).digest("hex");
}

function parseDate(item) {
  if (item.isoDate) return new Date(item.isoDate);
  if (item.pubDate) return new Date(item.pubDate);
  return null;
}

function extractPublisherFromDescription(html) {
  if (!html) return null;
  const m = String(html).match(/<font[^>]*>(.*?)<\/font>/i);
  return m ? m[1].trim() : null;
}

function normalizeGoogleNewsItem(sourceKey, item) {
  const link = item.link || null;

  const publisher =
    item.source?._ ||
    extractPublisherFromDescription(item.description) ||
    null;

  return {
    sourceKey, // 내부 식별자    
    linkHash: hashLink(link), // 내부 식별자
    title: item.title || null,    
    link, // 원본 링크 (선택)
    publisher,
    publishedAt: parseDate(item),
  };
}

module.exports = {
  kr_it: {
    url: "https://news.google.com/rss/search?q=IT&hl=ko&gl=KR&ceid=KR:ko",
    normalize: (item) => normalizeGoogleNewsItem("kr_it", item),
  },
};
```

---

## 4. RSS 수집 함수 구현

`src/jobs/rss.fetcher.js`
```js
const axios = require("axios");
const { XMLParser } = require("fast-xml-parser");

const parser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: "",
  textNodeName: "#text",
});

const arr = (v) => (v ? (Array.isArray(v) ? v : [v]) : []);
const iso = (s) => {
  if (!s) return null;
  const d = new Date(s);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
};

const mapItem = (it) => {
  const src = it.source;
  const source =
    !src
      ? null
      : typeof src === "string"
      ? { _: src, url: null }
      : { _: src["#text"] ?? src._ ?? null, url: src.url ?? null };

  return {
    title: it.title ?? null,
    link: it.link ?? null,
    pubDate: it.pubDate ?? null,
    isoDate: iso(it.pubDate),
    description: it.description ?? null,
    source,
  };
};

async function fetchRss(url) {
  const { data } = await axios.get(url, {
    timeout: 10000,
    headers: { "User-Agent": "news-fetcher/1.0" },
  });

  const channel = parser.parse(data)?.rss?.channel;
  return arr(channel?.item).map(mapItem);
}

module.exports = { fetchRss };
```

실패 시:

* 네트워크 오류
* 파싱 오류

모두 예외로 처리되어 상위로 전파된다.

---

## 5. job에 RSS 수집 연결

기존 `fetch_news.job.js`를 확장한다.

```js
const rssSources = require("./rss.sources");
const rssFetcher = require("./rss.fetcher");
const newsService = require("../services/news.service");

async function execute() {
  const items = [];

  for (const source of Object.values(rssSources)) {
    const rssItems = await rssFetcher.fetchRss(source.url);
    for (const item of rssItems) {
      items.push(source.normalize(item));
    }
  }

  await newsService.fetchAndSave(items);
}

module.exports = { execute };
```

---

## 6. 전체 흐름 요약

```text
[PM2 / worker.js 실행]
        ↓
[fetch_news.job.execute()]
        ↓
[RSS 소스 순회]
        ↓
[rss.fetcher]
  - axios로 RSS XML 요청
  - fast-xml-parser로 XML 파싱
        ↓
[Raw RSS Item]
        ↓
[rss.sources.normalize()]
  - sourceKey 부여
  - link → hashLink
  - 날짜 파싱
  - publisher 추출
        ↓
[정규화된 JSON 데이터]
        ↓
[news.service.fetchAndSave()]
        ↓
[news.repository]
        ↓
[DB 저장]
```

---

## 8. 실패 처리 원칙

* RSS 수집 실패 → 워커 실패
* 파싱 실패 → 워커 실패
* 저장 실패 → 워커 실패

> 배치는 "조용히 실패"하면 안 된다.

---

## 다음 단계

다음 문서에서는  
실행 결과를 **로그로 남기고 운영 가시성을 높이는 방법**을 다룬다.

→ [**PM2 기반 Node.js 서비스 운영**](09-pm2_echosystem.md)
