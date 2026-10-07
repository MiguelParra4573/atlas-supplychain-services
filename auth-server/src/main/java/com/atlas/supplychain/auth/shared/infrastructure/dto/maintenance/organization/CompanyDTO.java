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
public class CompanyDTO {

    // Ausente/ignorado en Crear -- lo genera el backend (secuencia auth.company_id_seq,
    // Spec 1.17). Inmutable una vez creada: en Actualizar identifica cuál empresa se edita.
    @NotBlank(groups = {Actualizar.class})
    @Size(max = 2)
    private String idCompany;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 50)
    private String alias;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 200)
    private String businessName;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 13)
    private String ruc;

    @Size(max = 300)
    private String address;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 200)
    private String basePath;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 10)
    private String cedulaRepresLegal;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150)
    private String legalRepFirstName;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 150)
    private String legalRepLastName;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private boolean state;

    public interface Crear {}
    public interface Actualizar {}
}
