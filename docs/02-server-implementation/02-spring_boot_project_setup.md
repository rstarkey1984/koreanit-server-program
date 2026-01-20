# Spring Boot 프로젝트 생성 및 실행

이 장에서는
**Spring Boot 기반 서버 프로젝트를 표준 방식으로 생성하고 실행**한다.

앞 장에서 정리한
웹 서버(Web Server)와 WAS의 역할 구분을 바탕으로,
이제 **실제로 동작하는 서버 프로그램(WAS)** 을 만든다.


---

## 1. 프로젝트 생성 방식 기준

이 강의에서는
**Spring Initializr를 이용한 ZIP 생성 방식**을 표준으로 사용한다.

이 방식은:

* IDE에 의존하지 않는다
* 서버 환경에서 그대로 재현 가능하다
* Spring Boot 공식 템플릿을 사용한다

> 이후 모든 실습은
> 이 프로젝트를 기준으로 진행한다.

---

## 2. 프로젝트 기본 스펙 (고정)

| 항목              | 값                   |
| --------------- | ------------------- |
| Build Tool      | Gradle (Groovy)     |
| Language        | Java                |
| Java Version    | 21 (LTS)            |
| Spring Boot     | 3.x                 |
| Packaging       | Jar                 |
| Group           | com.koreanit        |
| Artifact / Name | spring-server       |
| Package         | com.koreanit.server |

---

## 3. 프로젝트 디렉터리 준비

서버 프로젝트를 저장할 디렉터리를 만든다.

```bash
mkdir -p ~/projects/koreanit-server
cd ~/projects/koreanit-server
```

---

## 4. Spring Initializr로 프로젝트 생성 (CLI 방식)

WSL 환경에서
Spring Initializr를 사용해 프로젝트 ZIP을 생성한다.

```bash
curl -L "https://start.spring.io/starter.zip \
?type=gradle-project \
&language=java \
&bootVersion=3.4.1 \
&javaVersion=21 \
&groupId=com.koreanit \
&artifactId=spring-server \
&name=spring-server \
&packageName=com.koreanit.server \
&packaging=jar \
&dependencies=web,validation,jdbc,mysql" \
-o spring-server.zip
```

압축 해제:

```bash
unzip spring-server.zip -d spring-server
rm spring-server.zip
```

---

## 5. 생성된 프로젝트 구조 확인

```bash
cd spring-server
ls
```

주요 파일:

```text
build.gradle
settings.gradle
gradlew
gradlew.bat
src/
```

> 이 프로젝트 구조는
> 이후 강의 전체에서 기준이 된다.

---

## 6. Spring Boot 서버 실행

Gradle Wrapper를 사용해 서버를 실행한다.

```bash
./gradlew bootRun
```

정상 실행 시 로그에 다음과 같은 메시지가 출력된다.

```text
Tomcat started on port(s): 8080
Started SpringServerApplication
```

---

## 7. 실행 확인

브라우저에서 다음 주소로 접속한다.

```text
http://localhost:8080
```

아직 컨트롤러를 만들지 않았기 때문에
기본 오류 페이지가 표시된다.

이 상태는 **정상**이다.

> 중요한 것은
> 서버 프로세스가 정상적으로 실행되고 있다는 점이다.

---

## 8. 지금 단계의 핵심 의미

이 시점에서 우리는:

* 웹 서버(Nginx) 뒤에서
* 실제로 실행되는 WAS를 만들었고
* 코드 기반 서버가 동작하는 환경을 준비했다

이제부터는
**이 서버에 기능을 하나씩 추가**하게 된다.

---

## 이 장의 핵심 정리

* Spring Boot 프로젝트는 공식 템플릿으로 생성한다
* IDE 없이 서버 환경에서 실행할 수 있다
* 이 프로젝트가 이후 모든 실습의 기준이다

---

## 다음 단계

→ [**03. 프로젝트 구조 개요**](03-project_structure_overview.md)
