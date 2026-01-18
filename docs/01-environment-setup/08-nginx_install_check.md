# Nginx 웹 서버 설치 및 동작 확인

*(웹 서버 기본 개념 & 실행 확인)*

이 장에서는
Ubuntu 서버에 **Nginx 웹 서버를 설치하고 실제로 동작하는지 확인**한다.

학생은 이 장을 끝내면
“웹 서버가 무엇이고, 어떻게 실행되는지”를 설명할 수 있어야 한다.

---

## 강의 목표

* 웹 서버(Web Server)의 역할을 이해한다
* Nginx를 패키지 관리자로 설치할 수 있다
* nginx 명령어가 실행되는 원리를 이해한다
* 브라우저와 curl로 웹 서버 동작을 확인할 수 있다

---

## 1. 웹 서버란 무엇인가?

웹 서버는
**요청(HTTP)을 받아서 응답(HTML, 파일 등)을 돌려주는 프로그램**이다.

브라우저에서 주소를 입력하면:

```
브라우저 → 웹 서버 → 응답
```

이 역할을 하는 대표적인 프로그램이
**Nginx**다.

---

## 2. Nginx 설치

### 2-1. 패키지 목록 갱신

```bash
sudo apt update
```

---

### 2-2. Nginx 설치

```bash
sudo apt install -y nginx
```

설치가 완료되면
웹 서버 프로그램이 시스템에 추가된다.

---

## 3. nginx 명령어 확인 (PATH 복습)

### 3-1. nginx 실행 파일 위치 확인

```bash
which nginx
```

출력 예:

```text
/usr/sbin/nginx
```

설명:

* `nginx`도 하나의 실행 파일
* PATH에 등록된 경로에 있기 때문에
  파일명만으로 실행 가능

---

### 3-2. nginx 버전 확인

```bash
nginx -v
```

---

## 4. Nginx 서비스 상태 확인

Nginx는 **systemd 서비스**로 동작한다.

### 4-1. 서비스 상태 확인

```bash
sudo systemctl status nginx
```

정상 상태 예:

```text
Active: active (running)
```

---

### 4-2. 서비스 자동 실행 설정

```bash
sudo systemctl enable nginx
```

* 서버 재부팅 시 자동 실행

---

## 5. 웹 서버 동작 확인

### 5-1. curl로 확인

```bash
curl http://localhost
```

기본 환영 페이지 HTML이 출력되면 성공이다.

---

### 5-2. 브라우저로 확인 (Windows)

Windows 브라우저 주소창에 입력:

```text
http://localhost
```

**“Welcome to nginx!”** 페이지가 보이면
웹 서버가 정상 동작 중이다.

---

## 6. 기본 웹 루트 확인

Nginx 기본 웹 루트 디렉터리:

```text
/var/www/html
```

확인:

```bash
ls /var/www/html
```

`index.nginx-debian.html` 파일이
기본 페이지다.

---

## 이 장의 핵심 정리

* Nginx는 대표적인 웹 서버 프로그램
* nginx도 PATH에 등록된 실행 파일
* systemd로 서비스가 관리된다
* 브라우저와 curl로 동작을 확인할 수 있다

---

## 다음 단계

→ [**09. Nginx 설정 파일 구조 이해**](09_nginx_config_structure.md)