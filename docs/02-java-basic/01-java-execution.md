# 자바 실행

이 강의는 **Spring Boot에 들어가기 전**, 서버 개발에 필요한 **자바 기본 구조와 실행 흐름**을 직접 만들어 보며 이해하는 것을 목표로 한다.

---

## 1. 자바 프로젝트 시작

이번 실습에서는 **빌드 도구(Gradle, Maven) 없이**
가장 순수한 형태의 자바 프로젝트를 생성한다.

* 자바 프로그램의 최소 실행 단위를 이해한다
* IDE 설정에 의존하지 않고 구조를 파악한다
* 이후 Spring Boot 구조와 자연스럽게 연결한다

---

## 2. 프로젝트 구조 (순수 자바)

```text
java-practice/
└─ src/
    └─ App.java
```

* `src` : 자바 소스 코드가 위치하는 디렉터리
* `App.java` : 프로그램 시작점 (main 메서드 포함)

---

## 3. 자바 프로젝트 생성 (VS Code)

다음 순서대로 프로젝트를 생성한다.

1. `Ctrl + Shift + P`
2. `Java: Create Java Project`
3. **No build tools** 선택
4. 경로 선택

   ```text
   /home/ubuntu/projects/koreanit-server/
   ```
5. 프로젝트 이름에 **`java-practice`** 입력

---

## 4. App.java 생성

`src` 아래에 `App.java` 파일을 생성하고 아래 코드를 작성한다.

```java
public class App {

    public static void main(String[] args) {
        System.out.println("자바 프로그램 시작"); 
    }
}
```

---

### 자바에서 클래스란?

자바에서 클래스는 **객체를 만들기 위한 기본 설계도**이자  
**코드가 구성되는 핵심 단위**다.

* 자바 코드는 항상 **타입(class 또는 interface) 안에서 작성**된다
* **메서드는 단독으로 존재할 수 없고**, 반드시 클래스나 인터페이스 안에 정의된다
* 프로그램의 시작은 **클래스 안에 정의된 main 메서드**에서 이루어진다


```java
public class App { ... }
```

* `App` : 클래스 이름
* `App.java` : public 클래스 이름과 동일한 파일 이름


---

### main 메서드의 의미

```java
public static void main(String[] args)
```

이 메서드는

* JVM이 **가장 먼저 호출하는 진입점(entry point)** 이다
* 자바 프로그램은 항상 **main 메서드부터 실행**된다


---

### `System.out` 이란?

> 자바 프로그램이 콘솔(터미널)로 출력할 때 사용하는 기본 출력 통로

---

### 핵심 규칙

* `public`
  → JVM이 클래스 외부에서 접근해야 하므로 **항상 public**

* `static`
  → 객체 생성 없이 바로 실행해야 하므로 **static 필수**

* `void`
  → JVM에게 값을 반환하지 않음
  → **프로그램 실행이 목적이지 결과 반환이 목적이 아님**

* `String[] args`
  → 프로그램 실행 시 전달되는 값
  → **main 메서드의 파라미터(매개변수)**

---

## 5. 빌드하기

> **빌드 = 사람이 작성한 소스 코드를 실행 가능한 형태로 변환하는 과정**

자바에서 빌드는
**`.java` 소스 파일을 JVM이 실행할 수 있는 `.class` 파일로 변환하는 것**을 의미한다.

---

## 5-1. 자바 빌드의 실제 과정

```text
App.java  ──(javac)──▶  App.class  ──(java)──▶  프로그램 실행
```

* `javac` : 자바 컴파일러
* `.java` : 사람이 작성한 소스 코드
* `.class` : JVM이 이해하는 바이트코드
* `java` : JVM을 실행해 `.class` 파일을 실행

---

## 5-2. 빌드 명령어

```bash
javac -d bin src/App.java
```

이 명령어는 다음을 의미한다.

| 구성             | 의미                             |
| -------------- | ------------------------------ |
| `javac`        | 자바 컴파일러 실행                     |
| `-d bin`       | 컴파일 결과(.class)를 `bin` 디렉토리에 생성 |
| `src/App.java` | 컴파일할 소스 파일                     |

---

## 5-3. 빌드 결과 구조

빌드 전:

```text
project/
 ├─ src/
     └─ App.java
```

빌드 후:

```text
project/
 ├─ src/
 │   └─ App.java
 └─ bin/
     └─ App.class
```

* `bin` 폴더는 **컴파일 결과물만 존재**
* 실제 실행은 `.java`가 아니라 `.class`를 기준으로 이루어진다

---

## 5-4. 실행하기
> java 명령은 .class 파일을 직접 실행하는 것이 아니라, classpath에서 클래스 이름(App)을 찾아 JVM으로 로딩한 뒤 실행한다

```bash
java -cp bin App
```
또는
```bash
cd bin
java App
```

| 구성        | 의미                              |
| --------- | ------------------------------- |
| `java`    | JVM 실행 (인자값으로 실행할 클래스 이름을 적는다)                       |
| `-cp bin` | classpath를 bin으로 지정 ( `.class` 파일은 이 디렉터리에서 찾아라 )  |
| `App`     | 실행할 클래스 이름 (`App.class` 확장자 생략) |

---


## 6. 실행 확인

VS Code에서 실행하거나
터미널에서 직접 실행하면 다음 출력이 나타난다.

```text
자바 프로그램 시작
```

이 한 줄이 출력되면

* 자바 컴파일
* JVM 실행
* main 메서드 호출

까지의 흐름이 정상적으로 동작한 것이다.

---

## 7. String[] args 출력해보기

이제 프로그램을 실행할 때 전달되는 값인 `args`를 직접 출력해 본다.

`args`는 **프로그램 실행 시 외부에서 전달되는 문자열 값들**이다.

---

### 7-1. args 값 출력 코드

`App.java`를 아래와 같이 수정한다.

```java
public class App {

    public static void main(String[] args) {
        System.out.println("자바 프로그램 시작");
        System.out.println(args[0]);
        System.out.println("[App] 정상 종료");
    }
}
```


---

### 7-2. 실행 시 값 전달하기

터미널에서 다음과 같이 실행한다.

```bash
java App hello
```

출력 결과:

```text
자바 프로그램 시작
hello
```

* `hello` 라는 문자열이 실행 시 전달되어 `args[0]`으로 들어온다

---

## 7-3. 인자값을 전달하지 않으면

```bash
java App
```

실행 결과:

```text
자바 프로그램 시작
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0
        at App.main(App.java:5)
```

### 왜 이런 일이 발생할까?

* 실행 시 인자값을 전달하지 않았기 때문에
  `args` 배열의 길이는 `0`이다
* 그런데 코드에서 존재하지 않는 `args[0]`에 접근했다
* 그 결과 JVM이 `ArrayIndexOutOfBoundsException` 예외를 발생시켰다

### 중요한 포인트

* 이 오류는 **컴파일 오류가 아니다**
* 프로그램 실행 중에 발생한 **Runtime 예외**다
* 예외를 처리(`try-catch`)하지 않았기 때문에
  JVM이 **프로그램을 즉시 종료**시킨다

> 즉, 자바 프로그램은
> **예외가 발생하고 처리되지 않으면 정상적으로 종료되지 않는다**


---

## 다음 단계

[**예외 처리(Exception)와 프로그램 흐름 제어**](02-exception-handling.md)