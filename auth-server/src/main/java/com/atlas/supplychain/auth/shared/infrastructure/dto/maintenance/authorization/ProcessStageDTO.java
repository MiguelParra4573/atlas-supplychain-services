package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Etapa de un proceso de negocio (Spec 1.3). Lo administra el proveedor del ERP. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStageDTO {

    private Long idProcessStage;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Long idBusinessProcess;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 50, groups = {Crear.class, Actualizar.class})
    private String code;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150, groups = {Crear.class, Actualizar.class})
    private String name;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Integer sequence;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Boolean state;

    public interface Crear {}
    public interface Actualizar {}
}
