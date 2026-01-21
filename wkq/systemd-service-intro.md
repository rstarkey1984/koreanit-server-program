# systemd 서비스

## 강의 목표

이 장이 끝나면 학생은 다음을 할 수 있어야 한다.

* systemd가 무엇인지 설명할 수 있다
* 리눅스에서 서비스가 어떻게 관리되는지 이해한다
* 직접 systemd 서비스 파일을 작성하고 등록할 수 있다
* 서버 프로그램을 부팅 시 자동 실행하도록 설정할 수 있다

---

## 1. 왜 systemd를 배우는가

지금까지 우리는 다음 방식으로 서버 프로그램을 실행했다.

```bash
java -jar app.jar
node server.js
```

이 방식의 문제점:

* 터미널을 닫으면 프로그램 종료됨
* 서버 재부팅 시 자동 실행 안 됨
* 프로세스 상태 관리 불가
* 장애 발생 시 자동 재시작 불가

> 실제 서버에서는 프로그램을 "명령어"가 아니라
> "서비스(Service)"로 관리한다.

---

## 2. systemd란 무엇인가

* 리눅스의 시스템 및 서비스 관리자
* 부팅부터 종료까지 모든 서비스 관리
* 대부분의 최신 리눅스 배포판에서 사용

> systemd는 리눅스 서버의 관리자 프로그램이다.

---

## 3. 서비스(Service)란 무엇인가

서비스란:

* 백그라운드에서 실행되는 프로그램
* 사용자의 직접 실행 없이 동작
* 필요 시 자동 재시작 가능
* 부팅 시 자동 실행 가능

대표적인 서비스 예시:

* nginx
* mysql
* redis
* docker
* 우리가 만든 서버 프로그램

---

## 4. systemd 기본 명령어

### 서비스 상태 확인

```bash
systemctl status nginx
```

### 서비스 시작 / 중지

```bash
systemctl start nginx
systemctl stop nginx
```

### 재시작 / 리로드

```bash
systemctl restart nginx
systemctl reload nginx
```

### 부팅 시 자동 실행 설정

```bash
systemctl enable nginx
systemctl disable nginx
```

---

## 5. systemd 서비스 파일 구조

systemd 서비스는 `.service` 파일로 정의한다.

### 파일 위치

```text
/etc/systemd/system/
```

### 기본 구조

```ini
[Unit]
Description=My App Service
After=network.target

[Service]
ExecStart=/usr/bin/java -jar /opt/app/app.jar
Restart=always
User=ubuntu
WorkingDirectory=/opt/app

[Install]
WantedBy=multi-user.target
```

---

## 6. systemd 서비스 파일 항목 설명

### [Unit]

* Description: 서비스 설명
* After: 이 서비스가 시작되기 전에 준비되어야 할 대상

### [Service]

* ExecStart: 실행 명령어
* Restart: 종료 시 재시작 정책
* User: 실행 사용자
* WorkingDirectory: 실행 기준 디렉터리

### [Install]

* WantedBy: 어느 런레벨에서 실행될지 설정

---

## 7. 실습: systemd 서비스 등록

### 1) 서비스 파일 생성

```bash
sudo vi /etc/systemd/system/demo-api.service
```

```ini
[Unit]
Description=Demo API Server
After=network.target

[Service]
ExecStart=/usr/bin/java -jar /home/ubuntu/demo/app.jar
Restart=always
User=ubuntu
WorkingDirectory=/home/ubuntu/demo

[Install]
WantedBy=multi-user.target
```

### 2) systemd 재로드

```bash
sudo systemctl daemon-reload
```

### 3) 서비스 시작 및 자동 등록

```bash
sudo systemctl start demo-api
sudo systemctl enable demo-api
```

### 4) 상태 확인

```bash
systemctl status demo-api
```

---

## 8. 서비스 로그 확인

```bash
journalctl -u demo-api
```

실시간 로그 확인:

```bash
journalctl -u demo-api -f
```

> 서버 장애 분석의 시작은 로그 확인이다.

---

## 9. 터미널 실행 vs systemd

| 항목     | 터미널 실행 | systemd |
| ------ | ------ | ------- |
| 터미널 종료 | 종료됨    | 계속 실행   |
| 서버 재부팅 | 실행 안 됨 | 자동 실행   |
| 장애 복구  | 수동     | 자동 재시작  |
| 운영 적합성 | 낮음     | 표준      |

---

## 10. 정리

* 서버 프로그램은 항상 실행되어야 한다
* systemd는 서버 운영의 기본 도구
* Docker를 사용하더라도 systemd 개념은 필수
* 실무 서버에서는 systemd와 Docker를 함께 사용한다

> systemd는 서버 개발자의 필수 운영 기본기다.
