package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Asignación de una operación a un módulo (Spec 1.2). Es el único mapeo que **escribe**
 * {@code auth.module_operation}; {@code OperationEntity.modules} y
 * {@code ModuleEntity.operationEntitySet} navegan la misma tabla en solo lectura.
 *
 * <p>Existe como entidad propia y no como {@code @JoinTable} a secas porque la tabla lleva
 * {@code id_company}: el aislamiento por tenant del resto de {@code auth} no se rompe en la
 * tabla puente.</p>
 */
@Entity
@Table(name = "module_operation", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleOperationEntity implements Serializable {

    @EmbeddedId
    private ModuleOperationId id;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    /** Override de la empresa sobre el orden por defecto de la etapa (Spec 1.3). {@code null}:
     * manda la {@code sequence} de {@code ProcessStage} de la operación, o el final si no tiene. */
    @Column(name = "display_order")
    private Integer displayOrder;

    /** Carpeta plegable de esta pantalla **dentro de este módulo** (Spec 1.4). Nullable: sin
     * carpeta, queda suelta. Columna plana -- mismo criterio que {@code Module.idBusinessProcess}. */
    @Column(name = "id_screen_group")
    private Long idScreenGroup;

    @Embeddable
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ModuleOperationId implements Serializable {

        @Column(name = "id_module", nullable = false)
        private Long idModule;

        @Column(name = "id_operation", nullable = false)
        private Long idOperation;
    }
}
