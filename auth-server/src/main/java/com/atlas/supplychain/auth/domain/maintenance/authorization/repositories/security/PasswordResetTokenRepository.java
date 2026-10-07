package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    void save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    int markUsed(Long id, Instant usedAt);

    int invalidateAllActive(String idUser, String idCompany, Instant now);
}
