# Controller 요청 데이터 파싱

이 장에서는 Controller가 **HTTP 요청에서 데이터를 꺼내는 방법**을 실습한다.

목표는 비즈니스 로직 구현이 아니라,

* 클라이언트가 보낸 값이
* 어떤 방식으로 Controller 메서드 파라미터로 들어오는지
* 그리고 어떤 응답으로 돌아가는지

이 흐름을 직접 체감하는 것이다.

---

## 1. 요청 데이터가 들어오는 위치 4가지

HTTP 요청에서 값은 보통 다음 네 곳 중 하나로 전달된다.

1. Query String (`?page=1`)
2. Path Variable (`/users/10`)
3. Body(JSON) (`{"username":"a"}`)
4. Header (`Authorization: ...`)

Controller는 이 값을 꺼내기 위해 **어노테이션 기반 파라미터 바인딩**을 사용한다.

---

## 2. Query String 받기: `@RequestParam`

### 2-1. 단일 값 받기

```java
@GetMapping("/echo")
public Map<String, Object> echo(@RequestParam String msg) {

    Map<String, Object> result = new HashMap<>();
    result.put("msg", msg);
    return result;
}
```

요청:

```
GET /echo?msg=hi
```

응답:

```json
{ "msg": "hi" }
```

---

### 2-2. 옵션 값 처리 (없어도 되는 파라미터)

```java
@GetMapping("/search")
public Map<String, Object> search(
    @RequestParam(required = false) String keyword
) {
    Map<String, Object> result = new HashMap<>();
    result.put("keyword", keyword);
    return result;
}
```

요청:

```
GET /search
GET /search?keyword=spring
```

---

### 2-3. 기본값 주기

```java
@GetMapping("/page")
public Map<String, Object> page(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
) {
    Map<String, Object> result = new HashMap<>();
    result.put("page", page);
    result.put("size", size);
    return result;
}
```

요청:

```
GET /page
GET /page?page=2&size=20
```

---

## 3. URL 경로 값 받기: `@PathVariable`

리소스의 식별자(id)를 URL 경로에 포함시키는 방식이다.

```java
@GetMapping("/users/{id}")
public Map<String, Object> userDetail(@PathVariable int id) {

    Map<String, Object> result = new HashMap<>();
    result.put("userId", id);
    return result;
}
```

요청:

```
GET /users/10
```

응답:

```json
{ "userId": 10 }
```

---

## 4. JSON Body 받기: `@RequestBody`

클라이언트가 JSON을 보내면 Controller는 이를 객체(Map 또는 DTO)로 받는다.

### 4-1. Map으로 받기 (구조 체감용)

```java
@PostMapping("/users")
public Map<String, Object> createUser(@RequestBody Map<String, Object> body) {

    Map<String, Object> result = new HashMap<>();
    result.put("received", body);
    return result;
}
```

요청:

```http
POST /users
Content-Type: application/json

{
  "username": "neo",
  "password": "1234",
  "nickname": "네오"
}
```

응답:

```json
{
  "received": {
    "username": "neo",
    "password": "1234",
    "nickname": "네오"
  }
}
```

---

### 4-2. DTO로 받기 (실전 권장)

```java
public class CreateUserRequest {
    private String username;
    private String password;
    private String nickname;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
}
```

```java
@PostMapping("/users-dto")
public Map<String, Object> createUserDto(@RequestBody CreateUserRequest req) {

    Map<String, Object> result = new HashMap<>();
    result.put("username", req.getUsername());
    result.put("nickname", req.getNickname());
    return result;
}
```

DTO를 사용하는 이유:

* 입력 구조를 고정할 수 있다
* 키 오타, 누락을 컴파일 시점에 줄일 수 있다
* 이후 검증(@Valid) 단계로 확장하기 쉽다

---

## 5. Header 값 받기: `@RequestHeader`

```java
@GetMapping("/whoami")
public Map<String, Object> whoami(
    @RequestHeader(required = false, name = "User-Agent") String userAgent
) {
    Map<String, Object> result = new HashMap<>();
    result.put("userAgent", userAgent);
    return result;
}
```

---

## 6. Controller가 지켜야 할 선

Controller는 여기까지만 담당한다.

* 요청 값 수신
* 기본적인 형태 확인
* Service 호출

Controller가 하면 안 되는 것:

* DB 접근
* 권한 판단
* 비즈니스 규칙 처리
* 트랜잭션 관리

---

## 이 장의 핵심 정리

* `@RequestParam` : Query String
* `@PathVariable` : URL 경로 변수
* `@RequestBody` : JSON Body
* `@RequestHeader` : Header
* Controller는 입력 처리까지만 담당한다

---

## 다음 단계

[**Service 계층 구현**](07-service_layer.md)
