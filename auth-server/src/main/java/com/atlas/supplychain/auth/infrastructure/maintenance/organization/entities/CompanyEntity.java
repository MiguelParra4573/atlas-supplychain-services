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
@Table(name = "company", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // Sin @GeneratedValue: el código corto ("01".."99") se asigna en
    // CompanyRepositoryImpl#crearCompany vía SELECT nextval('auth.company_id_seq')
    // (Spec 1.17) antes del persist -- no es un IDENTITY de la tabla.
    @Id
    @Column(name = "id_company", length = 2)
    private String idCompany;

    @Column(name = "alias", length = 50, nullable = false, unique = true)
    private String alias;

    @Column(name = "business_name", length = 200, nullable = false)
    private String businessName;

    @Column(length = 13, nullable = false, unique = true)
    private String ruc;

    @Column(length = 300)
    private String address;

    @Column(name = "base_path", length = 200, nullable = false)
    private String basePath;

    @Column(name = "cedula_repres_legal", length = 10, nullable = false)
    private String cedulaRepresLegal;

    @Column(name = "legal_rep_first_name", length = 150, nullable = false)
    private String legalRepFirstName;

    @Column(name = "legal_rep_last_name", length = 150, nullable = false)
    private String legalRepLastName;

    @Column(nullable = false)
    private boolean state;
}