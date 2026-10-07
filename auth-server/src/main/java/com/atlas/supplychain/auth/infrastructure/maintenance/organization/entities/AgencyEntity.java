package com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "agencies", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id_agency", length = 36)
    private String idAgency;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(nullable = false)
    private boolean state;
}