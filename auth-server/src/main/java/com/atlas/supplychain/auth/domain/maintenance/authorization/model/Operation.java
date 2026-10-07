package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Operation {

    private Long idOperation;

    private String idCompany;

    /** Código estable de la pantalla, propiedad del microservicio que la sirve (Spec 1.2). Es la
     * primera mitad de la autoridad RBAC, {@code RESOURCE:TAG} -- antes salía de
     * {@code Module.name}, que ahora es una etiqueta libre que cada empresa edita. No se modifica
     * desde el frontend de administración. */
    private String resource;

    private String tag;

    /** Nombre legible de la pantalla (Spec 1.4). Nullable: sin uno propio, el frontend cae al
     * código de {@code resource}, como ya hacía antes de este campo existir. */
    private String name;

    /** Ícono de la pantalla (Spec 1.4), mismo set cerrado que ya usa {@code Module.icon}. */
    private String icon;

    private String path;

    private String httpMethod;

    /** Etapa del proceso que cubre esta pantalla (Spec 1.3). Nullable: una pantalla transversal
     * (Usuarios, Roles, Módulos) no pertenece a ningún proceso. Es la secuencia por defecto del
     * menú si la empresa no la sobreescribió con {@code module_operation.display_order}. */
    private Long idProcessStage;

    private boolean permitAll;

    private boolean state;

    /** Módulos bajo los que esta operación aparece en el menú de la empresa (Spec 1.2, N:M). Puede
     * ser vacío: una operación recién auto-registrada no cuelga de ninguno hasta que un
     * administrador la asigne, y eso no le quita autoridad. Si en cambio cuelga de módulos y todos
     * están desactivados, la operación deja de autorizar -- ver {@code User.esVigente}. Fuera de
     * {@code equals}/{@code hashCode}/{@code toString} para no entrar en ciclo con
     * {@link Module#getOperationSet()}. */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Module> modules;

}
