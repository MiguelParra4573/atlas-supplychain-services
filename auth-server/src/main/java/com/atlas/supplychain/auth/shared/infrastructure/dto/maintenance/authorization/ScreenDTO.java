package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fila del buscador de pantallas (Spec 1.3): solo lectura, nunca body de request -- la asignación
 * se cambia con los endpoints dedicados de {@code OperationController}, no editando este DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenDTO {

    private String resource;

    /** Nombre legible de la pantalla (Spec 1.4). {@code null}: el frontend cae al código de
     * {@code resource}. */
    private String name;

    /** Ícono de la pantalla (Spec 1.4). */
    private String icon;

    private Long idProcessStage;

    private List<ModuleDTO> modules;
}
