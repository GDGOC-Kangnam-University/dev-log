package com.example.jwt_study.exception;

/**
 * 리프레시 토큰 재사용 탐지 예외 (보안 이벤트)
 */
public class TokenReuseDetectedException extends RuntimeException {
    public TokenReuseDetectedException(String message) {
        super(message);
    }
}