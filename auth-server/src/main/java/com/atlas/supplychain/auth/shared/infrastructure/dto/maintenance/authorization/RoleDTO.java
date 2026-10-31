package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    private Long idRole;

    private String idCompany;

    @NotBlank(groups = {Crear.class, Actualizar.class}) @Size(max = 50) private String name;

    // sin @NotNull: en un primitivo "boolean" la validacion es siempre trivialmente
    // cierta -- nunca puede ser null. Error Prone (NullablePrimitive) lo marca.
    private boolean state;

    public interface Crear {}

    public interface Actualizar {}
}
