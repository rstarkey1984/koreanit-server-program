# Controller에서 인자값을 받아 return으로 전달하기

이 장에서는 **Controller 메서드에 인자값을 전달하고**,
그 값이 `return`을 통해 다시 호출한 쪽으로 전달되는 흐름을 정리한다.

핵심 목표는 다음 두 가지다.

1. 값이 **어디서 들어와서 어디로 돌아가는지**
2. 패키지와 `import`가 **왜 필요한지**

---

## 이번 장의 핵심 구조

```text
App(main, args)
 ↓ (인자 전달)
Controller 메서드
 ↓ (return)
App에서 출력
```

* 아직 예외는 사용하지 않는다
* 정상 흐름에서 **값 전달 구조**에만 집중한다

---

## 프로젝트 폴더 구조

```text
java-practice/
├─ src/
│   ├─ App.java
│   └─ com/
│       └─ koreanit/
│           └─ spring/
│               └─ UserController.java
└─ bin/
```

* `App.java` : 프로그램 시작점 (main 메서드)
* `UserController.java` : 요청을 받아 값을 반환하는 Controller
* 지금은 HTTP 서버가 없으므로 **패키지 구조만 Spring을 흉내낸다**

---

## 1. 패키지(package)의 의미

`UserController`는 다음 패키지에 속해 있다.

```text
com.koreanit.spring
```

자바에서 클래스의 **실제 이름**은
파일 이름이 아니라 **패키지를 포함한 전체 이름**이다.

즉,

```text
UserController
```

가 아니라,

```text
com.koreanit.spring.UserController
```

가 이 클래스의 정확한 이름이다.

### 왜 패키지가 필요한가

* 같은 이름의 클래스라도

  * 패키지가 다르면 **서로 다른 클래스**다
* 패키지는

  * 클래스를 역할별로 묶고
  * 이름 충돌을 방지한다

---

## 2. import의 역할

`App.java`에서 `UserController`를 사용하려면
자바는 다음을 알아야 한다.

> 어느 패키지에 있는 UserController 인가?

그래서 다음 코드가 필요하다.

```java
import com.koreanit.spring.UserController;
```

이 의미는 다음과 같다.

> `com.koreanit.spring.UserController`를
> 이 파일 안에서는 `UserController`라는 이름으로 쓰겠다

중요한 점:

* `import`는 파일을 불러오는 문법이 아니다
* **클래스 전체 이름을 짧게 쓰기 위한 문법**이다

아래처럼 `import` 없이도 사용할 수 있다.

```java
com.koreanit.spring.UserController controller =
        new com.koreanit.spring.UserController();
```

---

## 3. Controller 클래스

`com.koreanit.spring.UserController`

```java
package com.koreanit.spring;

public class UserController {

    public String hello(String username) {
        System.out.println("[Controller] hello(username) 실행됨");
        System.out.println("[Controller] 전달받은 username = " + username);

        return "안녕 " + username;
    }
}
```

핵심 포인트

* Controller 메서드는 **외부에서 값을 전달받는다**
* Controller는 직접 출력이 목적이 아니다
* 처리 결과를 `return`으로 돌려준다

---

## 4. App에서 Controller 호출하기

`src/App.java`

```java
import com.koreanit.spring.UserController;

public class App {

    public static void main(String[] args) {
        System.out.println("[App] 프로그램 시작");

        UserController controller = new UserController();

        // args에서 값 꺼내기 (없으면 기본값 사용)
        String username = (args.length > 0) ? args[0] : "guest";

        System.out.println("[App] controller.hello(username) 호출 직전");
        String result = controller.hello(username);
        System.out.println("[App] controller.hello(username) 호출 직후");

        System.out.println("[App] result 출력: " + result);
        System.out.println("[App] 정상 종료");
    }
}
```

---

## 5. 실행 결과 확인

### 인자값 전달

```bash
java App user1
```

```text
[Controller] 전달받은 username = user1
[App] result 출력: 안녕 user1
```

### 인자값 없이 실행

