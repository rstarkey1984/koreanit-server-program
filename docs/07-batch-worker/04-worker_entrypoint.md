# 워커 엔트리포인트 구현

이 문서에서는  
Node.js 워커의 엔트리포인트(진입점)인 `src/worker.js`를 구현한다.

엔트리포인트는 프로그램이 실행될 때 가장 먼저 시작되는 지점이다. Node.js 워커에서는 이 파일을 기준으로 설정 로드, 의존성 초기화, 작업 실행 흐름이 결정된다.

1. 환경 변수 로드
2. 전체 작업 흐름 제어
3. 정상/실패 종료 코드 반환

> 실제 작업 로직은 `jobs/`, `services/`, `repositories/`로 분리한다.


---

## 1. 엔트리포인트가 지켜야 할 규칙

### 1-1. "엔트리포인트는 얇아야 한다"

`src/worker.js`에 비즈니스 로직이 들어가기 시작하면  
워커 전체가 유지보수 불가능한 구조로 변한다.

엔트리포인트는:

* 무엇을 할지(흐름)만 알고
* 어떻게 하는지(로직)는 모른다

---

### 1-2. try-catch는 엔트리포인트에만 둔다

워커 내부 로직에서 예외를 잡아버리면

* 실패가 성공처럼 보이거나
* 실패 원인이 숨거나
* 운영에서 "성공/실패" 판단이 불가능해진다

따라서:

* 내부 로직은 `throw`
* 엔트리포인트가 `catch`하고 종료 코드로 변환

---

## 2. worker.js 기본 뼈대 작성

파일 위치:

```text
worker/
  src/
    worker.js
```

---

### 2-1. 기본 코드 작성

`src/worker.js`를 아래처럼 작성한다.

```js
// src/worker.js
require("dotenv").config();

async function main() {
  console.log("[ 메인시작 ] : " + new Date());
  // 1) 준비 단계 (예: DB 연결 확인, 락 획득 등)
  // 2) job 실행
  // 3) 정리 단계 (예: DB 종료, 락 해제 등)
}

main()
  .then(() => {
    process.exit(1);
  })
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
```

이 시점에서는 실제 작업을 실행하지 않는다.  
우선 "성공이면 0, 실패면 1"로 끝나는 형태를 고정한다.

> 종료 코드(exit code)는 이 프로세스를 실행한 “외부 주체”가 사용한다.

---

## 3. 종료 코드 표준화 (0/1)

워커는 보통 다음 환경에서 실행된다.

* cron
* 운영 스크립트

이 환경들은 로그 메시지가 아니라 **종료 코드**로 성공/실패를 판단한다.

* `0` : 성공
* `1` : 실패

이 규칙을 항상 유지한다.

---

## 4. DB 풀을 사용하는 워커의 정리 단계

워커는 작업이 끝나면 반드시 종료된다.  
따라서 다음과 같은 리소스는 종료 전에 정리해야 한다.

* MySQL 커넥션 풀
* 파일 핸들
* 락(중복 실행 방지용)

여기서는 DB 풀만 정리한다.

---

### 4-1. DB 커넥션 풀 모듈 

`src/db/pool.js`

```js
const mysql = require("mysql2/promise");

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: Number(process.env.DB_PORT || 3306),
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

module.exports = pool;
```

---

### 4-2. main()에서 풀 종료까지 포함하기

`src/worker.js`를 아래처럼 수정한다.

```js
require("dotenv").config();

const pool = require("./db/pool");

async function main() {
  console.log("[ 메인시작 ] : " + new Date());

  // 준비 단계: 연결 확인 (가벼운 ping)
  const [rows, fields] = await pool.query("SELECT 1");
  console.log(rows);

  // TODO: job 실행 (다음 문서에서 구현)
  // await fetchNewsJob.execute();

  // 정리 단계: 커넥션 풀 종료
  await pool.end();
}

main()
  .then(() => {
    console.log("[ 정상종료 ] : " + new Date());
    process.exit(0);
  })
  .catch(async (err) => {
    // 실패 시에도 정리 시도
    try {
      await pool.end();
    } catch (e) {
      // 정리 실패는 로깅만 하고, 원래 실패를 유지한다
      console.error("pool.end() failed:", e);
    }

    console.error(err);
    process.exit(1);
  });

process.on("exit", (code) => {
  console.log("exit code =", code);
});
```

---

## 5. 실행 체크

### 5-1. .env 준비

`worker/.env` 예시:

```env
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=koreanit_app
DB_PASSWORD=password
DB_NAME=koreanit_service
```

---

### 5-2. 실행

```bash
npm start
```

또는

```bash
node src/worker.js
```

정상이라면:

* `SELECT 1`이 성공
* 커넥션 풀이 종료
* 프로세스가 종료 코드 0으로 끝난다

---

## 6. .gitignore 설정

`.env` 파일은 Git에 포함되면 안 된다.

`.gitignore`에 다음 항목을 추가한다.

```gitignore
.env
.env.*
```

이미 Git에 추가된 경우:

```bash
git rm --cached .env
git commit -m "chore: remove .env from git"
```

---

## 다음 단계

다음 문서부터는  
이 엔트리포인트에 "실제 작업(job)"을 연결한다.

특히 다음 요소가 순서대로 들어간다.

* 중복 실행 방지 락(옵션)
* job 실행
* 결과 로그 출력

→ [**워커 작업(job) 구현**](04-job_fetch_news.md)
