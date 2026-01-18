# MySQL 설치 및 기본 구성

이 장에서는
서버 프로그램이 사용할 **MySQL DB를 설치하고 기본 구성을 완료**한다.

이 강의는 이후 단계에서
Repository(SQL) 계층을 구현하고 실제 데이터를 저장/조회해야 하므로,
**DB 설치를 먼저 끝내고** 다음 장으로 넘어간다.

---

## 강의 목표

* Ubuntu(WSL)에서 MySQL 8.x를 설치할 수 있다
* MySQL 서비스가 정상 동작하는지 확인할 수 있다
* 실습용 DB와 전용 계정을 생성하고 권한을 부여할 수 있다
* 서버 애플리케이션이 사용할 접속 정보를 정리할 수 있다

---

## 1. 패키지 업데이트

```bash
sudo apt update
```

(선택)

```bash
sudo apt upgrade -y
```

---

## 2. MySQL 서버 설치

```bash
sudo apt install -y mysql-server
```

설치 확인:

```bash
mysql --version
```

---

## 3. 서비스 동작 확인 (systemd)

```bash
sudo systemctl status mysql
```

```bash
sudo systemctl start mysql
sudo systemctl enable mysql
```

---

## 4. 로컬 접속 확인

```bash
sudo mysql
```

```sql
SELECT VERSION();
```

```sql
exit
```

---

## 5. 실습용 DB / 전용 사용자 생성

```bash
sudo mysql
```

### 5-1. 데이터베이스 생성

```sql
CREATE DATABASE koreanit_service
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

### 5-2. 전용 사용자 생성

```sql
CREATE USER 'koreanit_app'@'localhost'
IDENTIFIED BY 'password';
```

### 5-3. 권한 부여

```sql
GRANT ALL PRIVILEGES
ON koreanit_service.*
TO 'koreanit_app'@'localhost';

FLUSH PRIVILEGES;
```

---

## 6. users 테이블 생성 및 데이터 확인

이제 실제 Repository 단계에서 사용할 수 있도록
간단한 `users` 테이블을 만들고 데이터를 넣어본다.

### 6-1. DB 선택

```sql
USE koreanit_service;
```

### 6-2. users 테이블 생성

```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(100) NOT NULL,
  name VARCHAR(50) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 6-3. 샘플 데이터 INSERT

```sql
INSERT INTO users (email, name) VALUES
('user1@test.com', '사용자1'),
('user2@test.com', '사용자2'),
('user3@test.com', '사용자3');
```

### 6-4. 데이터 조회 (SELECT)

```sql
SELECT * FROM users;
```

이 단계의 목적은

* 테이블 생성 흐름 이해
* INSERT → SELECT 데이터 흐름 확인

이다.

---

## 7. 애플리케이션 접속 정보 정리

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=koreanit_service
DB_USER=koreanit_app
DB_PASSWORD=password
```

---

## 8. 자주 발생하는 문제

### MySQL 서비스 inactive

* systemd 설정 확인
* `wsl --shutdown` 후 재접속

### 포트 확인

```bash
ss -ltnp | grep :3306
```

---

## 이 장의 핵심 정리

* MySQL 설치 및 서비스 확인 완료
* DB / 사용자 / 권한 구성 완료
* users 테이블 생성 및 SELECT 확인
* JDBC 커넥션 풀 설정과 Repository 실습을 위한 DB 준비 완료

---

## 다음 단계

→ [**07. JDBC 커넥션 풀**](07-jdbc_connection_pool.md)
