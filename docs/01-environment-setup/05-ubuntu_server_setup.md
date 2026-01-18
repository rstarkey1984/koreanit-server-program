# Ubuntu 서버 기본 세팅

*(패키지 업데이트 · OpenSSH · VS Code 연동)*

이 장에서는
WSL에 설치된 Ubuntu를 **실제로 접속해서 사용하는 서버 환경**으로 만든다.

학생은 이 장을 끝내면
**SSH로 서버에 접속하고, VS Code로 서버 안에서 개발**할 수 있어야 한다.

---

## 강의 목표

* Ubuntu 패키지 관리 시스템을 이해한다
* 서버 초기 필수 업데이트를 수행할 수 있다
* OpenSSH 서버를 설치하고 실행할 수 있다
* Windows에서 SSH로 Ubuntu 서버에 접속할 수 있다
* VS Code Remote-SSH로 서버 개발 환경을 구성할 수 있다

---

## 1. Ubuntu 패키지 업데이트 (서버 기본)

서버를 처음 구성할 때 가장 먼저 해야 할 작업은
**패키지 목록 갱신과 보안 업데이트**다.

> 업데이트하지 않은 서버는
> “문 열어놓은 서버”와 같다.

---

### 1-1. 패키지 목록 업데이트

```bash
sudo apt update
```

* 설치 가능한 패키지 **목록만 갱신**
* 실제 파일 다운로드는 아님

---

### 1-2. 패키지 업그레이드

```bash
sudo apt upgrade -y
```

* 설치된 패키지를 최신 버전으로 업데이트
* 서버 초기 세팅 시 **반드시 1회 수행**

---

### (설명 포인트)

* `update` : 목록 갱신
* `upgrade` : 실제 파일 업데이트
* 서버에서는 **항상 같이 사용**

---

## 2. OpenSSH 서버 설치

이제 Ubuntu를
**“접속 가능한 서버”**로 만든다.

---

### 2-1. OpenSSH 서버 설치

```bash
sudo apt install -y openssh-server
```

---

### 2-2. SSH 서비스 상태 확인

```bash
sudo systemctl status ssh
```

정상 상태 예시:

```text
Active: active (running)
```

---

### 2-3. 부팅 시 자동 실행 설정

```bash
sudo systemctl enable ssh
```

* `systemd=true` 설정과 연결되는 부분
* 서버라면 **항상 자동 실행**

---

## 3. WSL Ubuntu 네트워크 특성 이해

IP 확인:

```bash
hostname -I
```

또는

```bash
ip addr
```

### 설명 포인트 (중요)

* WSL은 **가상 네트워크 환경**
* 재시작 시 IP가 바뀔 수 있음
* 실습 목적에서는 문제 없음
* 보통 **localhost SSH** 사용

---

## 4. Windows → Ubuntu SSH 접속

Windows PowerShell 또는 Windows Terminal에서 실행

```powershell
ssh 사용자명@localhost
```

예시:

```powershell
ssh ubuntu@localhost
```

정상 접속 시 프롬프트:

```bash
ubuntu@ubuntu24:~$
```

### 설명 포인트

* 지금부터 **서버에 접속해서 작업**
* GUI가 아닌 **터미널 기반 서버 작업**

---

## 5. VS Code Remote-SSH 연동 (체감 핵심)

이 단계에서 학생 체감이 확 바뀐다.

> “내 컴퓨터에서
> 서버 안으로 들어가서 개발한다”

---

### 5-1. VS Code 확장 설치 (Windows)

* Remote - SSH
* Remote Explorer

---

### 5-2. VS Code로 서버 접속

1. VS Code 실행
2. `Ctrl + Shift + P`
3. `Remote-SSH: Connect to Host`
4. `ssh 사용자명@localhost`

---

### 5-3. 서버 폴더 열기

```text
/home/사용자명
```

예:

```text
/home/ubuntu
```

이제부터:

* 터미널
* 파일 편집
* Git
* 서버 실행

모두 **서버 안에서 작업**

---

## 이 장의 핵심 정리

* Ubuntu는 이제 단순한 리눅스가 아니라 **서버**
* SSH는 서버 접속의 표준
* VS Code는 **서버 내부 개발 도구**
* 이후 모든 실습은 이 환경을 기준으로 진행

---

## 다음 단계

→ [**06. 필수 서버 패키지 설치**(curl, git, build-essential)](06_required_server_packages.md)
