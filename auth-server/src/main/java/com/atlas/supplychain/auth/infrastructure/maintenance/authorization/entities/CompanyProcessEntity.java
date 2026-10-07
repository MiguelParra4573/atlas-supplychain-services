package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Qué procesos tiene activos cada empresa (Spec 1.0). Las FK se mapean como columnas planas y no
 * como {@code @ManyToOne}: esta tabla se consulta siempre por id, nunca navegando el objeto, y una
 * asociación perezosa acá solo agregaría cargas que nadie usa.
 */
@Entity
@Table(name = "company_process", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProcessEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_company_process")
    private Long idCompanyProcess;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @Column(name = "id_business_process", nullable = false)
    private Long idBusinessProcess;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;
}
