# 예외 처리(Exception)와 프로그램 흐름 제어

이 장에서는 프로그램 실행 중 발생하는 **예외(Exception)** 를 이해하고,
예외가 발생했을 때 **프로그램 흐름을 어떻게 통제하는지**를 실습으로 확인한다.

이 내용은 이후 Spring Boot의 **Global Exception Handler** 개념으로 그대로 확장된다.

---

## 1. 예외(Exception)란 무엇인가

예외는 **프로그램 실행 중(runtime)** 에 발생하는 오류 상황이다.

* 컴파일 오류 ❌ (코드 자체가 잘못됨)
* 예외(Exception) ⭕ (실행 중 문제가 발생함)

예:

* 존재하지 않는 배열 인덱스 접근
* null 값 사용
* 잘못된 입력값

---

## 2. 우리가 이미 만난 예외

다음 코드를 실행했을 때를 떠올려보자.

```java
System.out.println(args[0]);
```

인자값 없이 실행하면 다음 예외가 발생한다.

```text
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException
```

---

### 의미 해석

* `java.lang.ArrayIndexOutOfBoundsException`

  * 배열의 범위를 벗어난 인덱스에 접근했다는 뜻이다
  * `args` 배열에 0번 인덱스가 존재하지 않는다

* `args[0]`

  * 프로그램 실행 시 전달된 명령행 인자 배열의 첫 번째 값
  * 인자를 주지 않고 실행하면 `args.length == 0`

* `in thread "main"`

  * 이 예외는 **main 스레드에서 발생**했다는 의미
  * 자바 프로그램의 시작 지점에서 발생한 예외다

* 예외 메시지가 그대로 출력됨

  * 이 예외를 처리하는 `try-catch`가
    호출 스택 어디에도 존재하지 않았다는 뜻이다

---

## 3. try-catch로 예외 잡기

예외가 발생하더라도 **프로그램을 바로 종료하지 않고**
개발자가 흐름을 제어할 수 있다.

```java
public class App {

    public static void main(String[] args) {
        System.out.println("자바 프로그램 시작");

        try {
            System.out.println(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("자바 프로그램 종료");
    }
}
```

---

### `Exception e` 의미

> Exception 타입이거나, 그 자식 타입이면 전부 잡겠다

예외 계층 핵심 구조

```
Throwable
 └─ Exception
     ├─ RuntimeException
     │   ├─ NullPointerException
     │   ├─ IndexOutOfBoundsException
     │   │    └─ ArrayIndexOutOfBoundsException
     │   ├─ IllegalArgumentException
     │   └─ ...
     └─ Checked Exception 계열
         ├─ IOException
         ├─ SQLException
         └─ ...
```

---

## 4. if 와 try-catch의 역할 구분

### if 로 처리하는 경우

```java
if (args.length == 0) {
    System.out.println("값이 필요합니다");
    return;
}
```

* 예측 가능한 상황
* 입력값 검증
* 조건 분기

---

### try-catch 로 처리하는 경우

```java
try {
    System.out.println(args[0]);
} catch (Exception e) {
    e.printStackTrace();
}
```

* 예측하기 어려운 상황
* 시스템 오류
* 실행 중 문제

---

### 핵심 기준

> 예측 가능하면 if
> 예측 불가능하면 exception

---

## 5. 예외를 던지기 (throw)

```java
if (args.length == 0) {
    throw new IllegalArgumentException("인자값이 필요합니다");
}
```

* 정상 실행 흐름을 즉시 중단한다
* 예외 객체를 생성한다
* 호출한 쪽(상위 호출자)으로 예외를 전달한다

---

# 실습: Error.java로 예외 흐름 맛보기

이 장에서는 **예외가 발생하면 흐름이 어떻게 끊기는지**만 확인한다.
아직 호출 스택과 전파 개념은 깊게 다루지 않는다.

---

## 1. Error.java 파일 생성

```java
public class Error {

    public static void main(String[] args) {
        System.out.println("프로그램 시작");

        try {
            System.out.println(args[0]);
            System.out.println("try 블록 정상 종료");
        } catch (Exception e) {
            System.out.println("catch에서 예외 처리");
        }

        System.out.println("프로그램 종료");
    }
}
```

---

## 2. 실행 실습

### 1) 인자 없이 실행

* 예외 발생
* try 블록 중단
* catch 실행
* 프로그램 종료까지 실행됨

---

### 2) 인자를 주고 실행

* 예외 없음
* try 블록 전체 실행
* catch 미실행

---

## 3. try-catch 제거 실습

```java
public static void main(String[] args) {
    System.out.println("프로그램 시작");
    System.out.println(args[0]);
    System.out.println("프로그램 종료");
}
```

* 예외 발생 시 즉시 종료
* 이후 코드는 실행되지 않음


> 예외가 발생하면 그 지점 이후 코드는 실행되지 않고,
> 잡히지 않으면 프로그램은 즉시 종료된다.

---

## 4. 실습: 특정 예외만 catch 해보기

### 4-1. ArrayIndexOutOfBoundsException만 처리

```java
try {
    System.out.println(args[0]);
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("인자값이 없습니다");
}
```

* 실제 발생한 예외 타입만 잡는다
* JVM 에러 메시지는 출력되지 않는다

---

### 4-2. 다른 예외는 잡히지 않는다

```java
try {
    String s = null;
    System.out.println(s.length());
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("배열 인덱스 예외");
}
```

* NullPointerException 발생
* catch 되지 않음
* 프로그램 즉시 종료

---

### 4-3. 여러 예외를 각각 처리하기

```java
try {
    System.out.println(args[0]);
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("인자 예외");
} catch (NullPointerException e) {
    System.out.println("null 오류");
}
```

* 위에서부터 순서대로 검사
* 하나만 실행됨

---

### 4-4. Exception은 마지막에 둔다

```java
try {
    System.out.println(args[0]);
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("인자 예외");
} catch (Exception e) {
    System.out.println("그 외 모든 예외");
}
```


> 예외는 타입으로 흐름을 분기하며,
> catch는 지정한 예외 타입만 처리한다.

---

## 실습 핵심 요약

* 예외는 **타입으로 흐름을 분기**한다
* try-catch는 **프로그램 흐름 제어 장치**다
* catch 하지 않으면 예외는 상위로 전달되고, 끝까지 가면 종료된다

---

## 다음 단계

[**예외 전파 핵심 정리**](03-exception-propagation.md)
