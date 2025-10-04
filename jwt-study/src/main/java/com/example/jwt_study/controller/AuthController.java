package com.example.jwt_study.controller;

import com.example.jwt_study.config.JwtProperties;
import com.example.jwt_study.domain.User;
import com.example.jwt_study.dto.*;
import com.example.jwt_study.service.AuthService;
import com.example.jwt_study.service.TokenService;
import com.example.jwt_study.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API 컨트롤러
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /**
     * 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getUsername(), request.getPassword());
        UserResponse response = new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
        return ResponseEntity.status(201).body(response);
    }

    /**
     * 로그인 (액세스 토큰: JSON, 리프레시 토큰: HttpOnly 쿠키)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        // 1. 인증
        User user = authService.authenticate(request.getUsername(), request.getPassword());

        // 2. 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 3. 리프레시 토큰 DB 저장 (SHA-256 해싱)
        tokenService.saveRefreshToken(user.getId(), refreshToken);

        // 4. 리프레시 토큰 쿠키 설정
        setRefreshTokenCookie(response, refreshToken);

        // 5. 액세스 토큰 응답
        TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenExpiry()
        );

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 토큰 갱신 (RTR with 행잠금)
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body(null);
        }

        // 1. RTR 실행 (행잠금 + Soft Delete)
        String newRefreshToken = tokenService.rotateRefreshToken(refreshToken);

        // 2. 새 액세스 토큰 발급
        Long userId = jwtUtil.getUserId(newRefreshToken);
        String accessToken = jwtUtil.generateAccessToken(userId, null);

        // 3. 새 리프레시 토큰 쿠키 설정
        setRefreshTokenCookie(response, newRefreshToken);

        // 4. 새 액세스 토큰 응답
        TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenExpiry()
        );

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            tokenService.deleteRefreshToken(refreshToken);
        }

        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * 리프레시 토큰 쿠키 설정
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true) // HTTPS only
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(jwtProperties.getRefreshTokenExpiry())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 리프레시 토큰 쿠키 삭제
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}