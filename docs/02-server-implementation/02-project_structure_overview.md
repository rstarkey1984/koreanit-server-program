# 프로젝트 구조 개요

이 장에서는
앞 장에서 생성한 **Spring Boot 프로젝트의 전체 구조**를 한 번에 훑어본다.

이 단계의 목적은
각 파일의 세부 문법을 이해하는 것이 아니라,
**"어디에 무엇을 넣어야 하는지"라는 구조 감각**을 만드는 것이다.

---

## 1. 프로젝트 최상위 구조

프로젝트 루트에서 파일 목록을 확인한다.

```bash
ls
```

대표적인 구성은 다음과 같다.

```text
spring/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── gradle/
├── build/
├── src/
└── README.md (선택)
```

---

## 2. Gradle 관련 파일

### 2-1. build.gradle

* 프로젝트의 **빌드 설정 파일**
* 의존성(dependencies) 정의
* Java / Spring Boot 버전 관리

> 서버 실행 방식, 라이브러리 추가 여부는
> 모두 이 파일을 기준으로 결정된다.

---

### 2-2. gradlew

* Gradle Wrapper 실행 파일
* 로컬에 Gradle 설치 없이 **Gradle 작업(task)** 실행 가능

```bash
# 애플리케이션 실행 (빌드 아님)
./gradlew bootRun

# 전체 빌드 (컴파일 + 테스트 + jar 생성)
./gradlew build

# 실행 가능한 Spring Boot jar 생성
./gradlew bootJar
```

---

## 3. src 디렉터리 구조

```text
src/
├── main/
│   ├── java/
│   │   └── com/koreanit/spring/
│   │       └── Application.java
│   └── resources/
│       └── application.yml (또는 application.properties)
└── test/
```

---

## 4. main/java 영역

### 4-1. 패키지 구조

```text
com.koreanit.spring
```

이 패키지가
**서버 프로그램 코드의 시작점(root package)** 이다.

이 패키지를 기준으로
컴포넌트 스캔이 수행된다.

---

### 4-2. Application.java

* 서버 프로그램의 **진입점(entry point)**
* main 메서드를 포함
* Spring Boot 실행 시작 위치

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

> 이 파일은
> 서버 프로세스를 시작하는 역할만 담당한다.

---

## 5. main/resources 영역

### 5-1. application.yml / application.properties

* 서버 설정 파일
* 포트, DB 연결 정보, 로그 레벨 등 관리

> 현재 단계에서는 수정하지 않는다.
> 설정은 **필요해지는 시점에만 추가**한다.

---

## 6. test 영역

* 테스트 코드 작성 위치
* 현재 단계에서는 구조만 인지한다.

---

## 7. 앞으로 추가될 구조

이제부터 우리는
`com.koreanit.spring` 아래에
다음 구조를 단계적으로 추가한다.

```text
controller/
service/
repository/
```

각 계층은
**명확히 분리된 책임**을 가진다.

---

## 이 장의 핵심 정리

* Spring Boot 프로젝트는 정해진 기본 구조를 가진다
* 서버 코드는 main/java 아래에 작성한다
* 설정은 resources 아래에서 관리한다
* 구조 이해가 문법 이해보다 우선이다

---

## 다음 단계

→ [**간단한 서버 동작 확인 실습 (/health, Servlet)**](03-health_check.md)
