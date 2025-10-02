package com.example.jwt_study.exception;

/**
 * 토큰 만료 예외
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}