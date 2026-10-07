package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantedPermission {

    private Long idGrantedPermission;

    private String idCompany;

    private Role role;

    private Operation operation;

}
