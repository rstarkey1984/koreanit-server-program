# 07. Repository 분리와 App에서 3계층 조립

이 장에서는 **새로운 요구사항(데이터 개념의 등장)** 을 계기로 Repository 계층을 추가하고,

```text
Controller → Service → Repository
```

구조를 완성한다.

핵심 목표는 다음 두 가지다.

* 각 계층의 **역할을 명확히 분리**한다
* 객체 생성과 연결을 **App(main)에서 한 번에 조립**한다

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
│               ├─ UserService.java
│               └─ UserRepository.java
└─ bin/
```

---

## 1. 왜 Repository가 필요한가

지금까지 Service는 **입력값(body)** 만으로 결과를 만들었다.

즉, 아래 같은 “데이터 접근”은 아직 없었다.

* DB 조회
* 파일 읽기
* 외부 API 호출

---

하지만 이제 다음 요구가 생긴다고 가정해보자.

> "username을 기준으로 사용자 나이, 프로필 같은 데이터를 조회해야 한다"

이 순간부터 코드에 **데이터라는 개념**이 들어온다.

---

### 이때 선택지는 두 가지다

1. Service가 직접 데이터 저장소를 다룬다
2. 데이터 접근 전용 계층(Repository)을 분리한다

이 장에서는 **2번을 선택**한다.

이유는 단순하다.

> Service는 “판단(규칙)”에 집중하고
> 데이터가 어디서 오는지는 Repository가 맡게 하기 위해서다.

---

## 2. Repository의 역할

Repository는 오직 한 가지 책임만 가진다.

```text
"데이터를 가져오거나 저장한다"
```

Repository는 다음을 하지 않는다.

* 판단하지 않는다
* 메시지를 만들지 않는다
* 예외 정책을 정하지 않는다

즉, **데이터 접근 코드를 한 곳에 모아두는 역할**이다.

---

## 3. UserRepository 만들기

`com.koreanit.spring.UserRepository`

```java
package com.koreanit.spring;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    // DB를 흉내낸 저장소
    private final Map<String, Integer> users = new HashMap<>();

    public UserRepository() {
        // 초기 데이터
        users.put("user1", 20);
        users.put("user2", 30);
    }

    public Integer findAgeByUsername(String username) {
        return users.get(username); // 없으면 null
    }
}
```

---

## 4. Service에서 Repository 사용하기

Service는 더 이상 데이터를 직접 다루지 않는다.

```java
package com.koreanit.spring;

import java.util.Map;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getHelloMessage(Map<String, Object> body) {

        String username = (String) body.get("username");

        if (username == null || username.isBlank()) {
            username = "guest";
        }

        // 비즈니스 규칙 (판단)
        if ("admin".equals(username)) {
            throw new IllegalArgumentException("admin은 사용할 수 없는 이름입니다");
        }

        // 데이터 조회는 Repository에 위임
        Integer age = userRepository.findAgeByUsername(username);
        if (age == null) {
            age = 0;
        }

        return "안녕 " + username + " (" + age + "세)";
    }
}
```

---

## 5. Controller는 그대로 둔다

```java
package com.koreanit.spring;

import java.util.Map;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public String hello(Map<String, Object> body) {
        return userService.getHelloMessage(body);
    }
}
```

---

## 6. App에서 3계층 조립

```java
import com.koreanit.spring.UserController;
import com.koreanit.spring.UserRepository;
import com.koreanit.spring.UserService;

import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) {

        // 1) 객체 생성 + 연결
        UserRepository repository = new UserRepository();
        UserService service = new UserService(repository);
        UserController controller = new UserController(service);

        // 2) 요청 body 생성
        Map<String, Object> body = new HashMap<>();
        if (args.length > 0) body.put("username", args[0]);

        // 3) 호출 + 최종 처리
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

## 7. 전체 흐름 요약

```text
App
 ↓ (조립)
Controller
 ↓ (위임)
Service
 ↓ (데이터 요청)
Repository
```

* 객체 생성은 한 곳(App)에만 존재한다
* 각 계층은 자신의 역할만 가진다

---

## 8. Spring으로의 연결

```text
@RestController
@Service
@Repository
```

* 지금은 수동 조립
* 개념은 Spring DI와 동일

---

## 실습 1. Repository 예외를 Service에서 판단하기

### 목표

Repository는 판단하지 않고, Service가 규칙(정책)을 정한다는 것을 확인한다.

### 실습 내용

#### Step 1. Repository는 그대로 null 반환

`UserRepository`

```java
public Integer findAgeByUsername(String username) {
    return users.get(username); // 없으면 null
}
```

#### Step 2. Service에서 정책 추가

`UserService`

```java
Integer age = userRepository.findAgeByUsername(username);

if (age == null) {
    throw new IllegalStateException("존재하지 않는 사용자입니다");
}

return "안녕 " + username + " (" + age + "세)";
```

#### Step 3. App에서 메시지 확인

```bash
java App unknownUser
```

예상 출력

```text
[App] 예외 발생: java.lang.IllegalStateException: 존재하지 않는 사용자입니다
[App] 정상 종료
```

### 체크 포인트

* Repository는 여전히 "데이터만" 반환한다
* "이 상황이 문제인가?"는 Service가 판단한다

---

## 실습 2. Dispatcher로 확장하기 전 미니 과제

### 목표

요청이 여러 개로 늘어날 때, App의 분기 코드가 급격히 커진다는 것을 직접 확인한다.

### 실습 내용

#### Step 1. App에 path 개념을 추가한다

`App`

```java
// 2) 요청 경로 설정
String path = "";
if (args.length > 0) path = args[0];

// 3) 요청 body 생성
Map<String, Object> body = new HashMap<>();
if (args.length > 1) body.put("username", args[1]);  
```

#### Step 2. path에 따라 Controller 호출을 분기한다

```java
try {
    String result;

    if ("/hello".equals(path)) {
        result = controller.hello(body);
    } else {
        result = "404 Not Found";
    }

    System.out.println("[App] result = " + result);
} catch (Exception e) {
    System.out.println("[App] 예외 발생: " + e);
}
```

#### Step 3. 실행해 본다

```bash
java App user1 /hello
java App user1 /unknown
```

### 체크 포인트

* path가 늘어나면 `if/else`가 계속 커진다
* "요청 분배" 책임을 전담하는 별도 구조가 필요해진다

---

## 다음 단계

* Dispatcher / 요청 분배 구조로 확장
