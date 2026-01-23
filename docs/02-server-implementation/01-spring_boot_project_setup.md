# Spring Boot 프로젝트 생성 및 실행


## 이 장의 목적

1. Spring Boot 기반 **WAS 서버를 표준 방식으로 생성**한다
2. IDE에 의존하지 않고 **서버 환경에서도 재현 가능한 프로젝트 구성**을 만든다
3. 이후 모든 강의의 **기준 프로젝트 구조**를 확립한다

---

## VS Code 확장 설치

1. Extension Pack for Java

> 확장은 코드 작성·실행을 돕는 도구일 뿐,
> **프로젝트 생성 방식이나 서버 구조에는 영향을 주지 않는다**

---

## 1. 프로젝트 생성 기준

### 표준 생성 방식

1. **Spring Initializr ZIP 생성 방식** 사용
2. [https://start.spring.io](https://start.spring.io)

### 이 방식을 사용하는 이유

1. IDE 종속 없음
2. 서버 터미널 환경에서도 동일하게 재현 가능
3. Spring Boot 공식 템플릿 기반

---

## 2. 프로젝트 기본 스펙

| 항목              | 값               |
| --------------- | --------------- |
| Build Tool      | Gradle (Groovy) |
| Language        | Java            |
| Java Version    | 17 (LTS)        |
| Spring Boot     | 3.x             |
| Packaging       | Jar             |
| Group           | com.koreanit    |
| Artifact / Name | spring          |

---

## 3. 프로젝트 생성 절차 요약

### 3-1. 작업 디렉터리 준비

```bash
mkdir -p ~/projects/koreanit-server
cd ~/projects/koreanit-server
```

### 3-2. Spring Initializr에서 ZIP 다운로드

### 3-3. 서버(VS Code)로 `spring.zip` 업로드

### 3-4. 압축 해제

```bash
unzip spring.zip
```

---

## 4. 생성된 프로젝트 기본 구조

```text
spring/
├── build.gradle        # 프로젝트 빌드 설정 파일
├── settings.gradle     # Gradle 프로젝트 이름 설정
├── gradlew             # Gradle Wrapper (Linux / macOS)
├── gradlew.bat         # Gradle Wrapper (Windows)
├── gradle/             # Gradle Wrapper 관련 파일
├── build/              # 빌드 결과물 (자동 생성)
└── src/                # 실제 서버 프로그램 소스 코드
```

---

## 5. Gradle 관련 핵심 파일

### 5-1. build.gradle

1. 프로젝트 **빌드 설정 파일**
2. 의존성(dependencies) 관리
3. Java / Spring Boot 버전 기준 관리

> 서버 동작 방식은 이 파일을 기준으로 결정된다

### 5-2. gradlew (Gradle Wrapper)

1. 로컬 Gradle 설치 없이 빌드 및 실행 가능

주요 명령어:

```bash
./gradlew bootRun   # 서버 실행
./gradlew build     # 전체 빌드
./gradlew bootJar   # 실행 가능한 jar 생성
```

---

## 6. src 디렉터리 구조

```text
src/
├── main/
│   ├── java/                     # 동적 요청 처리 영역 (서버 로직)
│   │   └── com/koreanit/spring/
│   │       ├── Application.java  # 서버 실행 진입점
│   │       ├── controller/       # HTTP 요청/응답 처리
│   │       │   └── HealthController.java
│   │       ├── service/          # 비즈니스 로직
│   │       │   └── HealthService.java
│   │       └── repository/       # 데이터 접근 (DB)
│   │           └── HealthRepository.java
│   │
│   └── resources/                # 정적 리소스 & 설정 파일
│       ├── static/               # 정적 파일 (그대로 응답)
│       │   ├── index.html        # GET / → 자동 반환
│       │   ├── css/
│       │   │   └── style.css
│       │   ├── js/
│       │   │   └── app.js
│       │   └── images/
│       │       └── logo.png
│       │
│       ├── templates/            # (선택) 서버 렌더링 뷰
│       │   └── hello.html
│       │
│       └── application.yml       # 서버 설정 파일
│
└── test/                          # 테스트 코드
    └── java/
```

---

## 7. main/java 영역

### 7-1. Root Package

```text
com.koreanit.spring
```

1. 서버 프로그램 코드의 시작점
2. 컴포넌트 스캔 기준 패키지
> 컴포넌트 스캔 기준이란? Spring이 서버 시작 시, 자동으로 객체(Bean)를 만들 클래스를 찾는 과정

### 7-2. Application.java 역할

> Application 클래스는 서버를 시작시키는 역할만 담당하며,
> 요청 처리나 비즈니스 로직은 절대 작성하지 않는다.

1. 서버 프로그램의 **진입점(entry point)**
2. `main()` 메서드 포함
3. Spring Boot 실행 트리거 역할만 담당

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

> `SpringApplication.run()` 은 Spring 컨테이너를 만들고, 필요한 Bean을 전부 준비한 뒤, 내장 웹 서버를 띄워 요청을 받을 수 있는 상태로 만든다.

---

## 8. main/resources 영역

### 8-1. application.yml / application.properties

1. 서버 설정 파일
2. 포트, DB 연결 정보, 로그 레벨 관리

> 현재 단계에서는 수정하지 않는다
> 설정은 **필요해지는 시점에만 추가**한다

---

## 9. Spring Boot 서버 실행

### 9-1. 실행 명령

```bash
./gradlew bootRun
```

### 9-2. 정상 실행 로그 기준

```text
Tomcat started on port(s): 8080
Started SpringServerApplication
```

### 9-3. 실행 확인

```text
http://localhost:8080
```

---

## 10. API 서버 기본 화면 구성

1. `http://localhost:8080` 접속 시
   **API 서버가 정상 실행 중임을 확인하는 기본 화면** 제공
2. 목적

   * 서버 실행 여부 즉시 확인
   * API 서버임을 명확히 표시
   * Controller / DB 없이 동작 구조 이해

---

## 11. 정적 리소스 동작 원리

```text
src/main/resources/static/
```

1. Spring Boot는 위 경로를 정적 리소스로 자동 인식한다
2. `index.html`이 존재하면 `GET /` 요청 시 자동 반환된다

---

## 12. 기본 화면 파일 위치

```text
src/main/resources/static/index.html
```
예제파일: [index.html](/koreanit-server/spring/src/main/resources/static/index.html)

---

## 13. 실행 확인 절차

1. 서버 실행

```bash
./gradlew bootRun
```

2. 브라우저 접속

```text
http://localhost:8080
```

3. API 서버 안내 화면 표시 → 정상 동작

* `static/index.html`은 루트(`/`)의 기본 응답
* API 서버의 **생존 신호** 역할
* 아직 Controller 로직은 사용하지 않는다

---

## 이 장의 핵심 포인트

1. Spring Boot 프로젝트는 **정해진 기본 구조**를 가진다
2. 서버 코드는 `main/java`
3. 설정은 `main/resources`
4. **구조 이해가 문법 이해보다 우선**이다

---

## 다음 단계

→ [**02. 간단한 서버 동작 확인 실습**](02-health_check.md)