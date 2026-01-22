# cron 기반 워커 실행

이 문서에서는  
Node.js 워커를 **cron으로 주기 실행**하는 방법을 정리한다.

워커는 HTTP 서버가 아니므로,  
운영 환경에서는 보통 다음 중 하나로 실행된다.

* cron (가장 단순)
* systemd timer
* CI 스케줄러

이 강의안에서는 **cron**을 기준으로 한다.


---

## 1. cron 개념

cron은 Linux에서  
**정해진 시간에 명령어를 실행하는 스케줄러**다.

워커는 다음 구조이므로 cron과 잘 맞는다.

```text
실행 → 작업 수행 → 종료
```

---

## 2. 실행 기준 정리

워커 프로젝트 경로 예시:

```text
~/projects/koreanit-server/worker
```

실행 파일:

```text
src/worker.js
```

실행 명령:

```bash
npm start
```

> cron에서는 상대 경로와 현재 디렉터리가 다를 수 있으므로  
> 항상 **절대 경로 + cd**를 기준으로 작성한다.

---

## 3. 환경 변수 로드 주의사항

워커는 `dotenv`로 `.env`를 로드한다.

```js
require("dotenv").config();
```

따라서 cron에서 별도로 환경 변수를 export하지 않아도 된다.

단, cron의 현재 경로가 다르면 `.env`를 못 찾을 수 있다.

안전하게 실행하려면:

* 반드시 `worker` 디렉터리로 `cd` 후 실행

---

## 4. crontab 설정

### 4-1. 편집기 열기

```bash
crontab -e
```

---

### 4-2. 예시: 5분마다 실행

```cron
*/5 * * * * cd /home/ubuntu/projects/koreanit-server/worker && npm start >> /home/ubuntu/projects/koreanit-server/worker/logs/worker.log 2>&1
```

설명:

* `*/5 * * * *` : 5분마다
* `cd ... && npm start` : 워커 디렉터리로 이동 후 실행
* `>> worker.log` : 표준 출력/에러 로그를 파일에 append
* `2>&1` : stderr를 stdout으로 합침

---

## 5. 로그 파일 디렉터리 준비

위 예시를 사용한다면 `logs` 폴더가 필요하다.

```bash
mkdir -p /home/ubuntu/projects/koreanit-server/worker/logs
```

---

## 6. 실행 확인

### 6-1. cron 등록 확인

```bash
crontab -l
```

---

### 6-2. 로그 확인

```bash
tail -n 200 /home/ubuntu/projects/koreanit-server/worker/logs/worker.log
```

---

## 7. 운영 포인트

### 7-1. 중복 실행 방지 락과 같이 써야 한다

cron은 시간이 되면 무조건 실행한다.  
이전 실행이 끝나지 않았더라도 새로운 실행이 시작될 수 있다.

따라서:

* 락 없으면 중복 실행 발생
* 락 있으면 중복 실행은 실패로 처리

---

### 7-2. 작업 시간이 cron 주기보다 길면?

예:

* 5분마다 실행 설정
* 실제 작업이 10분 걸림

이 경우:

* 두 번째 실행은 락 획득 실패
* 로그에 실패가 기록됨

이 상황은 "버그"가 아니라  
**설정의 문제**다.

---

## 8. 이 문서에서 다루지 않는 것

* systemd 서비스 등록
* 로그 로테이션(logrotate)
* 알림/모니터링(슬랙/메일)

> 목적은 "cron으로 워커 실행" 기본기다.

---

## 다음 단계

다음 문서에서는  
cron으로 실행되는 워커가 실제로 수행할  
**RSS 수집 → 변환 → 저장** 흐름을 구현한다.

→ [RSS 수집 및 변환 구현](10-rss_fetch_and_parse.md)
