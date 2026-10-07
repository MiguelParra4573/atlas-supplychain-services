package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Activación de un {@link BusinessProcess} para una empresa (Spec 1.0). Es la tabla de
 * suscripción: análoga a {@code GrantedPermission}, pero a nivel de empresa en vez de rol.
 *
 * <p>Arranca con estado actual simple, sin historial de activaciones/desactivaciones: si hace falta
 * saber cuándo y por qué se dio de baja un proceso, eso ya lo cubre {@code ms-auditoria} y no se
 * reinventa acá (Spec 1.0 §Riesgos).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProcess {

    private Long idCompanyProcess;

    private String idCompany;

    private Long idBusinessProcess;

    /** Si está en false, los módulos de ese proceso dejan de verse **y de autorizar** para la
     * empresa -- ver {@code User.esVigente}. Desactivar lo contratado tiene que cortar el acceso,
     * no solo esconder el menú. */
    private Boolean active;

    private Instant activatedAt;
}
