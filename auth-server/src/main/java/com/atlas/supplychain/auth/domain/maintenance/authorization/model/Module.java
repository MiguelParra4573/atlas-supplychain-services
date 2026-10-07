package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

/**
 * Etiqueta con la que una empresa agrupa sus pantallas en el menú (Spec 1.2). Es configuración
 * libre por empresa -- "Mantenimiento de Clientes" puede colgar de Ventas en una empresa y de
 * Administración en otra -- y **no participa de la cadena de seguridad**: la autoridad RBAC sale de
 * {@code Operation.resource} + {@code Operation.tag}, no de este {@code name}. Renombrar un módulo
 * mueve pantallas de lugar en el menú, nunca cambia permisos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Module {

    private Long idModule;

    private String idCompany;

    private String name;

    private Boolean state;

    private String icon;

    /** Categoría del rail de navegación (Spec 1.4), catálogo de la empresa (`MenuCategory`).
     * Nullable a propósito -- mismo criterio que {@code idBusinessProcess}: un módulo sin
     * categoría no se pierde, solo no aparece en el rail hasta que alguien lo clasifique.
     * Reemplaza el viejo {@code category} (`chk_modules_category`, changelog 024, enum cerrado de
     * 3 valores igual para todas las empresas). */
    private Long idMenuCategory;

    /** Proceso de negocio del que cuelga este módulo (Spec 1.0). **Nullable a propósito**: un
     * módulo transversal (RBAC, Organización, Geografía) no pertenece a ningún proceso y está
     * siempre disponible, sin depender de ninguna activación. */
    private Long idBusinessProcess;

    /** Orden entre los módulos del mismo proceso en el menú (Spec 1.3). A igualdad, se desempata
     * por {@code name}. {@code null} se trata como {@code 0} -- ver {@code MenuServiceImpl}. */
    private Integer displayOrder;

    /**
     * Derivado, no almacenado: si el proceso de este módulo está activo para la empresa del usuario
     * que se cargó. Lo resuelve el repositorio al armar el agregado (join con
     * {@code company_process}); vale {@code true} cuando el módulo no tiene proceso.
     *
     * <p>Existe como campo en vez de resolverse con una consulta porque {@code User.esVigente} se
     * evalúa en cada petición autenticada y no puede disparar I/O. Y no se resuelve descartando el
     * módulo en la carga: una operación cuyos módulos quedaran todos fuera pasaría a verse "sin
     * módulo", y una operación sin módulo **sí** autoriza -- desactivar un proceso terminaría
     * dando acceso en vez de quitarlo.</p>
     */
    @Builder.Default
    private Boolean processActive = Boolean.TRUE;

    /** Operaciones asignadas a este módulo (Spec 1.2, N:M). Fuera de {@code equals}/{@code
     * hashCode}/{@code toString} para no entrar en ciclo con {@link Operation#getModules()} -- y
     * porque este agregado se usa como clave del mapa del menú. */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Operation> operationSet;
}
