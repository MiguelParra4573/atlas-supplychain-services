package com.atlas.supplychain.auth.application.security.dto;

import java.time.Instant;

public record ResetTokenData(
        Long id,
        String idUser,
        String companyId,
        Instant expiresAt,
        boolean used
) {
    public boolean isValid(Instant now) {
        return !used && now.isBefore(expiresAt);
    }
}
