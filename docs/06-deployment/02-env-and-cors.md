# 환경 분리 + CORS (배포 준비 단계)

본 장은 **실제 배포를 하기 전 반드시 거쳐야 하는 단계**다.

이 단계가 끝나면 서버는 다음 상태가 된다.

* dev / prod 환경이 명확히 분리됨
* 민감 정보가 코드에서 제거됨
* 프론트엔드 요청을 고려한 CORS 구조 완성

> 아직 systemd 배포는 하지 않는다.
> 이 단계는 **배포 준비(Deployment Preparation)** 단계다.

---

## 1. 왜 환경 분리가 필요한가

### 문제 상황

* `application.yml` 하나로 개발/운영을 같이 사용
* 운영 DB 정보가 코드에 포함됨
* Git에 업로드할 수 없는 상태

### 목표

> 실행 환경(dev / prod)에 따라
> **설정만 바뀌고 코드는 그대로** 유지되는 구조

---

## 2. Profiles 기반 환경 분리

### 1) 설정 파일 구조

```text
src/main/resources/
├─ application.yml
├─ application-dev.yml
└─ application-prod.yml
```

역할:

* `application.yml` : 공통 기본값
* `application-dev.yml` : 로컬 개발용
* `application-prod.yml` : 운영용 (실제 값 없음)

---

### 2) application.yml (공통)

```yaml
spring:
  forward-headers-strategy: framework

  profiles:
    default: dev

logging:
  level:
    root: INFO
```

> 별도의 Profile 지정이 없을 경우
> 기본적으로 `dev` 환경으로 실행된다.

---

### 3) application-dev.yml

```yaml
spring: 

  application:
    name: spring

  datasource:
    url: jdbc:mysql://localhost:3306/koreanit_service?serverTimezone=Asia/Seoul&characterEncoding=utf8
    username: koreanit_app
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: 127.0.0.1
      port: 6379

  session:
    store-type: redis
```

> 개발 환경에서는 **직접 값 명시 허용**

---

### 4) application-prod.yml

```yaml
server:
  port: ${PORT}

spring:

  application:
    name: spring

  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

  session:
    store-type: redis
```

> 운영 환경에서는 **실제 값을 절대 코드에 넣지 않는다**
> 모든 값은 환경변수로만 주입한다.

---

## 3. 민감 정보 분리 (환경변수)

### 코드에서 제거해야 할 정보

* DB 접속 정보
* Redis 접속 정보
* 운영용 CORS Origin
* 포트 번호

---

### 환경변수 파일 위치 예시

(`/opt/koreanit-api/config/.env`)

```bash
sudo mkdir -p /opt/koreanit-api/config/
sudo touch /opt/koreanit-api/config/.env
sudo chown ubuntu /opt/koreanit-api/config/.env
```

---

### 프로젝트 경로에 링크 파일 생성

```bash
ln -s /opt/koreanit-api/config/.env ~/projects/koreanit-server/spring/.env
```

> Git에는 **링크 파일만 존재**
> 실제 값은 서버에만 존재

---

### .env 파일 내용 예시

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8000

DB_URL=jdbc:mysql://localhost:3306/koreanit_service
DB_USER=koreanit_app
DB_PASSWORD=password

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
```

---

## 4. 프로필 전환 방법

Spring Boot는 실행 시점에 활성화된 Profile에 따라
`application-{profile}.yml` 설정을 로드한다.

---

### 환경변수 방식 (권장)

`.env` 파일에 Profile을 정의한다.

```bash
SPRING_PROFILES_ACTIVE=prod
```

`.env` 파일로 **환경변수를 로드한 뒤 실행**한다.

```bash
set -a      # source로 읽은 변수를 자동 export
source .env
set +a

./gradlew bootRun
```

> Spring Boot는 `.env` 파일을 직접 읽지 않는다.   
> `.env`는 **환경변수를 만들기 위한 편의 파일**이며,    
> 실제로는 OS 환경변수만 사용된다.

---

### 환경변수 직접 설정

```bash
export SPRING_PROFILES_ACTIVE=dev
```

### 또는 환경변수 제거

```bash
unset SPRING_PROFILES_ACTIVE
```

---

## 5. 환경 분리 단계 요약

이 시점에서 서버는 다음 조건을 만족해야 한다.

* 실행 환경(dev / prod)에 따라 설정이 자동으로 분리됨
* 동일한 코드로 로컬 / 운영 실행 가능
* 운영 비밀값이 Git에 포함되지 않음

---

# CORS 설계 (프론트엔드 연동 준비)

본 장은 **브라우저 기반 프론트엔드와 연동하기 위한 필수 단계**다.

---

## 1. CORS의 실제 의미

### 오해

* CORS = 서버 보안 ❌

### 실제

* CORS = **브라우저 정책**
* 서버는 허용 여부만 명시

> Postman, curl, 서버 간 통신에는
> CORS가 적용되지 않는다.

---

## 2. 세션 기반 CORS 설계 포인트

세션 + 쿠키 구조에서는 다음이 중요하다.

* `allowCredentials = true`
* `allowedOrigins` 는 반드시 명시
* `"*"` 사용 금지

---

## 3. 환경별 CORS 설정

### `security/cors/DevCorsConfig.java`

```java
@Configuration
@Profile("dev")
public class DevCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

### `security/cors/ProdCorsConfig.java`

```java
@Configuration
@Profile("prod")
public class ProdCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
            "http://localhost"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## 4. SecurityConfig에서 CORS 활성화

```java
.cors(cors -> {})
```

---

## 5. 프론트엔드 테스트 파일 배포

```bash
cp src/main/resources/static/client.html ~/projects/koreanit-server/www/html/
```

브라우저 접속:

```
http://localhost/client.html
```

---

## 체크리스트

* [ ] dev / prod 설정 파일이 분리되었는가
* [ ] 운영 비밀값이 코드에 존재하지 않는가
* [ ] CORS Origin이 환경별로 제어되는가
* [ ] 세션 + 쿠키 요청이 정상 동작하는가

---

## 다음 단계

**배포 (systemd 기반 운영)**