```bash
java App
```

```text
[Controller] 전달받은 username = guest
[App] result 출력: 안녕 guest
```

---

## 6. return 흐름의 핵심

다음 한 줄이 이 장의 핵심이다.

```java
String result = controller.hello(username);
```

의미는 다음과 같다.

* `controller.hello(username)`가 실행된다
* `return`을 만나면
* **그 값이 호출한 자리로 되돌아온다**

개념적으로는 아래와 같다.

```java
String result = "안녕 " + username;
```

`return`은 출력이 아니라 **값 전달**이다.

---

## 7. 이 구조의 확장

지금 구조는 나중에 이렇게 바뀐다.

```text
App                → Tomcat
return String      → Response Body
```

* 지금은 콘솔 출력
* 나중에는 HTTP 응답

**return 구조는 그대로 유지된다**

---

# 실습: Controller에서 Map body로 값 전달하기

이 실습에서는
Controller가 여러 인자값을 직접 받지 않고,
**`Map<String, Object>` 형태의 body 하나만 전달받는 구조**를 만든다.

이 구조의 목적은 다음과 같다.

* 여러 값을 하나의 객체로 묶어 전달하는 방식 이해
* HTTP 요청의 `RequestBody` 개념을 미리 체험
* 이후 DTO / JSON / `@RequestBody` 로 자연스럽게 확장

---

## 이번 실습의 핵심 흐름

```text
App
 ↓ (Map body 생성)
Controller(Map<String, Object> body)
 ↓ (값 꺼내기)
return 결과
 ↑
App에서 출력
```

---

## 1. Controller 수정

`com.koreanit.spring.UserController`

```java
package com.koreanit.spring;

import java.util.Map;

public class UserController {

    public String hello(Map<String, Object> body) {

        System.out.println("[Controller] body 수신");
        System.out.println(body);

         // 1) 입력 꺼내기
        String username = (String) body.get("username");
        Integer ageObj = (Integer) body.get("age");

        // 2) 값 보정(없으면 기본값)
        if (username == null || username.isBlank()) {
            username = "guest";
        }
        int age = (ageObj == null) ? 0 : ageObj;

        return "안녕 " + username + " (" + age + "세)";
    }
}
```

핵심 포인트

* 요청 데이터는 하나의 `body` 객체로 전달된다
* 필요한 값은 `key`로 꺼내서 사용한다

---

## 2. App에서 body 만들어서 전달

`src/App.java`

```java
import com.koreanit.spring.UserController;

import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) {

        UserController controller = new UserController();        

        // 1) body HashMap 생성
        Map<String, Object> body = new HashMap<>();

        // 2) args에서 값 받아서 body에 삽입
        if (args.length > 0) body.put("username", args[0]);
        if (args.length > 1) body.put("age", Integer.parseInt(args[1]));

        // 3) Controller 호출
        try{
            String result = controller.hello(body);
            System.out.println("[App] result = " + result);
        }catch(Exception e){
            System.out.println("[App] 예외 발생: " + e);
        }
        
        System.out.println("[App] 정상 종료");        
    }
}
```

---

## 3. 실행 결과

실행
```
java App test 33
```

결과
```text
[Controller] body 수신
{age=33, username=test}
[App] result = 안녕 test (33세)
[App] 정상 종료
```

---

## 4. 이 실습이 중요한 이유

이 구조는 나중에 다음과 같이 확장된다.

### 지금 (순수 자바)

```java
public String hello(Map<String, Object> body) {
    ...
}
```

### 이후 (Spring)

```java
@PostMapping("/hello")
public String hello(@RequestBody Map<String, Object> body) {
    ...
}
```

* 구조는 동일하다
* 달라지는 것은 **값을 채워주는 주체** 뿐이다

---

## 다음 단계

다음 장에서는
Controller가 직접 판단하지 않고,
**Service로 비즈니스 로직을 분리하는 이유**를 다룬다.

→ [**Service를 분리하고 Controller → Service 흐름 만들기**](05-controller-service-flow.md)
