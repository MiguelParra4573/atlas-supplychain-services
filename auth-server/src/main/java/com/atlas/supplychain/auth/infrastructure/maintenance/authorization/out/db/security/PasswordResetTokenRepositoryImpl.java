package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordResetToken;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.PasswordResetTokenRepository;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.security.PasswordResetTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenMapper mapper;

    @Override
    public void save(PasswordResetToken token) {
        jpaRepository.save(mapper.domainToEntity(token));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::entityToDomain);
    }

    @Override
    public int markUsed(Long id, Instant usedAt) {
        return jpaRepository.markUsed(id, usedAt);
    }

    @Override
    public int invalidateAllActive(String idUser, String idCompany, Instant now) {
        return jpaRepository.invalidateAllActive(Long.parseLong(idUser), idCompany, now);
    }
}
