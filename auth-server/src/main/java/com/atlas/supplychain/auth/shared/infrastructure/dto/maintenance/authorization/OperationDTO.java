package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationDTO {

    private Long idOperation;

    /** Primera mitad de la autoridad {@code RECURSO:OPERACION} (Spec 1.2). */
    @NotBlank(groups = {Crear.class})
    private String resource;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    private String tag;

    /** Nombre legible de la pantalla (Spec 1.4). Opcional: sin uno, el frontend cae al código de
     * {@code resource}. */
    private String name;

    /** Ícono de la pantalla (Spec 1.4). Opcional. */
    private String icon;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    private String path;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    private String httpMethod;

    private boolean permitAll;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private boolean state;

    /** Etapa del proceso que cubre esta pantalla (Spec 1.3). Opcional: una pantalla transversal
     * no pertenece a ningún proceso. */
    private Long idProcessStage;

    /**
     * Módulos bajo los que esta operación aparece en el menú. Solo de salida: la asignación se
     * cambia con {@code POST/DELETE /operations/{id}/modules/{idModule}} (Spec 1.2), no mandando
     * la lista completa en un PATCH de la operación -- son dos permisos distintos y dos pantallas
     * distintas.
     */
    private List<ModuleDTO> modules;

    public interface Crear {}
    public interface Actualizar {}
}
