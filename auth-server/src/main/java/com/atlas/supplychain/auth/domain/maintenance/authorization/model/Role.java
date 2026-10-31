package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
