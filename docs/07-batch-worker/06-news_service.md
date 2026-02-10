# 뉴스 서비스 구현

이 문서에서는  
Node.js 워커에서 사용하는 **뉴스 서비스(service)** 를 구현한다.

서비스는  
job과 repository 사이에서  
**업무 흐름과 규칙을 담당하는 계층**이다.


---

## 1. service의 역할 정의

service는 다음 책임만 가진다.

* 하나의 업무 흐름을 표현
* 여러 repository 호출을 묶어 순서 제어
* 실패 조건을 판단하고 예외 발생

service는 다음을 **직접 하지 않는다.**

* SQL 작성
* 프로세스 종료 처리
* 외부 입출력 제어(job의 책임)

---

## 2. service 파일 위치

서비스는 `src/services` 하위에 위치한다.

```text
src/
  services/
    news.service.js
```

규칙:

* 하나의 도메인 = 하나의 service 파일
* 클래스 사용 ❌
* 함수 묶음 형태 유지

---

## 3. service 기본 구조

`src/services/news.service.js`

```js
const newsRepository = require("../repositories/news.repository");

async function fetchAndSave(items) {
  for (const item of items) {
    await newsRepository.save(item);
  }
}

module.exports = {
  fetchAndSave,
};
```

이 파일은  
**“뉴스를 수집해서 저장한다”** 는  
업무 흐름만 표현한다.

---

## 4. service에서 처리할 책임 범위

### 4-1. service가 하는 일

* 외부 데이터 처리 결과를 받아 저장 흐름 결정
* 중복 데이터 처리 정책 결정
* 저장 성공/실패 판단

예시 흐름:

```text
뉴스 수집
  ↓
저장 대상 선별
  ↓
repository 호출
  ↓
실패 시 예외 발생
```

---

### 4-2. service가 하지 않는 일

service에서는 다음을 하지 않는다.

* SQL 직접 작성 ❌
* 트랜잭션 직접 제어 ❌
* DB 커넥션 관리 ❌

> 이 책임들은 repository 또는 인프라 계층의 몫이다.

---


## 다음 단계

다음 문서에서는  
service가 호출하는 **repository 계층**을 구현한다.

→ [**뉴스 리포지토리 구현**](06-news_repository.md)
