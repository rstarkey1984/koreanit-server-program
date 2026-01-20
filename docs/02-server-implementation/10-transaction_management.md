# 트랜잭션 관리

이 장에서는
서버 프로그램에서 **여러 데이터 변경 작업을 하나의 작업 단위로 묶는 방법**을 이해한다.

앞 장에서 커넥션 풀을 통해
DB 연결이 어떻게 관리되는지 살펴봤다면,
이제는 **그 연결 위에서 데이터의 일관성을 어떻게 보장하는지**를 다룬다.


---

## 1. 트랜잭션이란 무엇인가

트랜잭션(transaction)은
**여러 작업을 하나의 논리적 작업 단위로 묶은 것**이다.

특징:

* 모두 성공하거나
* 하나라도 실패하면 모두 취소된다

> "전부 아니면 전무(all or nothing)"

---

## 2. 트랜잭션이 필요한 상황

예를 들어 다음과 같은 작업을 생각해보자.

1. 주문 정보 저장
2. 결제 정보 저장

만약 1번은 성공했지만
2번이 실패한다면 문제가 생긴다.

* 주문은 존재하지만
* 결제는 없는 상태

이런 상황을 막기 위해
트랜잭션이 필요하다.

---

## 3. 커밋과 롤백

트랜잭션에는 두 가지 중요한 동작이 있다.

* **Commit**: 모든 작업을 확정
* **Rollback**: 작업을 모두 취소

```text
성공 → Commit
실패 → Rollback
```

---

## 4. 트랜잭션과 DB 커넥션의 관계

트랜잭션은
**하나의 DB 커넥션 단위로 관리된다.**

```text
하나의 커넥션
  ↳ 여러 SQL
  ↳ 하나의 트랜잭션
```

그래서 커넥션 풀과 트랜잭션은
항상 함께 이해해야 한다.

---

## 5. 트랜잭션을 사용하지 않으면 생기는 문제

트랜잭션이 없으면:

* 중간 실패 시 데이터 불일치
* 예외 처리 난이도 증가
* 복구가 어려워짐

> 서버에서 데이터 무결성은
> 매우 중요한 책임이다.

---

## 6. Spring에서의 트랜잭션 관리 (개념)

Spring에서는
트랜잭션 관리를 **프레임워크가 대신 처리**해준다.

일반적인 흐름:

```text
Service 시작
  → 트랜잭션 시작
  → Repository 작업들
  → 정상 종료 시 Commit
  → 예외 발생 시 Rollback
```

> 실제 코드 적용은
> 다음 실습에서 진행한다.

---

## 7. 지금 단계에서 하지 않는 것

이 장에서는 다음을 다루지 않는다.

* 트랜잭션 전파 옵션
* 격리 수준(isolation)
* 세부 설정

이 내용들은
서버 구조 이해 이후에 다룬다.

---

# 실습: 트랜잭션이 없을 때 vs 있을 때

이 실습은
**두 번의 INSERT 중 하나가 실패했을 때**
DB에 어떤 결과가 남는지 확인한다.

실습 목표:

* 트랜잭션이 없으면 첫 번째 INSERT가 남는 것을 확인
* `@Transactional`이 있으면 둘 다 롤백되는 것을 확인

---

## 8. 실습 준비: tx_logs 테이블 생성

MySQL에서 아래 SQL을 실행한다.

```sql
USE koreanit_service;

CREATE TABLE IF NOT EXISTS tx_logs (
  id BIGINT PRIMARY KEY,
  message VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

초기화(선택):

```sql
TRUNCATE tx_logs;
```

---

## 9. Repository: INSERT 메서드 만들기

`TxLogRepository`를 생성한다.

```java
package com.koreanit.server.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TxLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public TxLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(long id, String message) {
        String sql = "INSERT INTO tx_logs (id, message) VALUES (?, ?)";
        return jdbcTemplate.update(sql, id, message);
    }
}
```

---

## 10. Service: 트랜잭션 없는 버전 / 있는 버전

`TxDemoService`를 생성한다.

```java
package com.koreanit.server.service;

import com.koreanit.server.repository.TxLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TxDemoService {

    private final TxLogRepository txLogRepository;

    public TxDemoService(TxLogRepository txLogRepository) {
        this.txLogRepository = txLogRepository;
    }

    // 트랜잭션 없음: 첫 INSERT는 남을 수 있다
    public void runWithoutTx() {
        txLogRepository.insert(1L, "step1 - without tx");

        // 두 번째 INSERT에서 PK 중복 에러 발생(일부러 실패)
        txLogRepository.insert(1L, "step2 - without tx (duplicate)");
    }

    // 트랜잭션 있음: 둘 다 롤백된다
    @Transactional
    public void runWithTx() {
        txLogRepository.insert(2L, "step1 - with tx");

        // 두 번째 INSERT에서 PK 중복 에러 발생(일부러 실패)
        txLogRepository.insert(2L, "step2 - with tx (duplicate)");
    }
}
```

---

## 11. Controller: 실행 엔드포인트 추가

`TxDemoController`를 생성한다.

```java
package com.koreanit.server.controller;

import com.koreanit.server.service.TxDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TxDemoController {

    private final TxDemoService txDemoService;

    public TxDemoController(TxDemoService txDemoService) {
        this.txDemoService = txDemoService;
    }

    // 트랜잭션 없이 실행 (실패 후에도 일부 데이터가 남는지 확인)
    @GetMapping("/tx/no")
    public String runNoTx() {
        txDemoService.runWithoutTx();
        return "ok";
    }

    // 트랜잭션으로 실행 (실패하면 모두 롤백되는지 확인)
    @GetMapping("/tx/yes")
    public String runWithTx() {
        txDemoService.runWithTx();
        return "ok";
    }
}
```

---

## 12. 실습 실행 및 확인

### 12-1. 서버 실행

```bash
./gradlew bootRun
```

### 12-2. 트랜잭션 없는 실행

브라우저 또는 curl:

```bash
curl -i http://localhost:8080/tx/no
```

요청은 500 에러가 날 수 있다(의도된 실패).

DB 확인:

```sql
SELECT * FROM tx_logs ORDER BY created_at DESC;
```

기대 결과:

* `id=1` 행이 남아있을 수 있다

---

### 12-3. 트랜잭션 있는 실행

```bash
curl -i http://localhost:8080/tx/yes
```

DB 확인:

```sql
SELECT * FROM tx_logs ORDER BY created_at DESC;
```

기대 결과:

* `id=2`는 **남지 않는다** (롤백)

---

## 13. 이 실습의 핵심 정리

* 트랜잭션이 없으면 일부 작업이 DB에 남을 수 있다
* `@Transactional`은 Service 경계에서 “작업 단위”를 만든다
* 실패 시 Rollback으로 일관성을 지킨다
* 트랜잭션은 “하나의 커넥션” 위에서 동작한다

---

## 다음 단계

→ [**요청 / 응답 흐름 정리**](11-request_response_flow.md)
