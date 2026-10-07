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

/** Carpeta plegable dentro de un módulo puntual (Spec 1.4). Columna plana {@code id_module}, sin
 * {@code @ManyToOne} -- mismo criterio que {@code ModuleEntity.idBusinessProcess}. */
@Entity
@Table(name = "screen_group", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenGroupEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_screen_group")
    private Long idScreenGroup;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @Column(name = "id_module", nullable = false)
    private Long idModule;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean state;
}
