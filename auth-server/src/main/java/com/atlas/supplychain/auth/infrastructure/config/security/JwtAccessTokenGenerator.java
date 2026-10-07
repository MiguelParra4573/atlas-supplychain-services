package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.security.contracts.AccessTokenClaims;
import com.atlas.supplychain.auth.application.security.contracts.AccessTokenGenerator;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAccessTokenGenerator implements AccessTokenGenerator {

    private final JwtService jwtService;

    @Override
    public String generate(AccessTokenClaims claims) {
        return jwtService.generarAccessToken(claims);
    }

    @Override
    public long ttlSeconds() {
        return jwtService.accessTokenTtlSeconds();
    }

}