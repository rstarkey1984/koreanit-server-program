# 03. 대상 없음 처리 (404) – Service 결과 해석 (ApiException 적용)

이 문서는 Users CRUD에서 **대상이 존재하지 않는 경우를 404로 처리**하는 기준을 정의한다.

이전 단계에서 다음을 고정했다.

* 요청 바디 누락 / JSON 파싱 실패 → 400 (`GlobalExceptionHandler`)
* 요청 값 판단 실패(`@Valid`) → 400 (`GlobalExceptionHandler`)

본 단계에서는 **요청 및 요청 값 판단이 정상인 상태에서, 대상 리소스가 존재하지 않는 경우**를 구분한다.

---

## 이 단계의 핵심 메시지

> 요청은 정상이다. 하지만 대상 리소스가 존재하지 않는다. → 404

---

## 처리 기준

Service는 다음 두 경우를 **404로 해석**한다.

1. `SELECT 단건 조회` 결과 없음 (`queryForObject` 예외)
2. `UPDATE / DELETE` 영향 행 수가 0

Repository는 여전히 **DB 접근만 담당**하며,
**의미 해석(404 포함)은 Service 책임**으로 고정한다.

---

## 0. 준비

### 파일: `common/error/ApiException`, `common/error/ErrorCode`

#### 파일 역할

* API 오류를 공통 형식으로 표현하기 위한 도메인 예외 타입을 제공한다.
* `ErrorCode`를 통해 HTTP 상태/에러 코드를 표준화한다.
* `GlobalExceptionHandler`에서 일관된 응답(`ApiResponse`)으로 변환된다.

Service는 아래 예외 타입을 사용한다.

```java
import com.koreanit.spring.common.error.ApiException;
import com.koreanit.spring.common.error.ErrorCode;
```

404 응답은 다음과 같이 표현한다.

```java
throw new ApiException(
    ErrorCode.NOT_FOUND_RESOURCE,
    "존재하지 않는 사용자입니다"
);
```

`GlobalExceptionHandler`는 이미 `ApiException`을 처리하므로 **추가 매핑은 필요 없다.**

---

## 1. 단건 조회 결과 없음 (`queryForObject`)

`JdbcTemplate.queryForObject()`는 조회 결과가 없으면 `EmptyResultDataAccessException`을 발생시킨다.
본 단계에서는 이 DB 기술 예외를 Service에서 **404 의미로 변환**한다.

### 파일: `service/UserService.java`

#### 변경 목적

* DB 기술 예외를 그대로 외부로 노출하지 않는다.
* 조회 결과 없음은 `ApiException(ErrorCode.NOT_FOUND_RESOURCE, ...)`로 변환한다.

#### 적용 코드

```java
import org.springframework.dao.EmptyResultDataAccessException;

public User get(Long id) {
    try {
        UserEntity e = userRepository.findById(id);
        return UserMapper.toDomain(e);

    } catch (EmptyResultDataAccessException e) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 사용자입니다. id=" + id
        );
    }
}
```

---

## 2. UPDATE / DELETE 영향 행 수 = 0

`JdbcTemplate.update()`는 영향받은 행 수(int)를 반환한다.
이 값이 0인 경우, 조건에 맞는 대상이 없었음을 의미한다.

---

### 2-1. 닉네임 변경 (`updateNickname`)

닉네임 변경은 **UPDATE 결과 행 수만으로 대상 없음 여부를 판단할 수 없다.**
닉네임이 기존 값과 동일한 경우, 대상은 존재하더라도 변경이 발생하지 않아 결과가 0이 될 수 있다.

따라서 다음 순서로 처리한다.

1. 사전 조회로 대상 존재 여부 확인
2. 동일 값이면 정상 종료
3. 실제 변경 수행

### 파일: `service/UserService.java`

#### 적용 코드

```java
public void changeNickname(Long id, String nickname) {
    // 1) 대상 존재 여부 확인 (없으면 여기서 404)
    User user = get(id);

    // 2) 값이 동일하면 변경 없음 → 정상 처리
    if (user.getNickname().equals(nickname)) {
      return;
    }

    // 3) 실제 변경
    int updated = userRepository.updateNickname(id, nickname);

    if (updated == 0) {
      throw new ApiException(
          ErrorCode.NOT_FOUND_RESOURCE,
          "존재하지 않는 사용자입니다. id=" + id
      );
    }
}
```

---

### 2-2. 비밀번호 변경 (`changePassword`)

### 파일: `service/UserService.java`

#### 적용 코드

```java
public void changePassword(Long id, UserPasswordChangeRequest req) {
    String passwordHash = passwordEncoder.encode(req.getPassword());

    int updated = userRepository.updatePassword(id, passwordHash);

    if (updated == 0) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 사용자입니다. id=" + id
        );
    }
}
```

---

### 2-3. 삭제 (`delete`)

### 파일: `service/UserService.java`

#### 적용 코드

```java
public void delete(Long id) {
    int deleted = userRepository.deleteById(id);

    if (deleted == 0) {
        throw new ApiException(
            ErrorCode.NOT_FOUND_RESOURCE,
            "존재하지 않는 사용자입니다. id=" + id
        );
    }
}
```

---

## 3. 테스트

파일: `404.http`

#### 파일 역할

* 대상 없음(404) 케이스를 고정된 엔드포인트로 검증한다.
* 조회/수정/삭제에서 동일한 에러 코드(`NOT_FOUND_RESOURCE`)가 반환되는지 확인한다.
* DB 기술 예외가 외부로 노출되지 않는지 점검한다.

```http
@baseUrl = http://localhost:8080

### Step1 - 존재하지 않는 사용자 (404)
GET {{baseUrl}}/api/users/999999

### Step2 - 닉네임 변경 대상 없음 (404)
PUT {{baseUrl}}/api/users/999999/nickname?nickname=test

### Step2 - 비밀번호 변경 대상 없음 (404)
PUT {{baseUrl}}/api/users/999999/password
Content-Type: application/json

{
  "password": "1234"
}

### Step2 - 삭제 대상 없음 (404)
DELETE {{baseUrl}}/api/users/999999
```

---

## 체크리스트

* 대상 존재 여부는 Service에서 해석한다.
* 조회 결과 없음 / 영향 행 수 0 → `ApiException(ErrorCode.NOT_FOUND_RESOURCE, ...)`로 변환한다.
* Repository는 결과만 반환하고 의미 판단은 수행하지 않는다.
* DB 기술 예외를 비즈니스 의미(404)로 변환한다.

---

## 다음 단계

* 중복 제약(UNIQUE) 해석 (409)
* 인증/인가(401/403)
