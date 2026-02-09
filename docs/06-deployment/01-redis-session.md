# Redis 세션 외부화

본 장에서는 기존 **HttpSession(JVM 메모리)** 기반 인증 구조를
**Redis 기반 세션 저장소**로 전환한다.

이 작업이 끝나면 서버는 다음 상태를 갖는다.

- 서버 재시작 후에도 로그인 유지
- 다중 서버 확장 가능한 구조
- 기존 로그인/인증 코드 수정 없음

---

## 1. 왜 Redis로 세션을 분리해야 하는가

### 기존 구조의 문제점
- 세션이 JVM 메모리에 존재
- 서버 재시작 시 세션 소멸
- 서버가 2대 이상이면 로그인 불일치 발생

### 목표
> API 서버를 **무상태(stateless)** 로 만들고  
> 세션 상태를 **외부 저장소(Redis)** 로 이전한다.

---

## 2. Redis 서버 직접 설치 (Ubuntu 기준)

### 1) Redis 설치
```bash
sudo apt update
sudo apt install redis-server -y
```

### 2) 서비스 상태 확인
```bash
sudo systemctl status redis-server
```

### 3) 부팅 시 자동 실행 확인
```bash
sudo systemctl enable redis-server
```

### 4) 외부 접근 차단 확인
기본 설정은 localhost 바인딩이다.

```bash
sudo nano /etc/redis/redis.conf
```

확인 항목:
```
bind 127.0.0.1 -::1
```
> Redis를 서버 내부 전용 세션 저장소로 사용하겠다는 의미    
> 외부 접근을 원천 차단한 안전한 기본 설정

---

## 3. Spring Session + Redis 적용

### 1) 의존성 추가 (Gradle)
```gradle
// Redis를 HttpSession 저장소로 사용하기 위한 Spring Session 모듈
implementation 'org.springframework.session:spring-session-data-redis'

// Redis 연결 및 직렬화 처리를 위한 Spring Boot Redis 스타터
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### 2) application.yml (공통)
```yaml
spring:
  
  ... (기존 설정) ...

  session:
    store-type: redis

  data:
    redis:
      host: 127.0.0.1
      port: 6379
```

> 이 설정만으로 HttpSession 저장소가 Redis로 자동 전환된다.

---

## 4. 기존 코드 변경 없음

### 인증 코드 예시
```java
session.setAttribute("LOGIN_USER_ID", userId);
```

```java
session.getAttribute("LOGIN_USER_ID");
```

- 코드 수정 ❌
- 인터페이스 변경 ❌
- 저장 위치만 Redis로 변경 ⭕

---

## 5. 동작 확인 실습

### 1) 로그인 수행
```http
POST /api/login
```

### 2) 서버 재시작
```bash
sudo systemctl restart koreanit-api
```

### 3) 로그인 유지 확인
```http
GET /api/me
```

---

## 6. Redis 세션 확인(선택)

```bash
redis-cli
keys *
```

---

## 체크리스트

- [ ] Redis 서비스가 systemd로 관리되는가
- [ ] 서버 재시작 후 로그인 유지되는가
- [ ] 기존 인증 코드 수정이 없는가

---

## 다음 단계

[**환경 분리 + CORS (배포 준비 단계)**](02-env-and-cors.md)

