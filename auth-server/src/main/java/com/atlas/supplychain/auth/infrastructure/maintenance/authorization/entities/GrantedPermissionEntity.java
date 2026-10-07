package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "granted_permissions", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantedPermissionEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_granted_permission")
    private Long idGrantedPermission;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", nullable = false)
    private RoleEntity roleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operation", nullable = false)
    private OperationEntity operationEntity;

}