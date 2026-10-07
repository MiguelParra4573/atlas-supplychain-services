package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Carpeta plegable dentro de un módulo (Spec 1.4). {@code idModule} no viaja en el body -- sale
 * del path del endpoint, mismo criterio que {@code idCompany} sale del principal. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenGroupDTO {

    private Long idScreenGroup;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150, groups = {Crear.class, Actualizar.class})
    private String name;

    /** Opcional: {@code null} en el alta se guarda como 0. */
    private Integer displayOrder;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Boolean state;

    public interface Crear {}
    public interface Actualizar {}
}
