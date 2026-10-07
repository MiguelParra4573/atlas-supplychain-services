package com.atlas.supplychain.auth.domain.maintenance.organization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agency {

    private String idAgency;

    private String idCompany;

    private String name;

    private String address;

    private boolean state;

}
