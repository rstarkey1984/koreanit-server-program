# Service를 분리하고 Controller → Service 흐름 만들기

이 장에서는 Controller의 역할을 더 명확히 하기 위해
**비즈니스 로직을 Service로 분리**한다.

목표는 다음 질문에 답하는 것이다.

> "Controller는 왜 판단을 하지 않고,
> Service가 결과를 만들어 return 하거나,
> 실패 시 예외를 throw 해야 하는가?"

---

## 프로젝트 폴더 구조

```text
java-practice/
├─ src/
│   ├─ App.java
│   └─ com/
│       └─ koreanit/
│           └─ spring/
│               ├─ UserController.java
│               └─ UserService.java
└─ bin/
```

* `UserController` : 요청을 받고 Service를 호출
* `UserService` : 비즈니스 로직을 수행하고 결과를 결정
* 예외 처리는 **App(main)** 에서 수행한다

---

## 1. Service 클래스 만들기

`com.koreanit.spring.UserService`

```java
package com.koreanit.spring;

import java.util.Map;

public class UserService {

    public String getHelloMessage(Map<String, Object> body) {
        System.out.println("[Service] body 수신");
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

* Service는 **결과를 만드는 책임**을 가진다

---

## 2. Controller에서 Service 사용하기

`com.koreanit.spring.UserController`

```java
package com.koreanit.spring;

import java.util.Map;

public class UserController {

    private final UserService userService; // 멤버변수

    public UserController() { // 생성자
        this.userService = new UserService();
    }

    public String hello(Map<String, Object> body) {
        return userService.getHelloMessage(body);
    }
}
```
## 생성자와 멤버변수의 관계 (UserController)

```java
private final UserService userService;
```

### 1. 멤버변수란?

* 클래스 안에 선언된 변수
* 객체가 생성된 이후 **객체가 살아있는 동안 유지**됨
* 클래스의 모든 메서드에서 공통으로 사용 가능

> `UserController`가 동작할 때 항상 필요로 하는 **의존 객체**

---

```java
public UserController() {
    this.userService = new UserService();
}
```

### 2. 생성자의 역할

* 객체가 `new` 될 때 **단 한 번 실행**됨
* 멤버변수를 **초기화**하는 책임을 가짐
* 멤버변수에 실제 사용할 객체를 연결

즉,

> "UserController가 생성될 때
> UserService 객체도 하나 만들어서 함께 가진다"

라는 의미

---

### 3. 생성자 + 멤버변수 흐름 정리

```text
UserController 생성
 ↓
생성자 실행
 ↓
userService 멤버변수 초기화
 ↓
hello() 메서드에서 userService 사용
```

```java
public String hello(Map<String, Object> body) {
    return userService.getHelloMessage(body);
}
```

* `hello()`는 userService를 직접 생성하지 않음
* 이미 생성자에서 준비된 멤버변수를 사용

---

## 3. App에서 Controller 호출

`src/App.java` 코드 바뀌는거 없음

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

실행 결과:

```text
[Controller] body 수신
{age=0, username=guest}
[App] result = 안녕 guest (0세)
```

---

## 4. 비즈니스 로직 추가 (실패는 예외로 처리)

Service에 **간단한 판단 로직**을 추가한다.

> 규칙: `admin` 이라는 이름은 사용할 수 없다

여기서부터 중요한 규칙:

* 정상일 때만 `return`
* 실패일 때는 **문자열을 반환하지 말고 `throw`로 중단**

---

### 4-1. UserService 수정

```java
package com.koreanit.spring;

import java.util.Map;

public class UserService {

    public String getHelloMessage(Map<String, Object> body) {
        System.out.println("[Service] body 수신");
        System.out.println(body);

        // 1) 입력 꺼내기
        String username = (String) body.get("username");
        Integer ageObj = (Integer) body.get("age"); // null 가능

        // 2) 값 보정(없으면 기본값)
        if (username == null || username.isBlank()) {
            username = "guest";
        }
        int age = (ageObj == null) ? 0 : ageObj;

        // 3) 비즈니스 로직 (판단)
        if ("admin".equals(username)) {
            // 실패는 문자열 return이 아니라 throw로 중단
            throw new IllegalArgumentException("admin은 사용할 수 없는 이름입니다");
        }

        // 4) 정상일 때만 결과 return
        return "안녕 " + username + " (" + age + "세)";
    }
}
```

* Service에서 **조건 판단(if)** 이 발생한다
* 이 지점이 바로 **비즈니스 로직**이다
* 실패는 `return "실패"`가 아니라 **throw**로 처리한다

---

### 4-2. UserController 수정

```java
package com.koreanit.spring;

import java.util.Map;

public class UserController {

    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    public String hello(Map<String, Object> body) {
      
        // Controller는 판단/검증하지 않는다
        // Service가 만든 결과를 그대로 return
        // Service가 던진 예외도 그대로 위로 전파
        return userService.getHelloMessage(body);
    }
}
```

* Controller는 예외를 잡지 않는다 ( try-catch 없음 )
* Service가 던진 예외는 그대로 위로 전달된다

---

### 4-3. App (여기서 catch)

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
        }catch(Exception e){ // controller.hello 메서드 호출시 발생한 에러 catch
            System.out.println("[App] 예외 발생: " + e);
        }
        
        System.out.println("[App] 정상 종료");        
    }
}
```

---

## 5. 실행 결과 확인

실행
```bash
java -cp bin App user1 20
```
또는
```bash
cd bin
java App user1 20
```


### 실패 케이스 (admin)
실행:
```
java App admin 20
```

출력 예:
```
[Controller] body 수신
{age=20, username=admin}
[Service] body 수신
{age=20, username=admin}
[App] 예외 발생: java.lang.IllegalArgumentException: admin은 사용할 수 없는 이름입니다
[App] 정상 종료
```

---

## 6. 역할 분리의 의미

* Controller

  * 외부 요청을 받는다
  * Service를 호출한다
  * 정상 결과를 return 한다

* Service

  * 비즈니스 로직을 수행한다
  * 정상일 때만 결과를 return 한다
  * 실패하면 예외를 throw 한다

* App(main)

  * 최종 catch 위치
  * 실패 메시지를 출력한다

> Controller는 **흐름 제어자**이고
> Service는 **판단자**다

---

## 7. 이 구조의 확장

이 구조는 이후 이렇게 확장된다.

```text
Controller → Service → Repository
```

그리고 Spring에서는:

```text
Controller (@RestController)
Service (@Service)
Repository (@Repository)
```

* 어노테이션이 추가될 뿐
* **역할과 흐름은 변하지 않는다**

---

## 다음 단계

의존성 연결을 명확히 하고
생성자를 통한 객체 전달을 이해한다.

→ [**객체 생성과 의존성 연결**](06-constructor-injection-and-dependency.md)
