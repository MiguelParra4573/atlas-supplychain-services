package com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.dto;

import java.time.Instant;

public record ServiceTokenResponse(String token, Instant expiresAt) {
}
