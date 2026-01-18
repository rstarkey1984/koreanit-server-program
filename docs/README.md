# 서버 프로그램 구현 – 강의 안내

이 문서는
**이 저장소의 강의 진행 순서와 학습 흐름을 안내하는 문서**다.

강의는 반드시 **아래 순서대로** 진행한다.

> 이 README는 "어떻게 학습할 것인가"에 대한 안내서이며,
> 저장소 전체 소개는 루트 `README.md`를 참고한다.

---

## 1. 개발환경 구축 (01 ~ 11)

서버 프로그램을 개발하기 전에
**실제 서버와 동일한 환경을 먼저 구성**한다.

이 단계에서는 **서버 프로그램 코드를 작성하지 않는다**.

### 학습 목표

* 서버 프로그램의 전체 구조 이해
* Linux / Ubuntu 환경 적응
* Web Server(Nginx) 역할 이해
* Git을 통한 서버 작업 관리 습관 형성

### 문서 목록

* [01. 서버프로그램 구현 과정 소개](01-environment-setup/01_intro.md)
* [02. 왜 서버 프로그램은 리눅스를 사용하는가](01-environment-setup/02-why_linux.md)
* [03. 왜 Ubuntu를 사용하는가](01-environment-setup/03-why_ubuntu.md)
* [04. WSL2 + Ubuntu 환경 구성](01-environment-setup/04-wsl_ubuntu_setup.md)
* [05. Ubuntu 서버 기본 세팅](01-environment-setup/05-ubuntu_server_setup.md)
* [06. 필수 서버 패키지 설치](01-environment-setup/06-required_server_packages.md)
* [07. 리눅스 서버 기초 맛보기](01-environment-setup/07-linux_server_basics.md)
* [08. Nginx 웹 서버 설치 및 동작 확인](01-environment-setup/08-nginx_install_check.md)
* [09. Nginx 설정 파일 구조 이해](01-environment-setup/09-nginx_config_structure.md)
* [10. 정적 파일 서비스 및 루트 디렉터리 설정](01-environment-setup/10-nginx_static_root.md)
* [11. Git으로 서버 프로젝트 관리](01-environment-setup/11_git_server_project.md)

---

## 2. 서버 프로그램 구현 (12 ~ )

이 단계부터
**실제 서버 프로그램 코드를 작성**한다.

### 학습 목표

* Web Server와 WAS 역할 구분
* Spring Boot 기반 서버 구현
* HTTP 요청 처리 구조 이해
* DB 연동 및 CRUD 구현

### 문서 목록 (예정)

* 12. 웹 서버와 WAS 개념 이해
* 13. Spring Boot 실행 환경 구성
* 14. Controller / Service / Repository 구조

---

## 3. 공통 모듈 설계

* 공통 응답 구조
* 예외 처리
* 인증 / 인가
* 로깅

---

## 4. 배치 / 워커 서버 분리

* 메인 서버 vs 워커 서버
* Node.js 기반 배치 처리
* 비동기 작업 구조
  n

---

## 학습 안내

* 모든 실습은 **리눅스 서버 환경** 기준으로 진행된다
* ORM(JPA)은 사용하지 않는다
* SQL 중심 Repository 패턴을 사용한다
* 역할 분리를 가장 중요한 설계 기준으로 삼는다
