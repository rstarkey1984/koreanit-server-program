# WSL2 + Ubuntu 24.04 환경 구성

> 이 장은 Windows 환경에서 실제 서버와 거의 동일한 Linux 서버 환경을 구성하기 위해 WSL2 + Ubuntu를 사용하는 방법에 집중한다.

공식설치가이드 - https://learn.microsoft.com/ko-kr/windows/wsl/install

> **(중요) WSL2 사용하려면 Windows 10 22H2 버전 이상 필요**

---

## 1. Windows에 WSL2 설치

> WSL(Windows Subsystem for Linux)은
**Windows 안에서 Linux 환경을 그대로 실행**할 수 있게 해주는 기능이다.

### 1-1. PowerShell 관리자 실행

> Windows 검색 → PowerShell → 우클릭 → 관리자 권한으로 실행

---

### 1-2. Microsoft-Windows-Subsystem-Linux 기능 활성화

```powershell
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
```

---

### 1-3. VirtualMachinePlatform 기능 활성화

```powershell
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
```

WSL2, Docker Desktop 등은 **하드웨어 가상화 기능이 필수**다.

---

### 1-4. WSL 기본 버전을 WSL2로 설정

리눅스 커널 업데이트:
```powershell
wsl --update
```

wsl 기본 버전을 wsl2로 설정:
```powershell
wsl --set-default-version 2
```

### 1-5. WSL Ubuntu 기본 배포판 설치:
```powershell
wsl --install
```


### 1-6. Ubuntu 24.04 설치:

```powershell
wsl --install -d Ubuntu-24.04
```

### 1-7. WSL 배포판 설치확인:
```powershell
wsl -l -v
```

### 1-8. WSL 기본 실행 배포판 설정:
```powershell
wsl -s Ubuntu-24.04
```

### (선택) WSL 실행 명령 도움말:
```powershell
wsl --help
```

---

## 2. Ubuntu 24.04 초기 설정

### 2-1. 계정 생성

Ubuntu 최초 실행 시 사용자 이름과 비밀번호를 설정한다.

* 사용자명은 소문자만 가능
* 비밀번호 입력 시 화면에 보이지 않음

---

### 2-2. /etc/wsl.conf 설정 (중요)

WSL 환경을 **서버용으로 사용하기 위한 핵심 설정**이다.

```bash
sudo sh -c 'cat > /etc/wsl.conf << "EOF"
[boot]
systemd=true

[user]
default=ubuntu

[automount]
enabled=false

[interop]
enabled=false
appendWindowsPath=false

[network]
hostname=ubuntu24
EOF'
```

* systemd=true : 서버 서비스 자동 실행
* automount/interop 비활성화 : Windows 환경과 분리
* hostname : 서버 식별용

확인:

```bash
cat /etc/wsl.conf
```
접속종료:
```bash
exit
```

---

### 2-3. WSL 재시작

```powershell
wsl -l -v
wsl --shutdown
wsl
```

정상 프롬프트 예:

```bash
ubuntu@ubuntu24:~$
```

### 프로젝트 폴더 생성

```bash
mkdir -p ~/projects/koreanit-server
cd ~/projects/koreanit-server
```

## 윈도우 터미널에서 wsl 실행시 전체 흐름

```text
[ Windows 터미널 ]
        ↓
      wsl
        ↓
[ wsl.exe (Windows 프로그램) ]
        ↓
[ 리눅스 배포판 기동 ]
        ↓
[ 로그인 쉘 실행 ]
        ↓
[ /etc/profile → ~/.profile 자동 실행 ]
        ↓
[ 리눅스 쉘 사용 ] → ubuntu@ubuntu24:~$
```

## ( 선택 ) PowerShell 7 업그레이드
```
https://learn.microsoft.com/ko-kr/powershell/scripting/install/install-powershell-on-windows?view=powershell-7.4&utm_source=chatgpt.com
```


---

## 다음 단계

→ [**Ubuntu 서버 기본 세팅**](05-ubuntu_server_setup.md)


