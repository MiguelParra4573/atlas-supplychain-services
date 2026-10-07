package com.atlas.supplychain.auth.domain.maintenance.authorization.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistory {

    private Long id;

    private String idUser;

    private String idCompany;

    private String passwordHash;

    private Instant createdAt;
}
