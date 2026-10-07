package com.atlas.supplychain.auth.infrastructure.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String privateKeyPath;
    private String publicKeyPath;

    private String issuer;
    /** {@code kid} del header JWT; debe coincidir con el publicado en el JWKS. */
    private String keyId = "auth-server-rsa-1";
    private Integer accessTokenTtlMinutes;
    private Integer refreshTokenTtlDays;
    private Long serviceTokenTtlSeconds = 60L;
}

