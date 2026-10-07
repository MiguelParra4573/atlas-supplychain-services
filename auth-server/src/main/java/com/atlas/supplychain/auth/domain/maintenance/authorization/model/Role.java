package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    private Long idRol;

    private String idCompany;

    private String name;

    private Set<GrantedPermission> grantedPermissions;

    private boolean state;
}
