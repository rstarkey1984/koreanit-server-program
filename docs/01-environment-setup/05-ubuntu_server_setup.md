# Ubuntu 서버 개발 환경 구축

*(패키지 업데이트 · OpenSSH · VS Code 연동)*

이 장에서는
WSL에 설치된 Ubuntu를 **실제로 접속해서 사용하는 서버 환경**으로 만든다.

이 장을 끝내면 **SSH로 서버에 접속하고, VS Code로 서버 안에서 개발**할 수 있다.


---

## 1. Ubuntu 패키지(package) 업데이트
> Ubuntu 패키지는 Canonical이 공식 저장소를 통해 관리·배포하고, 사용자는 apt로 설치한다.    
> 서버를 처음 구성할 때 가장 먼저 해야 할 작업은 **패키지 목록 갱신과 보안 업데이트**다.

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


## 패키지(package)란?

> 프로그램 실행에 필요한 **실행 파일, 설정 파일, 라이브러리**를 하나로 묶어 배포하는 단위다.  
> Ubuntu는 `apt` 패키지 관리자를 통해 패키지를 설치·업데이트·삭제하며, 의존성을 자동으로 관리한다.

## APT ( Advanced Package Tool ) 란?

> `apt`는 Ubuntu에서 소프트웨어 패키지를 **설치·업데이트·삭제**하고  
> 필요한 의존성까지 함께 관리해 주는 패키지 관리 도구다.

## 필수 패키지 설치
```
sudo apt install -y curl build-essential openjdk-17-jdk unzip
```
> `curl`(네트워크 요청), `build-essential`(컴파일러·빌드 도구 묶음)을 설치해 소스 다운로드·빌드·개발 환경을 준비한다.

---

## 2. OpenSSH 서버 설치

> OpenSSH 서버는 원격에서 서버에 안전하게 접속하기 위해 사용하는 표준 SSH 서비스로, 설치하면 외부 PC에서 터미널로 Ubuntu 서버에 접속할 수 있다.

### 2-1. OpenSSH 서버 설치

```bash
sudo apt install -y openssh-server
```

---

### 2-2. SSH 서비스 상태 확인

```bash
sudo systemctl status ssh
```
> systemctl 은 리눅스에서 systemd 기반 서비스(데몬)를 시작·중지·상태 확인·자동실행 설정까지 관리하는 명령어다.

정상 상태 예시:

```text
Active: active (running)
```


---

## 3. Windows → Ubuntu SSH 접속
> SSH 접속이란, SSH 클라이언트 프로그램이 원격 서버에서 실행 중인 SSH 서버(sshd)에 접속하는 것을 의미한다.

`Windows PowerShell` 또는 `Windows Terminal`에서 실행

```powershell
ssh 사용자명@도메인orIP
```

예시:

```powershell
ssh ubuntu@localhost
```
실행 흐름
> ssh 접속 = 네트워크 로그인 → sshd가 세션 생성 → 계정에 설정된 로그인 쉘 실행

### 정상 접속 시 프롬프트:

```bash
ubuntu@ubuntu24:~$
```






---

## 5. VS Code Remote-SSH 연동 

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

### 5-3. VS Code 탐색기로 서버 폴더 열기

```text
/home/ubuntu/projects/koreanit-server
```

---

## 이 장의 핵심 정리

* OpenSSH 서버를 설치함으로써 Ubuntu는 외부에서 접속해 관리하는 **서버 환경으로 사용된다.**

* SSH는 서버 접속의 표준

* VS Code는 **서버 내부로 접속해 개발하는 도구**

* 이후 모든 실습은 이 환경을 기준으로 진행됨

---

## ( 선택 ) 비밀번호 입력 없이 SSH 서버 접속하기 

### 1. Windows PowerShell에서 SSH 키페어 생성
> ssh-keygen은 SSH 접속에 사용할 공개키·개인키를 생성하는 키 생성 도구
```powershell
ssh-keygen -C "ubuntu24" -f "$env:USERPROFILE\.ssh\myfirstkey"
```
passphrase 입력하라고 뜨는데, 무시하고 두번 엔터    
( passphrase는 비밀키를 보호하기 위해 사용하는 추가 비밀번호 )

### 키 생성 확인:
```powershell
ls ~/.ssh/
```

### 2. 공개키를 서버 임시폴더에 업로드
```powershell
scp $env:USERPROFILE\.ssh\myfirstkey.pub ubuntu@localhost:/tmp/myfirstkey.pub
```
> `scp`: Secure Copy. SSH(암호화) 기반으로 **파일을 원격 서버로 전송**하는 명령어

### 3. SSH 접속용 공개키를 서버에 등록하고, 보안 권한까지 맞게 설정한 뒤 임시 파일을 제거하는 1회용 키 등록 스크립트.

```Powershell
ssh ubuntu@localhost "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat /tmp/myfirstkey.pub >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && rm /tmp/myfirstkey.pub"
```

### 4. Windows OpenSSH config 파일 설정

```Powershell
code $env:USERPROFILE/.ssh/config
```

`config` 파일내용
```
Host ubuntu24
  HostName localhost
  Port 22
  IdentityFile C:\Users\사용자이름\.ssh\myfirstkey
  User ubuntu
```

> 여기서 ubuntu24 는 SSH 접속 설정에 붙이는 별칭이며, 실제 서버 이름이나 사용자 이름과는 무관하다.

### 5. 윈도우 터미널에서 별칭으로 접속
```powershell
ssh ubuntu24
```


### 6. VSCode 에서 별칭으로 접속
![alt text](/img/vscode-remote-explorer1.png?1)

---

## 다음 단계

→ [**06. Ubuntu 서버 맛보기**](06-ubuntu_server_basic_usage.md)


---