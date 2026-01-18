# 워커 작업(job) 구현 – 뉴스 수집

이 문서에서는  
앞서 구현한 워커 엔트리포인트(`worker.js`)에  
**실제 작업(job)** 을 연결한다.

이 작업은 예시로  
외부 RSS를 수집해 DB에 저장하는 **뉴스 수집 작업**이다.

---

## 학습 목표

* 워커에서 job 개념의 역할을 이해한다
* 엔트리포인트와 job의 책임을 명확히 분리한다
* job이 실패했을 때 워커 전체가 실패하도록 설계한다
* job 코드를 독립적으로 테스트 가능한 형태로 작성한다

---

## 1. job의 역할과 책임

job은 다음 책임만 가진다.

* "무엇을 할지"에 대한 실제 작업 수행
* 성공 시 정상 종료
* 실패 시 예외 발생(throw)

job은 다음을 **알면 안 된다.**

* 프로세스 종료 코드
* 워커 전체 흐름
* 다른 job의 존재

---

## 2. job 파일 위치

job은 `src/jobs` 하위에 위치한다.

```text
src/
  jobs/
    fetch_news.job.js
```

파일명 규칙:

* 하나의 파일 = 하나의 job
* 동사는 명확하게
* `.job.js` 접미사로 역할을 구분

---

## 3. job 기본 구조

`src/jobs/fetch_news.job.js`

```js
async function execute() {
  // 실제 작업 수행
}

module.exports = {
  execute,
};
```

원칙:

* job은 **함수 묶음**
* 클래스 사용 ❌
* 상태를 내부에 저장하지 않는다

---

## 4. 예시: 뉴스 수집 job 구조

### 4-1. 서비스 / 레포지토리 사용

job은 직접 DB를 다루지 않는다.  
service → repository 흐름을 그대로 사용한다.

```js
// src/jobs/fetch_news.job.js
const newsService = require("../services/news.service");

async function execute() {
  await newsService.fetchAndSave();
}

module.exports = {
  execute,
};
```

job의 역할은 여기까지다.

---

## 5. 엔트리포인트에 job 연결

이제 `worker.js`에서 job을 실행한다.

```js
// src/worker.js
require("dotenv").config();

const pool = require("./db/pool");
const fetchNewsJob = require("./jobs/fetch_news.job");

async function main() {
  await pool.query("SELECT 1");

  await fetchNewsJob.execute();

  await pool.end();
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch(async (err) => {
    try {
      await pool.end();
    } catch (e) {
      console.error("pool.end() failed:", e);
    }

    console.error(err);
    process.exit(1);
  });
```

포인트:

* job 실패 → 예외 throw → 워커 실패
* 엔트리포인트는 **job 결과를 판단하지 않는다**

---

## 6. job 실패 시 동작 흐름

```text
job.execute()
  ↓
에러 발생 (throw)
  ↓
main() 실패
  ↓
catch 진입
  ↓
종료 코드 1
```

이 흐름이 보장되어야 한다.

---

## 7. 이 문서에서 다루지 않는 것

* RSS 파싱 구현
* 외부 API 상세
* 데이터 정합성 검증

이 부분은 다음 문서에서 단계적으로 추가한다.

---

## 다음 단계

다음 문서에서는  
뉴스 수집 job 내부에서 사용할  
**service / repository 구조**를 구현한다.

→ [뉴스 서비스 구현](05-news_service.md)
