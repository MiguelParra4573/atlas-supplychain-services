package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Botón del rail de navegación (Spec 1.4). Catálogo de la empresa. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryDTO {

    private Long idMenuCategory;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 50, groups = {Crear.class, Actualizar.class})
    private String code;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150, groups = {Crear.class, Actualizar.class})
    private String name;

    @Size(max = 100)
    private String icon;

    /** Opcional: {@code null} en el alta se guarda como 0. */
    private Integer displayOrder;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Boolean state;

    public interface Crear {}
    public interface Actualizar {}
}
