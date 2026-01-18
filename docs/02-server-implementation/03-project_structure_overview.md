# 프로젝트 구조 개요

이 장에서는
앞 장에서 생성한 **Spring Boot 프로젝트의 전체 구조**를 한 번에 훑어본다.

이 단계의 목적은
각 파일의 세부 문법을 이해하는 것이 아니라,
**"어디에 무엇을 넣어야 하는지"라는 구조 감각**을 만드는 것이다.

---

## 강의 목표

* Spring Boot 프로젝트의 기본 디렉터리 구조를 이해한다
* 각 구성 요소의 역할을 개략적으로 설명할 수 있다
* 이후 Controller / Service / Repository 계층 학습을 위한 기준을 잡는다

---

## 1. 프로젝트 최상위 구조

프로젝트 루트에서 파일 목록을 확인한다.

```bash
ls
```

대표적인 구성은 다음과 같다.

```text
spring-server/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── src/
└── README.md (선택)
```

---

## 2. Gradle 관련 파일

### 2-1. build.gradle

* 프로젝트의 **빌드 설정 파일**
* 의존성(dependencies) 정의
* Java / Spring Boot 설정

> 서버 실행, 라이브러리 추가는
> 모두 이 파일을 통해 관리된다.

---

### 2-2. gradlew

* Gradle Wrapper 실행 파일
* 로컬에 Gradle 설치 없이 빌드 가능

```bash
./gradlew bootRun
```

---

## 3. src 디렉터리 구조

```text
src/
├── main/
│   ├── java/
│   │   └── com/koreanit/server/
│   │       └── SpringServerApplication.java
│   └── resources/
│       └── application.yml (또는 application.properties)
└── test/
```

---

## 4. main/java 영역

### 4-1. 패키지 구조

```text
com.koreanit.server
```

이 패키지가
**서버 프로그램 코드의 시작점**이다.

---

### 4-2. SpringServerApplication.java

* 서버 프로그램의 **진입점(entry point)**
* main 메서드를 포함
* Spring Boot 실행 시작 위치

```java
@SpringBootApplication
public class SpringServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringServerApplication.class, args);
    }
}
```

> 이 파일은
> 서버 프로세스를 시작하는 역할만 한다.

---

## 5. main/resources 영역

### 5-1. application.yml / application.properties

* 서버 설정 파일
* 포트, DB 연결 정보, 로그 설정 등

> 아직은 수정하지 않는다.
> 설정은 **필요할 때** 다룬다.

---

## 6. test 영역

* 테스트 코드 작성 위치
* 이번 강의에서는
  기본 구조만 인지하고 넘어간다.

---

## 7. 앞으로 추가될 구조

이제부터 우리는
`com.koreanit.server` 아래에
다음 구조를 단계적으로 추가한다.

```text
controller/
service/
repository/
```

각 계층은
**명확히 분리된 역할**을 가진다.

---

## 이 장의 핵심 정리

* Spring Boot 프로젝트는 정해진 기본 구조를 가진다
* 코드는 main/java 아래에 작성한다
* 설정은 resources 아래에서 관리한다
* 구조를 이해하는 것이 문법보다 중요하다

---

## 다음 단계

→ [**04. Controller 계층 구현**](04-controller_layer.md)
