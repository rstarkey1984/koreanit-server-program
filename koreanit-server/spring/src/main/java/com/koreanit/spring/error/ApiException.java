package com.koreanit.spring.error;

/**
 * 애플리케이션 전용 예외 클래스
 *
 * - 비즈니스 로직 처리 중 발생한 오류를 표현한다
 * - ErrorCode를 함께 담아 GlobalExceptionHandler로 전달한다
 * - RuntimeException을 상속하여 트랜잭션 롤백 및 전파가 가능하다
 */
public class ApiException extends RuntimeException {

    /** 에러의 종류를 나타내는 코드 */
    private final ErrorCode errorCode;

    /**
     * ApiException 생성자
     *
     * @param errorCode 에러 분류용 코드(enum)
     * @param message   클라이언트에게 전달할 에러 메시지
     */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);          // RuntimeException의 메시지 설정
        this.errorCode = errorCode;
    }

    /**
     * 에러 코드 반환
     *
     * GlobalExceptionHandler에서
     * HTTP 상태 코드 매핑에 사용된다
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
