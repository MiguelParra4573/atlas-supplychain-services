package com.atlas.supplychain.auth.application.security.contracts;

import com.atlas.supplychain.auth.application.security.dto.ResetTokenData;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenStore {
    void invalidateAllActive(String idUser, String companyId, Instant now);

    void save(String idUser, String companyId, String tokenHash, Instant expiresAt);

    Optional<ResetTokenData> findByHash(String hash);

    boolean markUsed(Long id, Instant usedAt);
}
