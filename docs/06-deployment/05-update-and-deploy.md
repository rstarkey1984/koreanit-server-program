# 개발 업데이트 후 운영 서버 적용

본 문서는 **개발 서버에서 빌드 → 운영(실습) 서버에 jar만 교체**하는
표준 적용 절차를 기술한다.

본 절차는 다음 전제를 따른다.

- 운영 서버에서 git pull / build 하지 않는다
- 환경변수, 설정 파일은 변경하지 않는다
- jar 파일만 교체하여 반영한다

---

## 1. 적용 시나리오 정의

### 서버 역할 분리

| 구분 | 역할 |
|---|---|
| 개발 서버 | 코드 수정, 빌드 |
| 운영(실습) 서버 | jar 실행, 서비스 운영 |

### 운영 서버 고정 정보

- 서비스명: `koreanit-api`
- 실행 jar: `/opt/koreanit-api/app.jar`
- 환경변수: `/opt/koreanit-api/config/.env`
- 배포 방식: systemd

---

## 2. 표준 적용 절차 (요약)

1. 개발 서버에서 빌드
2. 운영 서버로 jar 업로드
3. 기존 jar 백업
4. jar 교체
5. 서비스 재시작
6. 로그 확인

---

## 3. 개발 서버 작업

### 1) 빌드
```bash
./gradlew clean build
```

### 2) jar 파일 확인
```text
build/libs/
└─ koreanit-api.jar
```

> jar 파일명은 고정하는 것을 권장한다.

---

## 4. 운영 서버로 jar 업로드

원격서버 일때 ( 예시 )
```bash
scp build/libs/spring-0.0.1-SNAPSHOT.jar ubuntu@<SERVER_IP>:/tmp/app.jar
```

로컬서버 일때 
```bash
cp build/libs/spring-0.0.1-SNAPSHOT.jar /tmp/app.jar
```
---

## 5. 운영 서버 적용 절차

### 1) 기존 jar 백업
```bash
sudo cp /opt/koreanit-api/app.jar /opt/koreanit-api/app.jar.bak
```

### 2) jar 교체
```bash
sudo mv /tmp/app.jar /opt/koreanit-api/app.jar
sudo chmod 644 /opt/koreanit-api/app.jar
```

### 3) 서비스 재시작
```bash
sudo systemctl restart koreanit-api
sudo systemctl status koreanit-api
```

---

## 6. 로그 확인

```bash
journalctl -u koreanit-api -f
```

---

## 7. 배포 스크립트 방식 (권장)

운영 서버에 다음 스크립트를 미리 준비한다.

```bash
sudo touch /opt/koreanit-api/deploy.sh
sudo chown ubuntu /opt/koreanit-api/deploy.sh
sudo chmod +x /opt/koreanit-api/deploy.sh
code /opt/koreanit-api/deploy.sh
```

`/opt/koreanit-api/deploy.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

SERVICE="koreanit-api"
APP_DIR="/opt/koreanit-api"
NEW_JAR="/tmp/app.jar"
APP_JAR="$APP_DIR/app.jar"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BAK_JAR="$APP_DIR/app.jar.$TIMESTAMP.bak"

if [[ ! -f "$NEW_JAR" ]]; then
  echo "ERROR: $NEW_JAR not found"
  exit 1
fi

echo "[1] backup"
if [[ -f "$APP_JAR" ]]; then
  sudo cp "$APP_JAR" "$BAK_JAR"
  echo "backup created: $(basename "$BAK_JAR")"
fi

echo "[2] replace jar"
sudo mv "$NEW_JAR" "$APP_JAR"
sudo chown root:root "$APP_JAR"
sudo chmod 644 "$APP_JAR"

echo "[3] restart"
sudo systemctl restart "$SERVICE"

echo "[4] status"
sudo systemctl --no-pager -l status "$SERVICE"

echo "[5] logs"
sudo journalctl -u "$SERVICE" -n 30 --no-pager
```

배포 스크립프 실행:
```bash
bash /opt/koreanit-api/deploy.sh
```

---

## 장애 발생 시 롤백

```bash
sudo cp /opt/koreanit-api/app.jar.bak /opt/koreanit-api/app.jar
sudo systemctl restart koreanit-api
```

---

## 체크리스트

- [ ] 개발 서버에서 빌드했는가
- [ ] jar만 교체했는가
- [ ] 서비스가 정상 기동되는가
- [ ] 로그에 에러가 없는가

---

이 문서는
**운영 서버에서 가장 많이 반복되는 작업**을
실수 없이 수행하기 위한 기준 문서다.

## 다음 단계

**Git 업로드 & 릴리즈**