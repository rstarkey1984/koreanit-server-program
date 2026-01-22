# MySQL 설치 및 기본 구성

이 장에서는
서버 프로그램이 사용할 **MySQL DB를 설치하고 기본 구성을 완료**한다.

이 강의는 이후 단계에서
Repository(SQL) 계층을 구현하고 실제 데이터를 저장/조회해야 하므로,
**DB 설치를 먼저 끝내고** 다음 장으로 넘어간다.


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

## 6. 애플리케이션 접속 정보 정리

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=koreanit_service
DB_USER=koreanit_app
DB_PASSWORD=password
```

## 7. 실습용 테이블 생성 및 시드 데이터 추가

### 1. vscode 에서 `sql` 폴더 생성

### 2. GitHub 강의 사이트 `sql` 경로에 `schema.sql`, `seed_data.sql` 을 다운로드 받아서 생성한 폴더에 넣기

### 3. 터미널에서 `sql` 경로로 이동

### 4. (테이블 생성) schema.sql 파일 있는 위치에서 아래 명령 실행:
```bash
sudo mysql koreanit_service < schema.sql
```

### 5. (시드데이터 추가) seed_data.sql 파일 있는 위치에서 아래 명령 실행:
```bash
sudo mysql koreanit_service < seed_data.sql
```


### 6. 확인

```bash
sudo mysql koreanit_service -e "SELECT COUNT(*) FROM users;"
```

```bash
sudo mysql koreanit_service -e "SELECT COUNT(*) FROM posts;"
```

```bash
sudo mysql koreanit_service -e "SELECT COUNT(*) FROM comments;"
```

---

## 이 장의 핵심 정리

* MySQL 설치 및 서비스 확인 완료
* DB / 사용자 / 권한 구성 완료
* 테이블 생성 및 시드 데이터 추가

---

## 다음 단계

→ [**09. Nginx 웹 서버 설치 및 동작 확인**](09-nginx_install_check.md)
