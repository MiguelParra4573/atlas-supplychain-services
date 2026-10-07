package com.atlas.supplychain.auth.domain.maintenance.organization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    private String idCompany;

    private String alias;

    private String businessName;

    private String ruc;

    private String address;

    private String basePath;

    private String cedulaRepresLegal;

    private String legalRepFirstName;

    private String legalRepLastName;

    private boolean state;

}
