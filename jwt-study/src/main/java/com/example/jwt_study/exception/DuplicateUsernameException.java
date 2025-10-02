package com.example.jwt_study.exception;

/**
 * 중복된 사용자명 예외
 */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}