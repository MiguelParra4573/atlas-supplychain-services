package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carpeta plegable dentro de UN módulo puntual (Spec 1.4) -- ej. el módulo "Nómina" agrupa las
 * carpetas "Solicitudes" y "Aprobaciones", cada una con sus propias pantallas. A diferencia de
 * {@link MenuCategory} (catálogo global de la empresa, reutilizable entre módulos), una
 * {@code ScreenGroup} pertenece a un {@code idModule} específico y no tiene sentido fuera de él --
 * por eso no lleva {@code code} único global, solo {@code name} dentro de su módulo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenGroup {

    private Long idScreenGroup;

    private String idCompany;

    private Long idModule;

    private String name;

    /** Orden entre carpetas del mismo módulo. {@code null} se trata como {@code 0}. */
    private Integer displayOrder;

    private Boolean state;
}
