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
public class RefreshToken {

    private Long id;
    private String tokenHash;
    private String idUser;
    private String idCompany;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String replacedByHash;

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
