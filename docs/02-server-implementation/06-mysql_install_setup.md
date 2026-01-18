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

MySQL 설치 전에 패키지 목록을 최신으로 만든다.

```bash
sudo apt update
```

(선택) 전체 패키지 업그레이드:

```bash
sudo apt upgrade -y
```

> 수업에서는 시간이 부족하거나 변경 리스크를 줄이고 싶다면
> `upgrade`는 생략하고 `update` + 필요한 패키지 설치만 진행해도 된다.

---

## 2. MySQL 서버 설치

Ubuntu 기본 저장소의 MySQL 서버를 설치한다.

```bash
sudo apt install -y mysql-server
```

설치 확인:

```bash
mysql --version
```

---

## 3. 서비스 동작 확인 (systemd)

WSL에서 systemd를 켜둔 상태(`wsl.conf`)라면
서비스 상태를 확인할 수 있다.

```bash
sudo systemctl status mysql
```

만약 실행 중이 아니면 시작한다.

```bash
sudo systemctl start mysql
```

부팅 시 자동 시작:

```bash
sudo systemctl enable mysql
```

---

## 4. 로컬 접속 확인

Ubuntu 서버 내부에서 MySQL에 접속한다.

```bash
sudo mysql
```

버전 확인:

```sql
SELECT VERSION();
```

접속 종료:

```sql
exit
```

---

## 5. 실습용 DB / 전용 사용자 생성

> root 계정으로 애플리케이션이 직접 접속하는 것은 권장하지 않는다.
> 실습도 **전용 사용자**를 만들어 진행한다.

MySQL 접속:

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

### 5-4. 생성 확인

```sql
SHOW DATABASES;
SELECT user, host FROM mysql.user;
```

종료:

```sql
exit
```

---

## 6. 접속 정보 정리 (애플리케이션용)

이후 Spring Boot에서 사용할 접속 정보는 다음과 같다.

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=koreanit_service
DB_USER=koreanit_app
DB_PASSWORD=password
```

> 실제 수업에서는 비밀번호를 단순하게 두되,
> 나중에 운영 파트에서 환경변수/비밀관리로 확장한다.

---

## 7. 자주 발생하는 문제

### 7-1. MySQL 서비스가 "inactive" 인 경우

* systemd 설정이 적용되지 않았을 수 있다
* `04_wsl_ubuntu_setup`에서 `wsl.conf`의 `systemd=true`를 확인하고
  Windows에서 `wsl --shutdown` 후 재접속한다

### 7-2. 포트 확인

```bash
ss -ltnp | grep :3306
```

---

## 이 장의 핵심 정리

* MySQL을 설치하고 서비스 동작을 확인했다
* 실습용 DB와 전용 사용자를 만들었다
* 이후 Repository(SQL) 구현을 위한 준비가 끝났다

---

## 다음 단계

→ [**07. Repository 계층과 SQL**](07-repository_layer_sql.md)

