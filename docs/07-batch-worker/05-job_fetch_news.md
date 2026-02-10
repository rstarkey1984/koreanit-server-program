# 워커 작업(job) 구현 – 뉴스 수집

이 문서에서는  
앞서 구현한 워커 엔트리포인트(`worker.js`)에  
**실제 작업(job)** 을 연결한다.

이 작업은 예시로  
외부 RSS를 수집해 DB에 저장하는 **뉴스 수집 작업**이다.


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
//const newsService = require("../services/news.service");

async function execute() {
  // 실제 작업 수행
  console.log('fetch_news.job 실행');
  //await newsService.fetchAndSave();
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

## 4. 엔트리포인트에 job 연결

이제 `worker.js`에서 job을 실행한다.
```js
const fetchNewsJob = require("./jobs/fetch_news.job");
```

```js
await fetchNewsJob.execute();
```

---

## 5. job 실패 시 동작 흐름

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

## 다음 단계

다음 문서에서는  
뉴스 수집 job 내부에서 사용할  
**service / repository 구조**를 구현한다.

→ [**뉴스 서비스 구현**](05-news_service.md)
