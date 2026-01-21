# 정적 파일 서비스 및 루트 디렉터리 설정

*(Nginx root, index, 정적 파일 제공)*

이 장에서는
Nginx를 사용해 **정적 파일을 서비스하는 기본 구조**를 이해하고,
웹 서버의 **루트 디렉터리(root)** 개념을 명확히 한다.


---

## 1. 정적 파일이란?

정적 파일이란
**서버에서 그대로 전달되는 파일**이다.

예:

* HTML
* CSS
* JavaScript
* 이미지 파일

특징:

* 서버 로직 실행 없음
* 요청 → 파일 → 응답

---

## 2. Nginx 기본 루트 디렉터리

Ubuntu에서 Nginx 기본 루트 디렉터리는 다음과 같다.

```text
/var/www/html
```

확인:

```bash
ls /var/www/html
```

기본 파일:

```text
index.nginx-debian.html
```

---

## 3. 기본 index 파일의 역할

브라우저에서 다음 주소로 접속하면:

```text
http://localhost/
```

Nginx는

1. root 디렉터리를 찾고
2. index 파일을 자동으로 응답한다

이 동작은 설정 파일에 정의되어 있다.

---

## 4. 정적 파일 직접 수정해보기

기본 index 파일을 열어본다.

```bash
sudo nano /var/www/html/index.nginx-debian.html
```

내용 일부를 다음과 같이 수정한다.

```html
<h1>Hello Nginx</h1>
<p>Static file served by Nginx</p>
```

저장 후 브라우저 새로고침(F5)을 한다.

---

## 5. 정적 파일 추가해보기

새 HTML 파일을 하나 추가한다.

```bash
sudo nano /var/www/html/test.html
```

내용:

```html
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>Nginx Test</title>
</head>
<body>
  <h1>Nginx Static Test</h1>
</body>
</html>
```

브라우저에서 접속:

```text
http://localhost/test.html
```

---

## 6. 서버에서 파일 권한 간단 설명

웹 서버는
**파일을 읽을 수 있어야 응답 가능**하다.

```bash
ls -l /var/www/html
```

설명 포인트:

* 읽기 권한(r)
* 웹 서버 사용자(www-data)

---

## 이 장의 핵심 정리

* 정적 파일은 서버에서 그대로 전달된다
* Nginx는 root 디렉터리에서 파일을 찾는다
* index 파일은 기본 응답 파일이다
* 파일을 수정하면 즉시 결과가 반영된다

---

## 다음 단계

→ [**11. Git을 이용한 서버 프로젝트 관리**](11-git_server_project.md)

