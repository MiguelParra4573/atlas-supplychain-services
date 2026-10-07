package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization;

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
public class AgencyDTO {

    // Ausente/ignorado en Crear -- lo genera el backend (UUID). Actualizar lo exige para
    // identificar cuál agencia se edita.
    @NotBlank(groups = {Actualizar.class})
    @Size(max = 36)
    private String idAgency;

    // Solo lectura: se completa en las respuestas, nunca se confía del cliente en la
    // entrada -- el idCompany real sale del principal autenticado (mismo criterio que Role).
    private String idCompany;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150)
    private String name;

    @Size(max = 300)
    private String address;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private boolean state;

    public interface Crear {}
    public interface Actualizar {}
}
