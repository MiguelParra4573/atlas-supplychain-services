package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Botón del rail de navegación (Spec 1.4). Catálogo dueño de la **empresa** -- a diferencia de
 * {@link BusinessProcess} (catálogo del proveedor, atado a {@code CompanyProcess}/suscripción),
 * una {@code MenuCategory} no tiene relación con facturación: es pura organización del menú, cada
 * empresa arma las suyas. Reemplaza al viejo {@code Module.category} (`chk_modules_category`,
 * changelog 024), un enum cerrado de 3 valores igual para todas las empresas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategory {

    private Long idMenuCategory;

    private String idCompany;

    /** Código estable, único por empresa (no global -- dos empresas pueden tener las dos un
     * "MANTENIMIENTO" sin colisión). */
    private String code;

    private String name;

    private String icon;

    /** Orden entre categorías del rail. {@code null} se trata como {@code 0}. */
    private Integer displayOrder;

    private Boolean state;
}
