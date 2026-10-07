package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de solo lectura (Spec 1.18) -- nunca es body de request, por eso no lleva grupos
 * {@code Crear}/{@code Actualizar}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuModuloDTO {

    private Long idModule;

    private String name;

    private String icon;

    /** Proceso del que cuelga (Spec 1.3) -- ordena el menú, eje distinto de la categoría. {@code
     * null} si el módulo es transversal. */
    private Long idBusinessProcess;

    private String businessProcessName;

    /** Categoría del rail (Spec 1.4), lo que el shell usa para agrupar los botones. {@code null}:
     * sin clasificar, no aparece en el rail. Reemplaza el viejo {@code category} (enum fijo). */
    private Long idMenuCategory;

    private String menuCategoryName;

    private List<MenuOperacionDTO> operations;
}
