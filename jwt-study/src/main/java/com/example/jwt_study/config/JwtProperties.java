package com.example.jwt_study.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정값
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secretKey;
    private long accessTokenExpiry; // 초 단위
    private long refreshTokenExpiry; // 초 단위
    private long refreshTokenAbsoluteExpiry; // 초 단위
}