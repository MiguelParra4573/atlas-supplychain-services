package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "uk_refresh_tokens_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_refresh_tokens_id_user", columnList = "id_user")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hex

    @Column(name = "id_user", nullable = false)
    private Long idUser;

    @Column(name = "id_company", nullable = false)
    private String idCompany;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_hash", length = 64)
    private String replacedByHash;

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }}
