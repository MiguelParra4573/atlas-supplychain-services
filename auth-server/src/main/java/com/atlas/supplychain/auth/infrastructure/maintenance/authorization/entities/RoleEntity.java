package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "roles", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long idRol;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @Column(length = 50, nullable = false)
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "roleEntity", cascade = CascadeType.ALL)
    private Set<GrantedPermissionEntity> grantedPermissionEntities;

    @Column(nullable = false)
    private boolean state;

}
