# 리버스 프록시 (Nginx)

본 장에서는 이미 배포된 API 서버(systemd)를
**외부 트래픽으로부터 보호**하고,
**표준 포트(80/443)** 로 서비스하기 위해
리버스 프록시를 구성한다.

이 단계가 끝나면 서버는 다음 상태가 된다.

- 외부 요청은 Nginx만 접근
- API 서버(8000)는 내부에서만 접근
- 실제 서비스 도메인 기준으로 동작
- HTTPS 적용 준비 완료

> 리버스 프록시를 쓰는 목적은 “접근을 편하게 하려는 것”이 아니라
외부 접근 경로를 하나로 강제하기 위함이다.    
> 그래서 백엔드 포트는 반드시 외부에서 차단한다.

---

## 1. 리버스 프록시란 무엇인가

### 구조 변화

```text
[Client]
   ↓ 80 / 443
[Nginx]
   ↓ 8000
[Spring Boot API]
```

### 리버스 프록시의 역할
- 외부 포트 노출 차단
- 요청/응답 중계
- HTTPS 종단 처리
- 보안 헤더 추가 가능

---

## 2. Nginx 설치

```bash
sudo apt update
sudo apt install nginx -y
```

### 서비스 상태 확인
```bash
sudo systemctl status nginx
```

---

## 3. API 서버 프록시 설정

### 1) 설정 파일 위치
```text
/etc/nginx/sites-available/www.localhost.conf
```

### 2) 설정 파일 예시
```nginx
server {
    listen 80;                 # IPv4 HTTP 요청 수신
    listen [::]:80;            # IPv6 HTTP 요청 수신

    server_name www.localhost; # 이 서버 블록이 처리할 도메인

    root /var/www/html;        # 정적 파일을 제공할 문서 루트 디렉터리

    location / {
        try_files $uri $uri/ /client.html;  # 요청한 경로에 해당하는 정적 파일/디렉터리가 있으면 그대로 응답
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8000;              # /api 요청을 백엔드(Spring Boot)로 전달
        proxy_set_header Host $host;                    # 원래 요청의 Host 헤더 전달
        proxy_set_header X-Real-IP $remote_addr;        # 실제 클라이언트 IP 전달
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; # 프록시 경유 IP 체인 전달
        proxy_set_header X-Forwarded-Proto $scheme;     # 원래 요청 프로토콜(http/https) 전달
    }
}
```

---

## 4. 설정 활성화

```bash
sudo ln -s /etc/nginx/sites-available/www.localhost.conf /etc/nginx/sites-enabled/
```

설정 테스트
```bash
sudo nginx -t
```

재시작
```bash
sudo systemctl reload nginx
```

---

## 5. API 서버 보호하기

```
server:
  address: 127.0.0.1  # 외부에서 API 포트로 직접 접근하지 못하도록 제한
  port: ${PORT}
```

이제 다음 요청은 실패해야 한다.

```text
http://localhost:8000/api/posts
```

정상 접근
```text
http://www.localhost/api/posts
```

---

## 6. HTTPS 고려 사항 (개념)

- HTTPS 적용 시
  - 쿠키 Secure 옵션 활성화
  - SameSite 정책 재검토
- 실제 운영에서는 Certbot(Let's Encrypt) 사용 권장

> HTTPS 설정은 별도 장에서 다룬다.

---


## 체크리스트

- [ ] 외부 포트는 Nginx만 노출되는가
- [ ] API 서버는 내부에서만 접근 가능한가
- [ ] 프록시 헤더가 정상 전달되는가
- [ ] 실제 도메인으로 API 호출이 가능한가

---

## 다음 단계

[**개발 업데이트 후 운영 서버 적용**](05-update-and-deploy.md)
