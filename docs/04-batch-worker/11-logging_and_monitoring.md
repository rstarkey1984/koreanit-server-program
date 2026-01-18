# 실행 로그 및 모니터링

이 문서에서는  
Node.js 워커의 실행 결과를 **로그로 남기고**,  
운영 환경에서 **정상/실패 여부를 확인하는 방법**을 정리한다.

배치/워커 프로그램은  
"지금 잘 돌고 있는지"를 화면이 아니라 **로그와 기록**으로 확인해야 한다.

---

## 학습 목표

* 워커 로그의 역할을 이해한다
* 어디에 무엇을 로그로 남겨야 하는지 기준을 잡는다
* cron 환경에서 로그를 확인하는 흐름을 익힌다
* "조용한 실패"를 방지하는 운영 감각을 익힌다

---

## 1. 워커 로그의 성격

워커 로그는 다음 특징을 가진다.

* 사용자에게 보여주지 않는다
* 개발자/운영자를 위한 기록이다
* 실행 단위 기준으로 남는다

따라서 로그는:

* 많을 필요 ❌
* 의미 있어야 ⭕
* 흐름을 파악할 수 있어야 ⭕

---

## 2. 로그 레벨 기준

이 강의안에서는  
아래 수준까지만 사용한다.

| 레벨 | 용도 |
|---|---|
| INFO | 정상 흐름, 작업 시작/종료 |
| ERROR | 실패, 예외 발생 |

DEBUG, TRACE는 사용하지 않는다.

---

## 3. 로그 위치 원칙

로그는 다음 위치에 남긴다.

* 워커 시작 시
* job 시작/종료 시
* 처리 건수 요약
* 실패 시 예외 전체

반대로 다음은 로그로 남기지 않는다.

* SQL 상세
* 반복 처리 중 개별 데이터

---

## 4. 간단한 로거 유틸리티

### 파일 위치

```text
src/
  utils/
    logger.js
```

---

### 구현 예시

```js
// src/utils/logger.js
function info(message, meta = {}) {
  console.log(
    JSON.stringify({
      level: "INFO",
      time: new Date().toISOString(),
      message,
      ...meta,
    })
  );
}

function error(message, err) {
  console.error(
    JSON.stringify({
      level: "ERROR",
      time: new Date().toISOString(),
      message,
      error: err?.message,
      stack: err?.stack,
    })
  );
}

module.exports = {
  info,
  error,
};
```

> 라이브러리를 쓰지 않고  
> 표준 출력(JSON)만 사용한다.

---

## 5. 엔트리포인트에 로그 적용

`src/worker.js` 예시:

```js
const logger = require("./utils/logger");

async function main() {
  logger.info("worker started");

  await lock.acquire("fetch_news_worker", 300);

  try {
    logger.info("job started", { job: "fetch_news" });
    await fetchNewsJob.execute();
    logger.info("job finished", { job: "fetch_news" });
  } finally {
    await lock.release("fetch_news_worker");
    logger.info("lock released");
  }

  logger.info("worker finished");
}
```

---

## 6. 처리 결과 요약 로그

job 또는 service 단에서  
"몇 건을 처리했는지" 정도는 요약 로그로 남긴다.

```js
logger.info("news fetched", {
  source: sourceKey,
  count: rssItems.length,
});
```

이 정도 로그면 충분하다.

---

## 7. 실패 로그 흐름

실패는 다음 위치에서 기록된다.

* 엔트리포인트 `catch`
* logger.error 한 번만

```js
.catch((err) => {
  logger.error("worker failed", err);
  process.exit(1);
});
```

> 실패 로그는 반드시 한 번만, 전체 스택 포함

---

## 8. cron 환경에서 로그 확인

cron 설정에서  
stdout/stderr를 파일로 리다이렉션했다.

```cron
>> logs/worker.log 2>&1
```

확인:

```bash
tail -n 200 logs/worker.log
```

---

## 9. 운영 체크 포인트

* 로그가 전혀 없다 → 실행 안 됨
* 시작 로그만 있고 종료 로그 없음 → 중간 실패
* 실패 로그가 반복 → cron 주기/락/외부 장애 점검

---

## 이 문서에서 다루지 않는 것

* 로그 로테이션(logrotate)
* 외부 로그 수집 시스템
* 알림(Slack, Email)

> 목적은 **"운영자가 눈으로 확인 가능한 로그"** 다.

---

## 다음 단계

이제 워커는:

* 구조적으로 완성
* 운영 실행 가능
* 실패 추적 가능

다음 단계는 선택 사항이다.

* 성능 최적화
* 재시도 전략
* 운영 고도화
