package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una pantalla asignada a un módulo con su override de orden actual (Spec 1.3), para la pantalla
 * de reordenamiento en {@code client/}. Solo lectura -- el cambio de orden se manda con
 * {@code PUT /operations/modules/{moduleId}/order}, no editando este DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleScreenDTO {

    private String resource;

    /** {@code null}: todavía sin override, no vale 0. */
    private Integer displayOrder;

    /** Carpeta actual de esta pantalla dentro de este módulo (Spec 1.4). {@code null}: sin
     * carpeta, la pantalla cuelga suelta del módulo. */
    private Long idScreenGroup;
}
