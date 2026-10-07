package com.atlas.supplychain.auth.infrastructure.common.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErrorResponse(
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,
        String correlationId) {
}
