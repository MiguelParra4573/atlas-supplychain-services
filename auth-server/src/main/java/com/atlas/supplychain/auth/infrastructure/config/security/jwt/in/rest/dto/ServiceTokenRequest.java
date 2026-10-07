package com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ServiceTokenRequest(@NotBlank String serviceName) {
}
