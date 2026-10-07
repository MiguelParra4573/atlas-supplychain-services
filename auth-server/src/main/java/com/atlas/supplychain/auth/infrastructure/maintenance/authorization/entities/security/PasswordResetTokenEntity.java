package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name="uk_password_reset_tokens_token_hash", columnList="token_hash", unique = true),
                @Index(name="idx_password_reset_tokens_id_user_id_company", columnList="id_user,id_company"),
                @Index(name="idx_password_reset_tokens_expires_at", columnList="expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="token_hash", nullable=false, length=64)
    private String tokenHash;

    @Column(name="id_user", nullable=false)
    private Long idUser;

    @Column(name="id_company", nullable=false)
    private String idCompany;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Column(name="used_at")
    private Instant usedAt;
}