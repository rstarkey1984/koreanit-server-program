# 워커 프로젝트 초기화

이 문서에서는
`~/projects/koreanit-server/worker` 경로에서 사용할 **Node.js 워커 프로젝트를 처음부터 초기화**한다.

이 워커는 HTTP 서버가 아니라,
**실행하면 작업을 수행하고 종료되는 배치 프로그램**이다.


---

## 워커 프로젝트 위치

이 프로젝트는 다음 구조를 기준으로 한다.

```text
~/projects/koreanit-server/
  spring-server/   # Spring Boot 메인 서버
  worker/          # Node.js 배치 / 워커
```

메인 서버와 워커는 **같은 프로젝트 루트**를 사용하지만,
실행되는 프로세스와 역할은 완전히 분리되어 있다.

---

## 1. 워커 프로젝트 생성

다음 명령으로 워커 폴더를 생성하고 이동한다.

```bash
mkdir -p ~/projects/koreanit-server/worker
cd ~/projects/koreanit-server/worker
```

---

## 2. Node.js 프로젝트 초기화

`npm init`을 통해 Node.js 프로젝트를 초기화한다.

```bash
npm init -y
```

생성되는 `package.json`은
이 워커 프로그램의 **설정 파일이자 실행 기준**이 된다.

---

## 3. 필수 패키지 설치

이 워커에서는 다음 기능이 필요하다.

* 환경 변수 로드
* MySQL DB 연결

필수 패키지를 설치한다.

```bash
npm install mysql2 dotenv
```

> Express, 웹 프레임워크는 설치하지 않는다.
> 이 워커는 서버가 아니다.

---

## 4. package.json 기본 설정

`package.json`을 열고
워커 실행을 위한 스크립트를 추가한다.

예시:

```json
{
  "name": "koreanit-batch-worker",
  "version": "1.0.0",
  "private": true,
  "type": "commonjs",
  "scripts": {
    "start": "node src/worker.js"
  },
  "dependencies": {
    "dotenv": "^16.4.0",
    "mysql2": "^3.9.0"
  }
}
```

이제 워커는 다음 명령으로 실행할 수 있다.

```bash
npm start
```

---

## 5. 기본 폴더 구조 생성

다음과 같이 워커 내부 구조를 만든다.

```text
worker/
  package.json
  .env
  src/
    worker.js
    db/
    jobs/
    repositories/
    services/
    utils/
```

폴더 생성 명령:

```bash
mkdir -p src/db src/jobs src/repositories src/services src/utils
```

---

## 6. 엔트리포인트 파일 생성

워커의 시작점이 되는 파일을 생성한다.

```bash
touch src/worker.js
```

이 파일은 다음 역할만 담당한다.

* 환경 변수 로드
* 전체 작업 흐름 제어
* 정상/비정상 종료 코드 반환

> 실제 작업 로직은 `jobs`, `services` 폴더로 분리한다.

---

## 체크 포인트

* 워커가 `~/projects/koreanit-server/worker` 경로에 생성되었는가?
* Spring Boot 프로젝트와 경로가 명확히 분리되어 있는가?
* 서버 프레임워크를 사용하지 않았는가?

---

## 다음 단계

→ [**Node.js 워커 구현을 위한 핵심 문법 정리**](02-nodejs_core_for_worker.md)
