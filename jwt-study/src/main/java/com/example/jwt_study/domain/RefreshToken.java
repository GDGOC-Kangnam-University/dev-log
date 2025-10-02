package com.example.jwt_study.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 엔티티 (RTR + 경쟁 조건 방지)
 * RFC 6749 - Refresh Token Rotation 구현
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hex

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // Idle 만료 (갱신 가능)

    @Column(name = "absolute_expires_at", nullable = false, updatable = false)
    private LocalDateTime absoluteExpiresAt; // 절대 만료 (불변)

    // RTR 경쟁 조건 방지 필드
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked", nullable = false)
    private Boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(Long userId, String tokenHash, LocalDateTime expiresAt, LocalDateTime absoluteExpiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
        this.revoked = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 토큰 사용 마킹 (RTR 시 호출, 재사용 감지 목적)
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
        this.revoked = true;
    }

    /**
     * Idle 만료 시간 갱신 (RTR 시 호출, absolute는 불변)
     */
    public void renewExpiresAt(LocalDateTime newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return expiresAt.isBefore(now) || absoluteExpiresAt.isBefore(now);
    }

    public boolean isUsed() {
        return revoked || usedAt != null;
    }
}