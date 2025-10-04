package com.example.jwt_study.repository;

import com.example.jwt_study.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 해시로 조회 (행 잠금 적용, RTR 경쟁 조건 방지)
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashWithLock(@Param("tokenHash") String tokenHash);

    /**
     * 토큰 해시로 조회 (일반 조회, 잠금 없음)
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 사용자 ID로 모든 리프레시 토큰 삭제 (재사용 탐지 시)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 만료된 토큰 정리 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.absoluteExpiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}