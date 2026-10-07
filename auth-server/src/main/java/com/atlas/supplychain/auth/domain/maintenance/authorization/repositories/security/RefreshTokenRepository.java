package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int revokeAllActiveByIdUserAndCompany(String idUser, String idCompany, Instant now);
}
