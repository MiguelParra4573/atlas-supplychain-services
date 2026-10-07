package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una operación protegida dentro del payload de {@code POST /modules/self-register}. No
 * reutiliza {@link OperationDTO} porque ese DTO carga campos que no aplican acá ({@code
 * idOperation}, {@code idModule}, {@code state}, grupos {@code Crear}/{@code Actualizar}): el
 * módulo/operación que se registra siempre queda activo, y el vínculo con {@code id_module} lo
 * decide el servidor por empresa, nunca el cliente (Diseño §1, punto "Idempotencia").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationSelfRegistrationDTO {

    /** Vocabulario cerrado (004-normalizar-catalogo-rbac.sql): CREAR/EDITAR/ELIMINAR/CONSULTAR/LISTAR. */
    @NotBlank
    private String tag;

    @NotBlank
    private String path;

    @NotBlank
    private String httpMethod;

    private boolean permitAll;
}
