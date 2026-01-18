# Common Modules

이 파트는
02-server-implementation에서 만든 서버를 기반으로,
여러 기능에서 반복되는 코드를 **공통 모듈(common modules)** 로 분리한다.

목표는
"기능을 더 만든다"가 아니라,
**서버 품질을 올리는 공통 규칙과 재사용 가능한 구성요소를 만든다**는 것이다.

---

## 이 파트에서 다루는 것

* 공통 응답 포맷(성공/실패)
* 공통 예외 및 에러 코드
* 공통 예외 처리기(Global Exception Handler)
* 요청 로깅(요청/응답 로그의 기준)
* (선택) 설정 분리: application.yml 프로파일(dev/prod)

---

## 문서 구성 (폴더 내부 번호 기준)

1. 공통 응답 포맷 (`01-common_response_format.md`)
2. 에러 코드와 공통 예외 (`02-error_codes_and_exceptions.md`)
3. Global Exception Handler (`03-global_exception_handler.md`)
4. 요청 로깅 미들웨어/필터 (`04-request_logging.md`)
5. 환경별 설정 프로파일 (`05-profiles_and_config.md`)

---

## 강의 시작

→ [**공통 응답 포맷**](01-common_response_format.md)
