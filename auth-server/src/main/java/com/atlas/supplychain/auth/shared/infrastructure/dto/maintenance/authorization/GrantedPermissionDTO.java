package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantedPermissionDTO {

    private Long idGrantedPermission;

    @NotNull(groups = {Crear.class})
    private Long idRole;

    private String roleName;

    @NotNull(groups = {Crear.class})
    private Long idOperation;

    private String operationTag;

    /** Primera mitad de la autoridad, {@code RECURSO:OPERACION} (Spec 1.2) -- reemplaza al viejo
     * {@code moduleName}, que ya no identifica nada: una operación puede estar en varios módulos a
     * la vez, o en ninguno. */
    private String resource;

    public interface Crear {
    }

    public interface Actualizar {
    }
}