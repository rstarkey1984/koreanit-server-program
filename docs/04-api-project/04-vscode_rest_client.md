# VS Code REST Client 사용법

이 문서는 **VS Code REST Client 확장**을 사용해
Spring Boot API를 테스트하는 방법을 정리한 강의용 문서다.

회원가입 API(`/api/v1/users`)를 기준으로 설명한다.

---

## 1. REST Client 확장 설치

1. VS Code 좌측 **Extensions** 탭
2. `REST Client` 검색
3. 제작자 **Huachao Mao**
4. Install

설치 후 VS Code는 `.http`, `.rest` 파일을 자동 인식한다.

---

## 2. 기본 사용 개념

* API 요청을 **텍스트 파일**로 작성한다
* `###` 로 요청을 구분한다
* 각 요청 위에 나타나는 **Send Request** 버튼으로 실행한다
* 응답은 **별도 탭**에 표시된다

---

## 3. 요청 파일 생성

프로젝트 루트 아래에 파일을 생성한다.

```text
api.http
```

---

## 4. POST 요청 기본 예제 (회원가입)

```http
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "username": "test01",
  "password": "1234",
  "nickname": "테스트",
  "email": "test01@test.com"
}
```

* 커서를 요청 블록 안에 두면 **Send Request** 버튼이 나타난다
* 버튼 클릭 시 요청이 전송된다

---

## 5. 여러 요청을 하나의 파일에 작성하기

```http
### 회원가입 성공
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "username": "test01",
  "password": "1234",
  "nickname": "테스트"
}

### 아이디 중복
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "username": "test01",
  "password": "1234",
  "nickname": "중복"
}
```

* `###` 기준으로 요청이 분리된다
* 각 요청마다 Send Request 버튼이 따로 표시된다

---

## 6. GET 요청 예제

```http
### GET 요청
GET http://localhost:8080/health
```

---

## 7. 응답 화면에서 확인할 수 있는 정보

REST Client 응답 탭에서 다음을 확인할 수 있다.

* HTTP 상태 코드 (200, 400, 409, 500 등)
* Response Headers
* Response Body (JSON)

예시 응답:

```json
{
  "success": false,
  "message": "이미 사용 중인 아이디입니다",
  "data": null,
  "code": "DUPLICATE_RESOURCE"
}
```

→ `ApiResponse` + `GlobalExceptionHandler` 구조 확인용으로 적합하다

---

## 8. 변수 사용하기 (권장)

```http
@host = http://localhost:8080

POST {{host}}/api/v1/users
Content-Type: application/json

{
  "username": "test02",
  "password": "1234",
  "nickname": "변수테스트"
}
```

* 서버 주소 변경 시 `@host` 한 줄만 수정하면 된다

---

## 9. 실패 케이스 테스트 예제

### 필수값 누락

```http
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "username": "",
  "password": "",
  "nickname": ""
}
```

기대 결과:

* HTTP 400
* `INVALID_REQUEST`

---

## 핵심 요약

> REST Client는
> **API 요청을 코드처럼 관리하는 도구**다.

## 다음 단계

[**05. Repository SQL 구현 (count / insert)**](05-repository-sql.md)
