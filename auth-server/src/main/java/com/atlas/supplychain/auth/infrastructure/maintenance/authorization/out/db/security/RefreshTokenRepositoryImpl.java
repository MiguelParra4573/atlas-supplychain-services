package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.RefreshToken;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.RefreshTokenRepository;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.security.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    @Override
    public void save(RefreshToken token) {
        jpaRepository.save(mapper.domainToEntity(token));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::entityToDomain);
    }

    @Override
    public int revokeAllActiveByIdUserAndCompany(String idUser, String idCompany, Instant now) {
        // idUser sigue String en el contrato de dominio (opaco, ver UserRepositoryImpl) --
        // id_user ya es bigint en BD desde el cambio de tipo de auth-server, se convierte acá.
        return jpaRepository.revokeAllActiveByIdUserAndCompany(Long.parseLong(idUser), idCompany, now);
    }
}
