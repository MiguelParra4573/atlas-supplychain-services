package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {

    private Long idModule;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    private String name;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private boolean state;

    @Size(max = 100)
    private String icon;

    /** Categoría del rail (Spec 1.4), catálogo de la empresa. Opcional: un módulo sin categoría
     * no aparece en el rail hasta que alguien lo clasifique, no es un alta inválida. */
    private Long idMenuCategory;

    /** Proceso de negocio del que cuelga (Spec 1.0). Opcional: un módulo transversal no pertenece
     * a ninguno y queda siempre disponible, sin depender de ninguna activación. */
    private Long idBusinessProcess;

    /** Orden entre los módulos del mismo proceso en el menú (Spec 1.3). Opcional: {@code null}
     * en el alta se guarda como 0. */
    private Integer displayOrder;

    public interface Crear {}
    public interface Actualizar {}

}