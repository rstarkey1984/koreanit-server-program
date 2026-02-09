# Git 업로드 & 릴리즈

본 장에서는 지금까지 만든 서버를
**Git에 안전하게 업로드**하고,
**배포 가능한 릴리즈 상태**로 마무리한다.

이 단계가 끝나면 다음이 보장된다.

- Git clone만으로 프로젝트 구조 파악 가능
- 민감 정보 유출 없음
- 운영 배포 재현 가능
- 배포 버전 추적 가능

---

## 1. Git에 절대 올리면 안 되는 것들

### 민감 정보
- DB 비밀번호
- Redis 접속 정보
- 운영 환경변수 파일
- 토큰 / 시크릿

### 운영 파일
- systemd 실제 서비스 파일
- nginx 실제 설정 파일
- 로그 파일
- 빌드 산출물

---

## 2. .gitignore 정리

```gitignore
# Gradle
.gradle/
build/

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store

# Logs
logs/
*.log

# Env / Secrets
.env
*.env
config/env
application-prod.yml
```

---
