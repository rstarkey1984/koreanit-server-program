# 객체 생성과 의존성 연결

이 장에서는 객체를 **어디에서 생성하고**, **어떻게 연결하는 것이 올바른 구조인지**를 정리한다.

지금까지 만든 `Controller → Service` 구조를 기준으로,
**생성자를 통한 의존성 연결(생성자 주입)** 이 왜 필요한지 이해하는 것이 목표다.

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

---

## 1. 지금 구조의 문제점

현재 `UserController`는 내부에서 직접 `UserService`를 생성한다.

```java
package com.koreanit.spring;

public class UserController {

    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }
}
```

이 구조의 문제는 다음과 같다.

* Controller가 Service의 **구현을 직접 알고 있다**
* Service를 교체하거나 확장하기 어렵다
* **객체 생성 책임(new)** 과 **사용 책임(호출)** 이 섞여 있다

---

## 2. 이 구조의 핵심 문제는 "역할이 섞인다"는 것이다

이 구조의 가장 큰 문제는 단순히 "교체가 어렵다"가 아니다.

Controller 안에서 `new`를 사용하면, Controller가 다음 두 가지 책임을 동시에 가지게 된다.

```text
1) 요청을 받아 흐름을 제어하는 책임
2) 어떤 Service를 생성해서 쓸지 결정하는 책임
```

즉, Controller가 **요청 처리자이면서 조립자 역할까지 떠안는 구조**가 된다.

---

### 2-1. 책임이 섞이면 생기는 문제

Controller의 원래 역할은 다음 하나다.

```text
요청을 받고 → Service에 위임 → 결과를 반환
```

하지만 내부에서 `new UserService()`를 하기 시작하면,

* Service 선택 기준이 Controller 안으로 들어오고
* 이후 조건 분기(if), 테스트 코드, 임시 로직이 섞이기 쉽다

이 순간부터 Controller는 더 이상 "단순한 흐름 제어자"가 아니다.

---

### 2-2. 실행 흐름이 코드 밖에서 보이지 않는다

```java
public UserController() {
    this.userService = new UserService();
}
```

이 코드는 Controller를 열어보기 전까지는

* 어떤 Service를 쓰는지
* 몇 개의 객체가 생성되는지
* 언제 생성되는지

외부에서는 알 수 없다.

---

### 2-3. 객체 생명주기를 통제할 수 없다

Controller 내부에서 객체를 생성하면,

* Controller가 생성될 때마다 Service도 함께 생성된다
* Service가 몇 개 만들어지는지 통제하기 어렵다

이 구조에서는

* 객체를 공유할지
* 하나만 유지할지
* 언제 생성/종료할지

같은 결정을 Controller가 떠안게 된다.

---

## 3. 객체 생성과 의존성의 의미

### 3-1. 객체 생성이란?

```java
UserService service = new UserService();
```

* 메모리에 **객체 인스턴스를 하나 생성**한다는 의미
* “누가 new를 호출하느냐”가 구조를 결정한다

---

### 3-2. 의존성이란?

```java
UserController controller = new UserController(service);
```

* Controller는 Service 없이는 동작할 수 없다
* 이 “필요 관계”를 **의존성(dependency)** 이라고 부른다

---

## 4. 생성자를 통한 의존성 전달

Controller가 Service를 직접 만들지 않고,
**외부에서 전달받도록** 구조를 바꾼다.

### 4-1. UserController 수정

`com.koreanit.spring.UserController`

```java
package com.koreanit.spring;

import java.util.Map;

public class UserController {

    private final UserService userService;

    // 생성자를 통해 "필요한 것"을 외부에서 전달받는다
    public UserController(UserService userService) {
        this.userService = userService;
    }

    public String hello(Map<String, Object> body) {
        System.out.println("[Controller] body 수신");
        System.out.println(body);

        // Controller는 판단하지 않고, Service에 위임한다
        return userService.getHelloMessage(body);
    }
}
```

핵심:

* Controller는 **Service가 어떻게 만들어졌는지 모른다**
* Controller가 아는 것은 “필요한 타입(UserService)” 뿐이다
* 교체 가능해진다 (Fake/Real 모두 가능)

---

## 5. App에서 객체 생성 및 연결

이제 객체 생성과 연결은 `App(main)`에서 수행한다.

`App`

```java
import com.koreanit.spring.UserController;
import com.koreanit.spring.UserService;

import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) {

        // 1) 객체 생성은 여기서만
        UserService service = new UserService();
        UserController controller = new UserController(service);

        // 2) 요청 body 생성
        Map<String, Object> body = new HashMap<>();
        if (args.length > 0) body.put("username", args[0]);
        if (args.length > 1) body.put("age", Integer.parseInt(args[1]));

        // 3) 호출 + 최종 catch
        try {
            String result = controller.hello(body);
            System.out.println("[App] result = " + result);
        } catch (Exception e) {
            System.out.println("[App] 예외 발생: " + e);
        }

        System.out.println("[App] 정상 종료");
    }
}
```

---

## 6. 이 구조의 의미 (핵심 정리)

```text
App은 조립자이고,
Controller는 흐름 제어자이며,
Service는 판단자다.
```

이 구조의 핵심 장점은 다음 세 가지다.

* **책임이 명확히 분리된다**
* **실행 흐름이 코드 한 줄로 보인다**
* **객체 생명주기를 통제할 수 있다**

---

## 7. Spring으로의 연결

Spring Boot에서는 이 역할을 **Spring Container**가 대신 수행한다.

```text
순수 자바: 개발자가 new + 연결
스프링:   컨테이너가 객체 생성 + 연결 자동화
```

* `@RestController`
* `@Service`

지금은 수동으로 하지만, **개념은 동일**하다.

---

## 다음 단계

Repository를 분리해서

```text
Controller → Service → Repository
```

→ [**객체 생성과 의존성 연결**](07-repository-layer-and-three-tier-assembly.md)