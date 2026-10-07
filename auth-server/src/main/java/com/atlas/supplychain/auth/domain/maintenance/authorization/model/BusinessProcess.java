package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proceso de negocio del catálogo maestro (Spec 1.0): lo que una empresa contrata -- Ventas,
 * Taller, Flota.
 *
 * <p>A diferencia de {@link Module}, que cada empresa arma a gusto, esto lo define **el proveedor
 * del ERP** y es global: no lleva {@code idCompany}. Qué empresa tiene cuál activo vive en
 * {@link CompanyProcess}. Esa separación es el punto de la spec: activar "Taller" para un cliente
 * nuevo tiene que ser alta de dato, no un {@code if (idCompany == X)} disperso por el código.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProcess {

    private Long idBusinessProcess;

    /** Código estable con el que el resto del sistema lo referencia (ej. {@code VENTAS}). */
    private String code;

    private String name;

    private String description;

    /** Baja lógica del catálogo. Un proceso inactivo acá no lo ve ninguna empresa, tenga o no
     * su {@link CompanyProcess} activo. */
    private Boolean state;
}
