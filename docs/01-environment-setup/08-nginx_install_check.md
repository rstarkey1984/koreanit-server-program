# Nginx 웹 서버 설치 및 동작 확인

*(웹 서버 기본 개념 & 실행 확인)*

이 장에서는
Ubuntu 서버에 **Nginx 웹 서버를 설치하고 실제로 동작하는지 확인**한다.

학생은 이 장을 끝내면
**“웹 서버가 무엇이고, 어떻게 실행되는지”**를 설명할 수 있어야 한다.

---

## 1. 웹 서버란 무엇인가?

웹 서버(Web Server)는
**HTTP 요청을 받아 정적인 응답(HTML, 이미지, 파일 등)을 반환하는 프로그램**이다.

사용자가 브라우저 주소창에 URL을 입력하면
다음과 같은 흐름으로 요청이 처리된다.

```text
브라우저 → 웹 서버 → 응답
```

이 단계에서 웹 서버는

* 요청을 해석하고
* 필요한 리소스를 찾아
* 그대로 응답한다

즉,
**웹 서버는 “요청을 받아서 응답을 돌려주는 역할”에 집중한다.**

---

## 2. 웹 서버의 핵심 역할

![이미지](/img/1_hXuuhny2NRwVopbkfz5caw.png)

웹 서버는
**애플리케이션 코드를 실행하는 프로그램이 아니다.**

대신,
외부 요청을 정리하고 내부 서버로 전달하는 **앞단 관문 역할**을 한다.

웹 서버의 주요 역할은 다음과 같다.

* HTTP 요청 수신
* 정적 파일 제공 (HTML, CSS, JS, 이미지)
* HTTPS 처리 (TLS 종료)
* 요청을 내부 서버(WAS)로 전달
* 비정상 요청의 1차 차단

즉, 웹 서버는
**인터넷과 내부 서버 사이에 위치한 첫 관문**이다.

---

## 3. Nginx 설치

### 3-1. 패키지 목록 갱신

```bash
sudo apt update
```

---

### 3-2. Nginx 설치

```bash
sudo apt install -y nginx
```

설치가 완료되면
Nginx 웹 서버 프로그램이 시스템에 등록된다.

---

## 4. nginx 명령어 확인 (PATH 복습)

### 4-1. nginx 실행 파일 위치 확인

```bash
which nginx
```

출력 예:

```text
/usr/sbin/nginx
```

설명:

* `nginx`도 하나의 실행 파일이다
* PATH에 등록된 경로에 있기 때문에
* **파일명만으로 실행 가능하다**

---

### 4-2. nginx 버전 확인

```bash
nginx -v
```

---

## 5. Nginx 서비스 상태 확인

> Nginx는 **systemd 기반 서비스(데몬)** 로 실행된다.

### 5-1. 서비스 상태 확인

```bash
sudo systemctl status nginx
```

> `systemctl`은 systemd 기반 서비스의
> 시작 / 중지 / 상태 확인 / 자동 실행 설정을 관리하는 명령어다.

정상 실행 상태 예:

```text
Active: active (running)
```

---

## 6. 웹 서버 동작 확인

### 6-1. curl로 확인

```bash
curl http://localhost
```

기본 환영 페이지 HTML이 출력되면
웹 서버가 정상 동작 중이다.

---

### 6-2. 브라우저로 확인 (Windows)

Windows 브라우저 주소창에 입력:

```text
http://localhost
```

**“Welcome to nginx!”** 페이지가 보이면 성공이다.

---

## 7. Nginx 설정하기

### 7-1. `/etc/nginx`를 작업 디렉터리에서 보기 위한 심볼릭 링크

```bash
ln -s /etc/nginx ~/projects/koreanit-server
```

---

### 7-2. 기본 서버 설정 파일 수정

기본설정파일 백업하기:
```bash
sudo cp -rpf /etc/nginx/sites-available/default /etc/nginx/sites-available/default.bak
```
소유자변경:
```
sudo chown ubuntu /etc/nginx/sites-available/default
```

`nginx/sites-available/default` 파일수정

```nginx
server {
    # ----------------------------------------
    # 80 포트에서 들어오는 기본 서버
    # ----------------------------------------
    listen 80 default_server;
    listen [::]:80 default_server;   # IPv6 80 포트도 함께 처리

    # ----------------------------------------
    # 정적 파일(document root) 설정
    # ----------------------------------------
    root /var/www/html;               # 정적 파일이 위치한 디렉터리
    index index.html index.htm index.nginx-debian.html; # 기본으로 찾을 index 파일들

    server_name _;                    # 모든 호스트 이름을 받는 기본 서버

    # ----------------------------------------
    # 정적 리소스 처리
    # ----------------------------------------
    location / {
        # 요청한 파일이 실제로 존재하면 반환
        # 없으면 404 응답
        try_files $uri $uri/ =404;
    }

    # ----------------------------------------
    # API 요청만 Spring Boot로 전달
    # ----------------------------------------
    location /api/ {
        # /api/** 요청을 localhost:8080로 프록시
        proxy_pass http://localhost:8080;

        # 원래 요청한 호스트 이름 전달
        proxy_set_header Host $host;

        # 실제 클라이언트 IP 전달
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $remote_addr;
    }
}
```


---

### 7-3. 웹 문서 경로 접근을 위한 설정

```bash
sudo chown -R ubuntu /var/www
ln -s /var/www ~/projects/koreanit-server/www
```

이제 다음 경로에서 웹 파일을 직접 수정할 수 있다.

```text
~/www/html/index.html
```

HTML 파일을 생성하고 브라우저에서 다시 확인한다.
[index.html 예시](/src/html/index.html)

---

## 이 장의 핵심 정리

* 웹 서버는 HTTP 요청을 받아 응답을 반환하는 프로그램
* Nginx는 대표적인 웹 서버다
* nginx는 PATH에 등록된 실행 파일이다
* systemd 서비스로 관리된다
* curl과 브라우저로 동작을 확인할 수 있다

---

## 다음 단계

→ [**09. MySQL 설치 및 기본 구성**](09-mysql_install_setup.md)

