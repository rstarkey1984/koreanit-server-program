# nvm으로 Node.js 설치

본 문서는 **Node Version Manager(nvm)** 를 사용하여 Node.js를 설치하고 관리하는 표준 절차를 정리한다.
서버 환경 및 교육 실습 기준으로 바로 사용할 수 있는 흐름을 기준으로 한다.

---

## 1. nvm이란?

nvm(Node Version Manager)은 **Node.js 버전을 여러 개 설치하고 전환**할 수 있게 해주는 도구이다.

* 프로젝트별 Node 버전 관리 가능
* 시스템 Node와 분리된 환경 구성
* LTS / 특정 버전 간 전환 용이

실무 및 교육 환경에서는 **nvm 사용이 사실상 표준**이다.

---

## 2. 설치 대상 환경

* OS: WSL(Ubuntu)
* Shell: bash

---

## 3. nvm 설치

### 3-1. 설치 스크립트 실행

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
```

---

### 3-2. 쉘 설정 반영

설치 후 터미널을 **새로 열거나**, 아래 명령어를 실행한다.

bash 사용 시:

```bash
source ~/.bashrc
```

---

### 3-3. 설치 확인

```bash
nvm --version
```

정상 출력 예시:

```text
0.39.7
```

---

## 4. Node.js 설치

### 4-1. 설치 가능한 버전 확인

```bash
nvm ls-remote
```

---

### 4-2. LTS 버전 설치 (권장)

```bash
nvm install --lts
```

---

### 4-3. 특정 버전 설치 (예시)

```bash
nvm install 22.22.0
```


### 4-4. 사용가능한 node 버전 확인
```
nvm ls
```

---

## 5. Node 버전 사용 설정

### 5-1. 현재 사용할 버전 지정

```bash
nvm use 24.13.0
```

---

### 5-2. 기본 버전으로 설정

```bash
nvm alias default 24.13.0
```

---

### 5-3. 현재 사용 중인 버전 확인

```bash
node -v
npm -v
```

---

## 6. nvm 사용 여부 확인

```bash
which node
```

정상 출력 예시:

```text
~/.nvm/versions/node/v24.13.0/bin/node
```

이는 시스템 Node가 아닌 **nvm으로 설치된 Node를 사용 중임을 의미한다**.

---

## 7. 자주 사용하는 nvm 명령어

```bash
nvm ls            # 설치된 Node 목록
nvm current       # 현재 사용 중인 버전
nvm use --lts     # LTS 버전 사용
nvm uninstall 16  # 특정 버전 삭제
```

## 다음 단계

Node.js 워커 구현을 진행한다.

→ [**사전 준비 – 뉴스 테이블 스키마**](00-prerequisite_database_schema.md)
