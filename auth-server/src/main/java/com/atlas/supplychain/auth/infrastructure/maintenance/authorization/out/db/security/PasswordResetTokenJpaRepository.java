package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.usedAt = :usedAt where t.id = :id and t.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("""
                update PasswordResetTokenEntity t
                   set t.usedAt = :now
                 where t.idUser = :idUser
                   and t.idCompany = :idCompany
                   and t.usedAt is null
                   and t.expiresAt > :now
            """)
    int invalidateAllActive(@Param("idUser") Long idUser,
                            @Param("idCompany") String idCompany,
                            @Param("now") Instant now);
}
