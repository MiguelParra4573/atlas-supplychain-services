package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
                update RefreshTokenEntity rt
                   set rt.revokedAt = :now
                 where rt.idUser = :idUser
                   and rt.idCompany = :idCompany
                   and rt.revokedAt is null
                   and rt.expiresAt > :now
            """)
    int revokeAllActiveByIdUserAndCompany(
            @Param("idUser") Long idUser,
            @Param("idCompany") String idCompany,
            @Param("now") Instant now
    );

}
