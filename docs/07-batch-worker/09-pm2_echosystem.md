# PM2 기반 Node.js 서비스 운영
> ecosystem.config.js + dotenv + .env 구성

## 1. 목적

본 강의안은 **이미 존재하는 `.env` 파일을 그대로 사용**하면서,
PM2의 `ecosystem.config.js`를 이용해 **Node.js 애플리케이션을 안정적으로 실행**하는 표준 방식을 설명한다.

이 구성을 통해 다음을 달성한다.

* PM2로 프로세스 실행 / 재시작 / 로그 관리
* `.env` 파일을 이용한 환경변수 분리
* 코드 수정 없이 로컬과 서버 환경 동일하게 운영
* 서버 재부팅 후 자동 실행

---

## 2. 전제 조건

다음 조건을 이미 만족한다고 가정한다.

* 프로젝트 루트에 `.env` 파일이 존재한다
* Node.js 애플리케이션은 `npm start`로 실행된다
* PM2를 사용할 서버 환경(systemd 기반)

---

## 3. 프로젝트 구조 예시

```text
my-app/
├─ ecosystem.config.js
├─ package.json
├─ .env
├─ .gitignore
└─ src/
   └─ app.js (또는 index.js)
```

---

## 4. pm2 설치

```bash
npm install -g pm2
```

설치확인:
```bash
pm2 -v
```

---

## 5. ecosystem.config.js 작성

프로젝트 루트에 `ecosystem.config.js`를 작성한다.

```js
module.exports = {
  apps: [
    {
      name: "my-app",
      script: "npm",
      args: "start",
      cwd: "/home/ubuntu/projects/koreanit-server/worker",

      // 핵심 매 1분마다 실행
      cron_restart: "*/1 * * * *",

      watch: false,
      autorestart: false,   // 중요: 종료되면 재시작하지 않음
      time: true
    }
  ]
};

```

### 핵심 포인트

* `script: "npm" + args: "start"` → `npm start` 실행
* `cwd`는 반드시 **프로젝트 절대경로**를 사용한다

### Cron Restart 크론 문법 정리

```text
*/1 * * * *
```

크론(cron) 문법은 **총 5칸**으로 구성된다.

```text
분   시   일   월   요일
```

각 위치의 의미:

```text
*/1  *   *   *   *
│
└─ 매 1분마다
```

정확한 해석:

> **매 분마다 실행**

자주 쓰는 예시:

* `*/5 * * * *` → 5분마다 실행
* `0 * * * *` → 매 시간 정각(0분)
* `0 2 * * *` → 매일 새벽 2시 실행

PM2의 `cron_restart`는 리눅스 크론 문법을 그대로 사용한다.


---

## 6. PM2 실행 및 관리

### 실행

```bash
pm2 start ecosystem.config.js
```

### 상태 확인

```bash
pm2 list
```

### 로그 확인

```bash
pm2 logs my-app
```

### 재시작 / 중지 / 제거

```bash
pm2 restart my-app
pm2 stop my-app
pm2 delete my-app
```

---

## 정리

* `.env` 파일은 **Node 애플리케이션이 직접 로드**한다
* PM2는 프로세스 관리만 담당한다
* `ecosystem.config.js + dotenv + .env` 조합은 가장 안정적인 운영 방식이다
