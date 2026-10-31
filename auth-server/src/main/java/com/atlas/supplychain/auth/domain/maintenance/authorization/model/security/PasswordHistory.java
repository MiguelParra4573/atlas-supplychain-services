package com.atlas.supplychain.auth.domain.maintenance.authorization.model.security;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
