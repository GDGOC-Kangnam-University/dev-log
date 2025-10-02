package com.example.jwt_study.service;

import com.example.jwt_study.config.JwtProperties;
import com.example.jwt_study.domain.RefreshToken;
import com.example.jwt_study.exception.TokenExpiredException;
import com.example.jwt_study.exception.TokenReuseDetectedException;
import com.example.jwt_study.repository.RefreshTokenRepository;
import com.example.jwt_study.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * 토큰 관리 서비스 (SHA-256 해싱, RTR with 행잠금)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /**
     * SHA-256 단방향 해싱
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 리프레시 토큰 저장 (로그인 시)
     */
    @Transactional
    public void saveRefreshToken(Long userId, String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiry());
        LocalDateTime absoluteExpiresAt = now.plusSeconds(jwtProperties.getRefreshTokenAbsoluteExpiry());

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .build();

        refreshTokenRepository.save(entity);
        log.info("리프레시 토큰 저장 완료: userId={}", userId);
    }

    /**
     * 리프레시 토큰 갱신 (RTR with 행잠금)
     * RFC 6749 - Refresh Token Rotation
     */
    @Transactional
    public String rotateRefreshToken(String oldRefreshToken) {
        // 1. JWT 서명 검증
        jwtUtil.validateTokenType(oldRefreshToken, "refresh");
        Long userId = jwtUtil.getUserId(oldRefreshToken);

        // 2. DB 조회 (SELECT FOR UPDATE - 행잠금)
        String tokenHash = hashToken(oldRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashWithLock(tokenHash)
                .orElseThrow(() -> {
                    // 재사용 탐지: 이미 삭제되었거나 사용된 토큰
                    log.warn("리프레시 토큰 재사용 탐지: userId={}, tokenHash={}", userId, tokenHash.substring(0, 8) + "...");
                    refreshTokenRepository.deleteAllByUserId(userId);
                    return new TokenReuseDetectedException("리프레시 토큰 재사용이 감지되었습니다");
                });

        // 3. 이미 사용된 토큰 확인 (경쟁 조건 방지)
        if (storedToken.isUsed()) {
            log.warn("이미 사용된 리프레시 토큰: userId={}", userId);
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new TokenReuseDetectedException("이미 사용된 리프레시 토큰입니다");
        }

        // 4. 만료 확인 (Absolute 우선)
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new TokenExpiredException("리프레시 토큰이 만료되었습니다");
        }

        // 5. 기존 토큰 사용 마킹 (Soft Delete)
        storedToken.markAsUsed();
        refreshTokenRepository.save(storedToken);

        // 6. 새 리프레시 토큰 발급
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        String newTokenHash = hashToken(newRefreshToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiry());
        // Absolute는 기존 값 유지
        LocalDateTime absoluteExpiresAt = storedToken.getAbsoluteExpiresAt();

        RefreshToken newToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(newTokenHash)
                .expiresAt(newExpiresAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .build();

        refreshTokenRepository.save(newToken);
        log.info("리프레시 토큰 갱신 완료: userId={}", userId);

        return newRefreshToken;
    }

    /**
     * 리프레시 토큰 삭제 (로그아웃 시)
     */
    @Transactional
    public void deleteRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    log.info("리프레시 토큰 삭제 완료: userId={}", token.getUserId());
                });
    }
}