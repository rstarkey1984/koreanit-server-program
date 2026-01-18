# Git으로 서버 프로젝트 관리

*(서버 개발을 위한 형상관리 기본 흐름)*

이 장에서는
서버 개발 환경에서 **Git으로 프로젝트를 관리하는 기본 흐름**을 익힌다.

학생은 이 장을 끝내면
"서버에서 작업한 내용을 Git으로 관리하고 되돌릴 수 있다"는 감각을 가져야 한다.

---

## 강의 목표

* Git이 왜 서버 개발 환경에서 필수인지 이해한다
* 서버에서 Git 저장소를 초기화할 수 있다
* 변경 사항을 커밋으로 관리할 수 있다
* `.gitignore`의 역할을 이해한다

---

## 1. 서버에서 Git이 왜 필요한가?

서버에서 작업을 하다 보면:

* 설정 파일을 잘못 수정했을 때
* 이전 상태로 되돌리고 싶을 때
* 여러 실험을 반복할 때

**되돌릴 수 없는 작업**이 자주 발생한다.

Git은:

* 작업 이력을 기록하고
* 이전 상태로 되돌릴 수 있게 해주는
* **서버 개발의 안전장치**다.

---

## 2. Git 설치 확인

06장에서 이미 Git을 설치했다.

확인:

```bash
git --version
```

---

## 3. 서버 프로젝트 디렉터리 준비

서버 프로젝트를 위한 디렉터리를 하나 만든다.

```bash
mkdir -p ~/projects/koreanit-server
cd ~/projects/koreanit-server
```

> 이후 실습은 이 디렉터리 기준으로 진행한다.

---

## 4. Git 저장소 초기화

현재 디렉터리를 Git 저장소로 만든다.

```bash
git init
```

확인:

```bash
ls -a
```

`.git` 디렉터리가 생성되면
Git 저장소가 초기화된 것이다.

---

## 5. Git 기본 설정 (최초 1회)

커밋에 기록될 사용자 정보를 설정한다.

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

확인:

```bash
git config --list
```

---

## 6. 추적 대상과 상태 확인

현재 Git 상태를 확인한다.

```bash
git status
```

아직 추적 중인 파일이 없음을 확인한다.

---

## 7. .gitignore 작성

Git으로 관리하지 않을 파일을 지정한다.

```bash
nano .gitignore
```

예시:

```text
# log files
logs/
*.log

# build output
build/
dist/

# environment
.env
```

---

## 8. 첫 번째 커밋

파일을 Git에 추가하고 커밋한다.

```bash
git add .
git commit -m "Initial commit"
```

이 시점이
**프로젝트의 기준선**이다.

---

## 9. 변경 사항 관리 흐름

이후 작업 흐름은 항상 같다.

```text
파일 수정
→ git status
→ git add
→ git commit
```

Git은
서버 작업을 안전하게 진행하기 위한
**기본 습관**이다.

---

## 이 장의 핵심 정리

* Git은 서버 개발의 안전장치다
* 모든 작업은 커밋 단위로 관리한다
* .gitignore로 불필요한 파일을 제외한다
* "되돌릴 수 있다"는 감각이 중요하다

---

## 다음 단계

→ [**서버 프로그램 구현하기**](/docs/02-server-implementation/README.md)