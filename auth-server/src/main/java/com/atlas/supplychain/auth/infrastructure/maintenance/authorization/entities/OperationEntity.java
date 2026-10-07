package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "operations", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operation")
    private Long idOperation;

    @Column(length = 150, nullable = false)
    private String resource;

    @Column(length = 100, nullable = false)
    private String tag;

    /** Nombre legible de la pantalla (Spec 1.4). Nullable: sin uno propio, el frontend cae al
     * código de {@code resource}. */
    @Column(length = 150)
    private String name;

    /** Ícono de la pantalla (Spec 1.4). */
    @Column(length = 100)
    private String icon;

    @Column(length = 300, nullable = false)
    private String path;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod;

    /** Etapa del proceso que cubre esta pantalla (Spec 1.3). Nullable: transversal, sin proceso. */
    @Column(name = "id_process_stage")
    private Long idProcessStage;

    @Column(name = "permit_all", nullable = false)
    private boolean permitAll; // 'S' o 'N'

    @Column(nullable = false)
    private boolean state;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    /**
     * Navegación de solo lectura sobre {@code auth.module_operation} (Spec 1.2). Las altas y
     * bajas de la asignación van por {@link ModuleOperationEntity}, que es el único mapeo que
     * escribe esa tabla -- de ahí el {@code insertable=false, updatable=false}: dos mapeos
     * escribiendo la misma tabla es la forma clásica de duplicar filas sin darse cuenta.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "module_operation",
            schema = "auth",
            joinColumns = @JoinColumn(name = "id_operation", insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "id_module", insertable = false, updatable = false))
    private Set<ModuleEntity> modules;

}
