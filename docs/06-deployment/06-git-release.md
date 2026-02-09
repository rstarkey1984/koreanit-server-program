# Git 초기 설정 & 저장소 생성 절차

본 문서는 **WSL / 서버 환경에서 Spring 프로젝트를 Git 저장소로 초기화**하고
**안전하게 GitHub에 업로드하기 위한 표준 절차**를 정리한다.

---

## 1. 프로젝트 초기화 및 기본 설정

Git 저장소는 프로젝트 디렉터리에서 초기화한다.

```bash
cd ~/projects/koreanit-server/spring
git init
```

저장소 초기화 이후,
해당 프로젝트에서 사용할 **커밋 작성자 정보(Local)** 를 설정한다.

```bash
git config user.name "testuser"
git config user.email "deploy@koreanit.io"
```

설정 확인:

```bash
git config user.name
git config user.email
```

---

## 2. 서버 / 운영 환경 권장 기준

### 원칙

* 서버는 **사람이 아닌 하나의 주체**로 취급한다
* 커밋 작성자는 **개인 계정이 아닌 프로젝트 단위로 식별**되어야 한다

### 권장 설정

* Global: 개인 기본값 유지 또는 미설정
* Local: **반드시 설정**

```bash
git config user.name "testuser"
git config user.email "deploy@koreanit.io"
```

---

## 3. GitHub에 저장소(Repository) 생성

### 1) GitHub에서 새 저장소 만들기

1. GitHub 접속 → **New repository**
2. 설정

   * **Repository name**: 예) `koreanit-server-spring`
   * **Public / Private** 선택
   * **Add README**: `Off`
3. **Create repository** 클릭

> 이미 로컬 프로젝트가 존재하므로
> GitHub에서 README를 생성하지 않는다.

---

## 4. 로컬 프로젝트와 GitHub 원격 저장소 연결

### 1) Git 저장소 상태 확인

```bash
git status
```

### 2) 원격 저장소(origin) 추가 (HTTPS 방식)

```bash
git remote add origin https://github.com/<GITHUB_ID>/<REPO_NAME>.git
```

예시:

```bash
git remote add origin https://github.com/rstarkey1984/koreanit-server-spring.git
```

> 서버 / WSL 환경에서는
> **OAuth 로그인 불필요한 HTTPS 방식**을 기본으로 사용한다.

### 3) 원격 연결 확인

```bash
git remote -v
```

---

## 5. 최초 커밋 준비

### 1) `.gitignore` 확인 / 작성

```gitignore
HELP.md
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/


### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/
!**/src/main/**/bin/
!**/src/test/**/bin/

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

### VS Code ###
.vscode/

### env
.env
```

### 2) 파일 추가

```bash
git add .
```

### 3) 최초 커밋

```bash
git commit -m "first commit"
```

---

## 6. 기본 브랜치 설정 및 최초 Push

### 1) 기본 브랜치를 `main`으로 통일

```bash
git branch -M main
```

### 2) GitHub로 push

```bash
git push -u origin main
```

> 이후부터는 `git push` 만으로 업로드 가능

---

## 7. 업로드 결과 확인

### GitHub에서 확인

* 저장소 파일 목록 정상 표시
* 커밋 히스토리 확인
* 기본 브랜치: `main`

---

## 8. 태그(Tag)와 릴리즈(Release) 개념

### 8-1. 태그(Tag)란?

태그는 **특정 커밋을 버전 기준점으로 고정하는 표시**다.

* “이 커밋이 v1.0.0이다”라고 **이름을 붙여두는 것**
* 코드가 계속 변경되어도, 태그가 찍힌 커밋은 **항상 동일하게 재현 가능**
* Git 자체 기능이며 GitHub 없이도 존재

> 태그 = **버전이 붙은 커밋**

---

### 8-2. 릴리즈(Release)란?

릴리즈는 **태그를 기준으로 GitHub에서 공개하는 배포 단위 설명 페이지**다.

* 태그를 선택해 “이 버전이 배포본이다”라고 명시
* 변경 사항(릴리즈 노트)을 사람에게 전달하기 위한 목적
* 필요 시 실행 파일(JAR 등)을 첨부할 수 있음

> 릴리즈 = **태그를 사람에게 설명하는 GitHub 화면**

---

### 8-3. 태그와 릴리즈의 관계

| 구분    | 태그(Tag)   | 릴리즈(Release) |
| ----- | --------- | ------------ |
| 위치    | Git 자체    | GitHub       |
| 대상    | 개발자 / 시스템 | 사용자 / 팀      |
| 역할    | 버전 기준점 고정 | 배포 버전 설명     |
| 필수 여부 | 필수        | 선택           |

핵심 흐름:

> **커밋 → 태그 → (선택) 릴리즈**

---

### 8-4. 버전 이름 규칙 (개념)

일반적으로 다음 형태를 사용한다.

```
vMAJOR.MINOR.PATCH
```

* `v1.0.0` : 첫 정식 배포
* `v1.1.0` : 기능 추가
* `v1.0.1` : 버그 수정

> **의미 있는 배포 시점마다 숫자를 올린다**

정도로 이해하면 충분하다.

---

### 8-5. 태그 / 릴리즈 작성 기준

* GitHub 저장소에는 **소스 코드만 관리**
* 배포 가능한 상태마다 태그로 버전 고정
* GitHub Release에는 다음만 작성

  * 버전 번호
  * 주요 변경 사항 요약

> **태그가 핵심이고, 릴리즈는 설명서다**

---

# 실습 (태그 기준 Clone & 실행)

### 실습 목표

* 최신 코드가 아닌 **특정 배포 버전(태그)** 기준으로 코드를 내려받는다
* 태그가 왜 "배포 기준점"인지 직접 체감한다

---

### 실습 1. 태그 기준으로 Git Clone

태그는 커밋의 기준점이므로,
**태그를 지정해 clone 하면 해당 버전의 코드만 내려받는다.**

```bash
git clone --branch v1.0.0 https://github.com/<GITHUB_ID>/<REPO_NAME>.git
```

> 이 방식은 `v1.0.0` 태그가 찍힌 시점의 코드 상태를 그대로 가져온다.
> 이후 `main` 브랜치에 변경이 있어도 영향을 받지 않는다.


---

### 실습 2. 프로젝트 실행

```bash
cd <REPO_NAME>
```

```bash
./gradlew bootRun
```

---

### 실습 핵심 포인트

* `main` 브랜치는 **계속 변하는 개발 흐름**이다
* 태그(tag)는 **절대 변하지 않는 배포 기준점**이다
* 운영 / 실습 / 재현은 항상 **태그 기준**으로 진행한다


---

# 포트폴리오 작성

`README.md`
```
1. 프로젝트 개요
2. 기술 스택 (Spring Boot, MySQL, Redis 등)
3. 프로젝트 구조
4. 주요 기능
5. 실행 방법
```
