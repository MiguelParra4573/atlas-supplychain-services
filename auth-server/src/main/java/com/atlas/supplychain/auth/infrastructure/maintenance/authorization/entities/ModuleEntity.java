package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "modules", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_module")
    private Long idModule;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean state;

    @Column(length = 100)
    private String icon;

    /** Categoría del rail (Spec 1.4), catálogo de la empresa. Nullable, columna plana -- mismo
     * criterio que {@code idBusinessProcess}. Reemplaza el viejo {@code category} (changelog 024). */
    @Column(name = "id_menu_category")
    private Long idMenuCategory;

    /** Proceso de negocio del que cuelga (Spec 1.0). Nullable: un módulo transversal no pertenece
     * a ninguno y está siempre disponible. Columna plana, no {@code @ManyToOne}: el menú resuelve
     * la activación con un join explícito contra {@code company_process}, no navegando el objeto. */
    @Column(name = "id_business_process")
    private Long idBusinessProcess;

    /** Orden entre los módulos del mismo proceso en el menú (Spec 1.3). */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    /**
     * Lado inverso de la N:M (Spec 1.2), solo lectura. Ya no cascadea: borrar un módulo es borrar
     * una etiqueta del menú, nunca las operaciones que tenía agrupadas -- esas son catálogo RBAC
     * de un microservicio y sobreviven al módulo. La baja de las filas de asignación la hace
     * {@code ModuleRepositoryImpl#eliminarModulo} explícitamente.
     */
    @ManyToMany(mappedBy = "modules", fetch = FetchType.LAZY)
    private Set<OperationEntity> operationEntitySet;
}
