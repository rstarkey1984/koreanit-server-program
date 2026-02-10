# 배포 (systemd 기반 운영)

본 장에서는 애플리케이션을 **단순 실행(jar 실행)** 이 아닌  
**운영 서비스(Service)** 로 배포한다.

이 단계가 끝나면 서버는 다음 상태가 된다.

- 서버 재부팅 후 자동 실행
- 프로세스 장애 시 자동 재시작
- 운영 환경변수 기반 실행
- 로그/실행 방식이 표준화됨

---

## 1. 배포란 무엇인가

### 개발 실행 vs 운영 배포

| 구분 | 개발 | 운영 |
|---|---|---|
| 실행 | 직접 실행 | 서비스로 관리 |
| 재부팅 | 수동 실행 | 자동 실행 |
| 장애 | 수동 복구 | 자동 재시작 |
| 설정 | 파일 직접 수정 | 환경변수 |

> 운영 배포의 핵심은  
> **“사람이 아니라 시스템이 서버를 관리하게 만드는 것”**

---

## 2. 빌드 산출물 생성

### 1) 빌드

```bash
./gradlew clean build
```

### 2) 산출물 확인

```text
build/libs/
├─ spring-0.0.1-SNAPSHOT-plain.jar
└─ spring-0.0.1-SNAPSHOT.jar
```

> jar 파일은 `.class` 파일들과, 자바 실행에 필요한 정보가 함께 포함된 배포용 파일이다.

### 3) 어떤 JAR를 사용해야 하나?

| 파일명                             | 의미                           | 사용 여부 |
| ------------------------------- | ---------------------------- | ----- |
| spring-0.0.1-SNAPSHOT-plain.jar | 애플리케이션 코드만 포함 (의존성 미포함)      | 사용 ❌  |
| spring-0.0.1-SNAPSHOT.jar       | 실행 가능한 JAR (의존성 포함, fat jar) | 사용 ⭕  |

> 운영에서는 **spring-0.0.1-SNAPSHOT.jar 파일 하나만** 있으면 된다.

### 4) 왜 plain.jar는 쓰지 않나?

* plain.jar는 Spring Boot 실행에 필요한 라이브러리가 포함되어 있지 않다
* 단독 실행 시 다음과 같은 문제가 발생한다

  * ClassNotFoundException
  * NoClassDefFoundError
* 보통 다음과 같은 경우에만 사용된다

  * 라이브러리 개발용 모듈
  * 다른 빌드 시스템에 포함될 하위 모듈

### 5) 운영 배포 기준

운영 서버에는 다음 **1개 파일만 전달**한다.

```text
spring-0.0.1-SNAPSHOT.jar
```

이 파일 하나로:

* java -jar 실행 가능
* 외부 설정(application-prod.yml) 분리 가능
* systemd / docker / 배치 실행 모두 대응 가능

---

## 3. 운영 디렉토리 구조 (권장)

```text
/opt/koreanit-api/
├─ app.jar
└─ config/
```

```bash
sudo mkdir -p /opt/koreanit-api
sudo cp build/libs/spring-0.0.1-SNAPSHOT.jar /opt/koreanit-api/app.jar
```

---

## 4. systemd 서비스 파일 작성

### 1) 서비스 파일 위치: `/etc/systemd/system`

파일생성:
```bash
sudo touch /etc/systemd/system/koreanit-api.service
```
소유자변경:
```bash
sudo chown ubuntu /etc/systemd/system/koreanit-api.service
```
VSCode 로 파일 편집:
```bash
code /etc/systemd/system/koreanit-api.service
```

### 2) koreanit-api.service 서비스 파일 편집
```ini
[Unit]
Description=Koreanit API Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/koreanit-api
ExecStart=/usr/bin/java -jar /opt/koreanit-api/app.jar
Restart=always
RestartSec=5
EnvironmentFile=/opt/koreanit-api/config/.env

[Install]
WantedBy=multi-user.target
```

### koreanit-api.service 구성 설명 (표)

| 섹션      | 설정                         | 설명                |
| ------- | -------------------------- | ----------------- |
| Unit    | Description                | 서비스 식별용 이름        |
| Unit    | After=network.target       | 네트워크 준비 후 실행      |
| Service | Type=simple                | 단일 프로세스 실행 방식     |
| Service | User=ubuntu                | ubuntu 계정으로 실행    |
| Service | WorkingDirectory           | 실행 기준 경로 지정       |
| Service | ExecStart                  | Spring Boot 실행 명령 |
| Service | Restart=always             | 종료 시 자동 재시작       |
| Service | RestartSec=5               | 재시작 전 5초 대기       |
| Service | EnvironmentFile            | 외부 환경변수 파일 로드     |
| Install | WantedBy=multi-user.target | 부팅 시 자동 실행 등록     |


---

## 5. 운영 환경변수 파일

### 파일 내용 확인
```text
cat /opt/koreanit-api/config/.env
```

### 출력 예시
```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8000

DB_URL=jdbc:mysql://localhost:3306/koreanit_service
DB_USER=koreanit_app
DB_PASSWORD=password

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
```

---

## 6. 서비스 등록 및 실행

```bash
sudo systemctl daemon-reload
sudo systemctl enable koreanit-api
sudo systemctl restart koreanit-api
```

### 상태 확인
```bash
sudo systemctl status koreanit-api
```

---

## 7. 로그 확인

```bash
journalctl -u koreanit-api -f
```
> systemd로 등록된 koreanit-api 서비스의 실시간 로그를 확인한다.

---

## 8. WSL 재부팅

```bash
exit
```
```Powershell
wsl --shutdown
```
```Powershell
wsl
```

재부팅 후:
```bash
sudo systemctl status koreanit-api
```

> 서버 재부팅 후 자동 실행되면 성공

---

## 체크리스트

- [ ] systemd 서비스로 등록되었는가
- [ ] 서버 재부팅 후 자동 실행되는가
- [ ] 장애 시 자동 재시작되는가
- [ ] 운영 환경변수로 실행되는가

---

## 다음 단계

[**리버스 프록시 (Nginx)**](04-reverse-proxy-nginx.md)
