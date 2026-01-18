# Nginx 설정 파일 구조 이해

*(nginx.conf, sites-available, sites-enabled)*

이 장에서는
Nginx 웹 서버의 **설정 파일 구조와 역할**을 이해한다.

학생은 이 장을 끝내면
"Nginx 설정은 어디서, 어떤 파일을 수정해야 하는지"를 설명할 수 있어야 한다.

---

## 강의 목표

* Nginx 설정 파일이 어디에 있는지 이해한다
* nginx.conf의 역할을 이해한다
* sites-available / sites-enabled 구조를 이해한다
* 설정 파일을 직접 수정하지 않고 구조를 먼저 파악한다

---

## 1. Nginx 설정 파일은 어디에 있을까?

Nginx 설정 파일은 다음 디렉터리에 모여 있다.

```text
/etc/nginx
```

확인:

```bash
ls /etc/nginx
```

출력 예:

```text
nginx.conf
sites-available
sites-enabled
conf.d
snippets
```

---

## 2. nginx.conf의 역할

```text
/etc/nginx/nginx.conf
```

이 파일은 **Nginx 전체 동작 방식**을 정의하는
가장 상위 설정 파일이다.

주요 역할:

* worker 프로세스 수
* 이벤트 처리 방식
* HTTP 기본 설정
* 하위 설정 파일 include

---

### 2-1. nginx.conf 열어보기

```bash
sudo less /etc/nginx/nginx.conf
```

설명 포인트:

* 이 파일은 **거의 수정하지 않는다**
* 실제 사이트 설정은 다른 파일에서 진행

---

## 3. sites-available 디렉터리

```text
/etc/nginx/sites-available
```

의미:

* 사용 가능한 사이트 설정 파일 보관
* 아직 활성화되지 않은 설정 포함 가능

확인:

```bash
ls /etc/nginx/sites-available
```

기본 파일:

```text
default
```

---

## 4. sites-enabled 디렉터리

```text
/etc/nginx/sites-enabled
```

의미:

* 실제로 Nginx가 읽는 사이트 설정
* 활성화된 설정만 존재

확인:

```bash
ls /etc/nginx/sites-enabled
```

보통 결과:

```text
default
```

---

## 5. sites-available ↔ sites-enabled 관계

두 디렉터리는
**심볼릭 링크(symbolic link)** 로 연결된다.

확인:

```bash
ls -l /etc/nginx/sites-enabled
```

출력 예:

```text
default -> /etc/nginx/sites-available/default
```

설명:

* 실제 설정 파일은 `sites-available`에 존재
* `sites-enabled`에는 링크만 존재
* 이 구조로 설정 ON/OFF 관리

---

## 6. conf.d 디렉터리

```text
/etc/nginx/conf.d
```

의미:

* 추가 설정 파일을 넣는 공간
* include 방식으로 자동 로딩

실무에서는:

* 공통 설정
* 모듈별 설정

등에 사용된다.

---

## 7. 설정 구조 요약

```text
/etc/nginx/
├── nginx.conf          (최상위 설정)
├── sites-available/    (사이트 설정 보관)
├── sites-enabled/      (활성화된 사이트)
├── conf.d/             (추가 설정)
└── snippets/           (설정 조각)
```

---

## 이 장의 핵심 메시지

* nginx.conf는 전체 설정의 뿌리
* 실제 사이트 설정은 sites-available에서 관리
* sites-enabled는 활성화 여부만 결정
* 설정 구조를 이해하면 운영이 쉬워진다

---

## 다음 단계

→ [**10. 정적 파일 서비스 및 루트 디렉터리 설정**](10_nginx_static_root.md)

