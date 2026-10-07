package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Proceso de negocio del catálogo maestro (Spec 1.0). Lo administra el proveedor del ERP. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProcessDTO {

    private Long idBusinessProcess;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 50, groups = {Crear.class, Actualizar.class})
    private String code;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150, groups = {Crear.class, Actualizar.class})
    private String name;

    @Size(max = 500, groups = {Crear.class, Actualizar.class})
    private String description;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Boolean state;

    /** Solo en la respuesta de "procesos de mi empresa": si está activo para esa empresa. */
    private Boolean active;

    public interface Crear {}
    public interface Actualizar {}
}
