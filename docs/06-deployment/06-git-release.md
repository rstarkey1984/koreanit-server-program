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

# 실습

## Git Clone 후 Spring Boot 애플리케이션 실행

```bash
git clone https://github.com/<GITHUB_ID>/<REPO_NAME>.git
```

```bash
cd <REPO_NAME>
```

```bash
./gradlew bootRun
```

---

# 포트폴리오 작성

`README.md`
```
1. 프로젝트 개요
2. 기술 스택 (Spring Boot, MySQL, Redis 등)
3. 실행 방법
   - 환경 변수
   - ./gradlew bootRun
4. 주요 기능
5. 프로젝트 구조 설명
```
