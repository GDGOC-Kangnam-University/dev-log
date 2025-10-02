package com.example.jwt_study.exception;

import com.example.jwt_study.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러 (응답 표준화)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 토큰 재사용 감지 예외 (보안 이벤트)
     */
    @ExceptionHandler(TokenReuseDetectedException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuseDetected(TokenReuseDetectedException e) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("invalid_grant", e.getMessage()));
    }

    /**
     * 토큰 만료 예외
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException e) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("token_expired", e.getMessage()));
    }

    /**
     * 유효하지 않은 토큰 예외
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("invalid_token", e.getMessage()));
    }

    /**
     * 인증 실패 예외 (잘못된 사용자명/비밀번호)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("invalid_credentials", e.getMessage()));
    }

    /**
     * 중복 사용자명 예외
     */
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsername(DuplicateUsernameException e) {
        return ResponseEntity.status(409)
                .body(new ErrorResponse("duplicate_username", e.getMessage()));
    }

    /**
     * 유효성 검증 실패 예외
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(400)
                .body(new ErrorResponse("validation_failed", message));
    }

    /**
     * 기타 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse("internal_server_error", "서버 오류가 발생했습니다"));
    }
}