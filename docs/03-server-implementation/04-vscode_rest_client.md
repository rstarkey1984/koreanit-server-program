# VS Code REST Client 사용법

이 문서는 **VS Code REST Client 확장**을 사용해
Spring Boot API를 테스트하는 방법을 정리한 강의용 문서다.

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

```text
spring/.http/api.http
```

---

## 4. GET 요청 예제

```http
### GET health
GET http://localhost:8080/health

### GET health/check 
GET http://localhost:8080/health/check?name=test&count=1
```

---

## 5. 응답 화면에서 확인할 수 있는 정보

REST Client 응답 탭에서 다음을 확인할 수 있다.

* HTTP 상태 코드 (200, 400, 409, 500 등)
* Response Headers
* Response Body (JSON)

예시 응답:

```json
{
  "message": "Hello JSON"
}
```

## 6. 변수 사용

```
@host = http://localhost:8080

### GET health
GET {{host}}/health

### GET health/check 
GET {{host}}/health/check?name=test&count=1
```

---

## 핵심 요약

> REST Client는
> **API 요청을 코드처럼 관리하는 도구**다.

## 다음 단계

[**Controller 요청 데이터 바인딩 실습 (TestController)**](05-controller_data_parse.md)
