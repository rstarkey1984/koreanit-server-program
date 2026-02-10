# 포트폴리오 작성 가이드

본 문서는 **GitHub 포트폴리오 제출 기준**으로,
`README.md`와 `GitHub Release`를 어떻게 작성해야 하는지 정리한다.

---

## 1. GitHub 저장소 – README.md 작성

`README.md`는 **프로젝트를 처음 보는 사람이 이해하기 위한 문서**다.
면접관, 강사, 협업자가 가장 먼저 확인하는 문서이므로
구조와 설명이 명확해야 한다.

### README.md 필수 구성

```text
1. 프로젝트 개요
2. 기술 스택
3. 프로젝트 구조
4. 주요 기능
5. 실행 방법
```

---

### 1) 프로젝트 개요

* 프로젝트 목적
* 어떤 문제를 해결하는지
* 개인 프로젝트 / 팀 프로젝트 여부

예시:

```text
Spring Boot 기반 REST API 서버로,
User / Post / Comment 도메인을 중심으로
서버 애플리케이션의 구조와 인증 흐름을 학습하기 위한 프로젝트입니다.
```

---

### 2) 기술 스택

단순 나열이 아니라 **선택 이유가 드러나도록** 작성한다.

예시:

```text
- Java 17
- Spring Boot
- Spring Security (세션 기반 인증)
- JDBC / JdbcTemplate
- MySQL
- Redis (선택: 세션 스토어)
- Gradle
```

---

### 3) 프로젝트 구조

* 패키지 구조 설명
* 계층 분리 원칙
* 각 계층의 책임

핵심 구조:

```text
Controller → Service → Repository
Entity → Domain → DTO
```

> 구조 설명은 포트폴리오 평가에서 매우 중요한 요소다.

---

### 4) 주요 기능

도메인 기준으로 기능을 정리한다.

예시:

```text
[User]
- 회원 가입
- 로그인 / 로그아웃
- 세션 기반 인증

[Post]
- 게시글 목록 조회
- 게시글 작성

[Comment]
- 댓글 작성
- 게시글별 댓글 조회
```

---

### 5) 실행 방법

클론 후 **바로 실행 가능해야 한다**.

* Java 버전 명시
* DB 준비 방법
* 실행 명령어

예시:

```bash
./gradlew bootRun
```

또는 JAR 실행 방식까지 포함하면 가산점이 있다.

---

## 2. GitHub Release 작성

Release는 README의 복사본이 아니다.

> **해당 버전에서 무엇이 완성되었는지 요약하는 공간**이다.

---

### Release 필수 구성

```text
- 버전 설명 (예: v1.0.0)
- 주요 기능 요약
- 실행 방법 요약
- 실행 산출물(JAR) 첨부
```

---

### Release 작성 예시

```text
## v1.0.0 – Initial Release

### 주요 기능
- User / Post / Comment REST API 구현
- Controller–Service–Repository 계층 분리
- 세션 기반 인증
- 공통 응답 및 예외 처리

### 실행 방법
- Java 17 이상
- 첨부된 JAR 파일 실행
  java -jar koreanit-api-1.0.0.jar

### 참고
- 상세 설명은 README.md 참고
```

---

### Release Assets 권장 구성

* 실행 가능한 JAR 파일
* (선택) 테스트용 client.html
* Source code (GitHub 자동 제공)

---

## 3. README vs Release 한 줄 정리

> **README는 프로젝트 설명서이고,
> Release는 해당 버전의 결과물 요약이다.**

---

## 4. 포트폴리오 제출안내

```text
1. GitHub 저장소에 README.md를 작성한다.
   - 프로젝트 개요
   - 기술 스택
   - 프로젝트 구조
   - 주요 기능
   - 실행 방법을 포함할 것

2. v1.0.0 태그로 GitHub Release를 생성한다.
   - 구현 완료된 기능 요약
   - 실행 방법 요약
   - 실행 가능한 JAR 파일을 첨부할 것
   - 간단한 실행 스샷
```
