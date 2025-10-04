-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt hash',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 리프레시 토큰 테이블 (RTR + 경쟁 조건 방지)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hex',
    expires_at TIMESTAMP NOT NULL COMMENT 'Idle expiration (renewable)',
    absolute_expires_at TIMESTAMP NOT NULL COMMENT 'Absolute expiration (fixed)',

    -- RTR 경쟁 조건 방지
    used_at TIMESTAMP NULL COMMENT 'Marked when used in RTR',
    revoked BOOLEAN DEFAULT FALSE COMMENT 'Revoked flag for reuse detection',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_token_hash (token_hash),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_revoked_used (revoked, used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;