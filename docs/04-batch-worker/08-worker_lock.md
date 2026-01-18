# 중복 실행 방지 락 구현 (TTL 정리 방식)

이 문서에서는  
Node.js 워커가 **동시에 여러 번 실행되는 상황을 방지하기 위한 락(lock)** 을  
**TTL + 만료 정리 방식**으로 구현한다.

이 방식은  
복잡한 분산 락이나 외부 인프라 없이도  
운영 환경에서 충분히 안전한 실행 제어를 제공한다.

---

## 이 문서의 목적

* 워커 중복 실행 문제가 왜 발생하는지 이해한다
* "프로세스가 죽을 수 있다"는 전제를 구조에 반영한다
* TTL 기반 락의 가장 단순한 안전장치를 구현한다
* 정상 종료 / 비정상 종료를 구분해 처리한다

---

## 중복 실행 문제의 본질

워커는 다음 이유로 중복 실행되기 쉽다.

* cron 오작동
* 실행 시간이 길어짐
* 운영자의 수동 재실행
* 프로세스 강제 종료(SIGKILL)

이때 가장 위험한 상황은:

> **락을 잡은 상태로 프로세스가 죽는 것**

---

## 해결 전략 요약

이 문서에서 사용하는 전략은 단순하다.

1. 워커 시작 시 **만료된 락을 모두 삭제**
2. 락 INSERT 시도
3. 성공 → 작업 실행
4. 정상 종료 시 락 DELETE
5. 비정상 종료 → TTL 만료 시 자동 정리

---

## 락 테이블 설계

```sql
CREATE TABLE worker_lock (
  lock_key   VARCHAR(100) NOT NULL,
  expires_at DATETIME     NOT NULL,
  PRIMARY KEY (lock_key),
  KEY idx_worker_lock_expires_at (expires_at)
) ENGINE=InnoDB;
```

의미:

* `lock_key` : 워커 식별자
* `expires_at` : 이 시간이 지나면 락은 무효

---

## lock 유틸리티 구현

### 파일 위치

```text
src/
  utils/
    lock.js
```

---

### 구현 코드

```js
// src/utils/lock.js
const pool = require("../db/pool");

async function cleanupExpired() {
  const sql = `
    DELETE FROM worker_lock
    WHERE expires_at < NOW()
  `;
  await pool.query(sql);
}

async function acquire(lockKey, ttlSeconds = 300) {
  // 1. 만료된 락 정리
  await cleanupExpired();

  // 2. 락 획득 시도
  const sql = `
    INSERT INTO worker_lock (lock_key, expires_at)
    VALUES (?, DATE_ADD(NOW(), INTERVAL ? SECOND))
  `;

  await pool.query(sql, [lockKey, ttlSeconds]);
}

async function release(lockKey) {
  const sql = `
    DELETE FROM worker_lock
    WHERE lock_key = ?
  `;
  await pool.query(sql, [lockKey]);
}

module.exports = {
  acquire,
  release,
};
```

---

## 엔트리포인트 적용

```js
await lock.acquire("fetch_news_worker", 300);

try {
  await fetchNewsJob.execute();
} finally {
  await lock.release("fetch_news_worker");
}
```

---

## 실패 흐름 정리

```text
락 INSERT 실패 (이미 존재)
  ↓
DB 에러 발생
  ↓
throw
  ↓
워커 실패 (exit 1)
```

중복 실행은  
**정상 동작이 아니라 실패**다.

---

## 이 방식의 장점

* 구현 단순
* 사고 모델 직관적
* 프로세스 강제 종료에도 안전
* 강의 난이도 최소

---

## 이 문서에서 다루지 않는 것

* 락 연장(heartbeat)
* owner 식별
* 분산 락(Redis 등)

> 목적은 **"안전한 최소 락" 구조 이해**다.

---

## 다음 단계

다음 문서에서는  
이 워커를 **cron과 연결해 실제로 실행**한다.

→ [cron 기반 워커 실행](09-cron_execution.md)
