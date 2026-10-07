package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de solo lectura (Spec 1.18) -- nunca es body de request, por eso no lleva grupos
 * {@code Crear}/{@code Actualizar}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuOperacionDTO {

    private Long idOperation;

    /** Código estable de la pantalla (Spec 1.2). El frontend resuelve con esto a qué ruta navega
     * cada ítem del menú -- antes lo deducía del nombre del módulo, que dejó de identificar una
     * pantalla cuando un módulo pasó a agrupar varias. */
    private String resource;

    private String tag;

    /** Nombre legible de la pantalla (Spec 1.4). {@code null}: el frontend cae al código de
     * {@code resource}. */
    private String name;

    private String icon;

    /** Carpeta plegable de esta pantalla dentro del módulo (Spec 1.4). {@code null}: sin carpeta,
     * queda suelta bajo el módulo. */
    private Long idScreenGroup;

    private String screenGroupName;

    private String path;

    private String httpMethod;
}
