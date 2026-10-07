package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.application.security.contracts.PasswordResetTokenStore;
import com.atlas.supplychain.auth.application.security.dto.ResetTokenData;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordResetToken;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenStoreImpl implements PasswordResetTokenStore {

    private final PasswordResetTokenRepository repository;

    @Override
    @Transactional
    public void invalidateAllActive(String idUser, String companyId, Instant now) {
        repository.invalidateAllActive(idUser, companyId, now);
    }

    @Override
    @Transactional
    public void save(String idUser, String companyId, String tokenHash, Instant expiresAt) {
        repository.save(PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .idUser(idUser)
                .idCompany(companyId)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build());
    }

    @Override
    public Optional<ResetTokenData> findByHash(String hash) {
        return repository.findByTokenHash(hash).map(this::toResetTokenData);
    }

    @Override
    @Transactional
    public boolean markUsed(Long id, Instant usedAt) {
        return repository.markUsed(id, usedAt) > 0;
    }

    private ResetTokenData toResetTokenData(PasswordResetToken token) {
        return new ResetTokenData(
                token.getId(),
                token.getIdUser(),
                token.getIdCompany(),
                token.getExpiresAt(),
                token.getUsedAt() != null
        );
    }
}
