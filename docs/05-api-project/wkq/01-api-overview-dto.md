# 회원가입 API (개요 & DTO)



> 이 단계의 핵심은
> **Controller → Service → Repository 흐름을 API 기준으로 설계하는 것**이다.

---

## 1. 구현할 API 명세

### 회원가입 API

* Method: `POST`
* URL: `/api/v1/users`
* Content-Type: `application/json`

#### 요청 바디 예시

```json
{
  "username": "test01",
  "password": "1234",
  "nickname": "테스트"
}
```

#### 성공 응답 예시

```json
{
  "success": true,
  "message": "회원가입 완료",
  "data": {
    "id": 1,
    "username": "test01",
    "nickname": "테스트"
  },
  "code": null
}
```

#### 실패 응답 예시 (중복 아이디)

```json
{
  "success": false,
  "message": "이미 사용 중인 아이디입니다",
  "data": null,
  "code": "DUPLICATE_RESOURCE"
}
```

---

## 2. 패키지 및 파일 구성

회원가입 API 구현을 위해
이 단계 이후 추가/수정될 파일 구조는 다음과 같다.

```text
controller/
└── UserController.java        (회원가입 API 엔드포인트 추가)

service/
└── UserService.java           (signup 메서드 구현)

repository/
└── UserRepository.java        (중복 체크 / 저장 SQL)

dto/
└── user/
      ├── UserSignupRequest.java
      └── UserSignupResponse.java
```

---

## 3. DTO ( Data Transfer Object ) 코드
> DTO는 계층 간에 데이터를 전달하기 위한 객체다.

### 3-1. UserSignupRequest

경로:

```text
src/main/java/com/koreanit/spring/dto/user/UserSignupRequest.java
```

```java
package com.koreanit.spring.dto.user;

public class UserSignupRequest {

    public String username;
    public String password;
    public String nickname;
    public String email;

    public UserSignupRequest() {
    }

    public UserSignupRequest(String username, String password, String nickname, String email) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

---

### 3-2. UserSignupResponse

경로:

```text
src/main/java/com/koreanit/spring/dto/user/UserSignupResponse.java
```

```java
package com.koreanit.spring.dto.user;

public class UserSignupResponse {

    public long id;
    public String username;
    public String nickname;

    public UserSignupResponse() {
    }

    public UserSignupResponse(long id, String username, String nickname) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
```


## 4. 왜 DTO 구조를 사용하는가
> DTO는 계층 간에 데이터를 전달하기 위한 객체다.

이 단계에서 정의한 DTO들은
단순히 데이터를 담기 위한 클래스가 아니라,
**API 서버의 책임과 흐름을 분리하기 위한 기준점**이다.

---

### 4-1. Controller ↔ Service ↔ Repository 역할 분리

회원가입 API의 흐름은 다음과 같이 고정된다.

```text
Controller → Service → Repository
```

이때 DTO의 역할은 명확하다.

* **Controller**

  * HTTP 요청(JSON)을 DTO로 받는다
  * Service 결과를 DTO로 응답한다
* **Service**

  * DTO를 입력으로 받아 비즈니스 로직을 수행한다
  * DB 구조나 HTTP 형식을 직접 다루지 않는다
* **Repository**

  * DTO가 아닌, 필요한 값만 받아 SQL을 실행한다

즉,
DTO는 **계층 사이의 전달 규칙(contract)** 역할을 한다.

---

### 4-2. Request DTO와 Response DTO를 분리하는 이유

요청과 응답을 같은 객체로 쓰지 않고
`UserSignupRequest` / `UserSignupResponse`를 나눈 이유는 다음과 같다.

* 요청에는 필요하지만, 응답에는 포함되면 안 되는 값이 있다
  (예: `password`)
* 응답에는 서버가 생성한 값이 포함된다
  (예: `id`)
* API 요구사항이 바뀔 때, 요청/응답을 독립적으로 수정할 수 있다

즉,

> **요청 DTO는 “받는 규칙”**
> **응답 DTO는 “돌려주는 규칙”** 이다.

---

### 4-3. getter / setter를 사용하는 이유

> getter / setter는 Spring(Jackson)이 DTO의 값을 넣고(JSON 요청) 읽기(JSON 응답) 위해 사용하는 기본 통로다.

이 DTO들은 **Spring + Jackson의 기본 동작 방식**을 따른다.

* JSON → DTO 변환 시: `setter` 사용
* DTO → JSON 변환 시: `getter` 사용

이 구조를 사용하면:

* 별도 설정 없이 자동 바인딩 가능
* 동작 원리를 설명하기 쉽다
* 이후 `record`, 불변 DTO로 확장 설명하기 좋다

이 단계에서는
**가장 기본적이고 명확한 형태를 기준으로 삼는다.**

---

### 4-4. dto/user 패키지로 분리한 이유

DTO를 `dto/user` 하위에 두는 이유는 **기능 단위 기준으로 API를 묶기 위해서**다.  
이 기준을 보통 **도메인**이라고 부른다.

* User 관련 요청/응답 → `dto/user`
* Post 관련 요청/응답 → `dto/post`
* Comment 관련 요청/응답 → `dto/comment`

이 구조를 쓰면:

* API가 늘어나도 파일 위치가 예측 가능하다
* Controller/Service 코드 가독성이 유지된다
* 실무 프로젝트 구조와 자연스럽게 이어진다


---

## 다음 단계

→ [**Controller 구현 – 회원가입 엔드포인트 추가**](02-controller-signup.md)

