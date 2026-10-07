package com.atlas.supplychain.auth.domain.maintenance.authorization.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    private Long id;
    private String tokenHash;
    private String idUser;
    private String idCompany;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}