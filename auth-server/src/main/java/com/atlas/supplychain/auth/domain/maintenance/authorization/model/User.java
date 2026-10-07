package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import com.atlas.supplychain.auth.domain.common.security.AuthorityName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String idUser;

    private String idCompany;

    private String username;

    private String name;

    private String lastName;

    private String email;

    private String passwordHash;

    private Instant passwordChangedAt;

    private int failedAttempts;

    private Instant lockUntil;

    private Instant passwordExpiresAt;

    private boolean state;

    private Role role;

    /**
     * Autoridades efectivas de este usuario, en formato RECURSO:OPERACION. Sin I/O: opera sobre el
     * agregado ya cargado -- el filtro JWT llama esto en cada petición autenticada, no puede
     * disparar una segunda consulta.
     *
     * <p>Desde la Spec 1.2 el <em>nombre</em> de la autoridad ya no sale de {@link Module} sino de
     * la propia {@link Operation} ({@code resource}+{@code tag}), así que renombrar o mover un
     * módulo nunca cambia un permiso. El <em>estado</em> del módulo sí sigue cortando acceso: ver
     * {@link #esVigente(GrantedPermission)}.</p>
     */
    public Set<String> permisosEfectivos() {
        return permisosVigentes()
                .map(p -> AuthorityName.of(p.getOperation()))
                .collect(Collectors.toSet());
    }

    /**
     * Permisos vigentes de este usuario agrupados por {@link Module}, para el árbol de menú
     * (Spec 1.18). Sin I/O, igual que {@link #permisosEfectivos()}.
     *
     * <p>Con la relación N:M de la Spec 1.2 un mismo permiso aparece bajo **cada** módulo activo
     * al que esté asignada su operación -- es el comportamiento buscado (la misma pantalla puede
     * vivir bajo dos agrupaciones de la misma empresa), no una duplicación a corregir.</p>
     *
     * <p>Una operación sin módulos no aparece en ningún lado del menú, aunque
     * {@link #permisosEfectivos()} sí la autorice: está concedida pero todavía sin agrupar, y el
     * menú no tiene dónde colgarla. Se ve en la pantalla de asignación de operaciones.</p>
     */
    public Map<Module, List<GrantedPermission>> permisosVigentesPorModulo() {
        return permisosVigentes()
                .flatMap(permiso -> modulosActivosDe(permiso.getOperation())
                        .map(module -> Map.entry(module, permiso)))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /** Recorrido común de "permiso vigente" -- un solo lugar donde vive el criterio (Spec 1.18). */
    private Stream<GrantedPermission> permisosVigentes() {
        if (role == null || role.getGrantedPermissions() == null) {
            return Stream.empty();
        }
        return role.getGrantedPermissions().stream().filter(User::esVigente);
    }

    /**
     * Un permiso vigente exige las tres cosas: operación existente, operación activa y agrupación
     * vigente. Esconder un módulo del menú tiene que cortar el acceso de verdad, no solo dejar de
     * pintarlo -- si no, el endpoint sigue respondiendo a quien lo llame por fuera del menú
     * (Postman, un enlace guardado, otra pantalla).
     */
    private static boolean esVigente(GrantedPermission permiso) {
        Operation operation = permiso.getOperation();
        return operation != null && operation.isState() && tieneAgrupacionVigente(operation);
    }

    /**
     * Una operación asignada a módulos necesita al menos uno activo; basta uno, porque la misma
     * pantalla puede vivir bajo dos agrupaciones y apagar una no debería apagar la otra.
     *
     * <p>Una operación sin ningún módulo pasa. Es el estado normal de una recién auto-registrada
     * por su microservicio, antes de que un administrador la agrupe: bloquearla dejaría cada
     * {@code ms-*} nuevo arrancando con todas sus operaciones muertas y sin forma de asignarlas.
     * Para quitarle acceso a una operación sin agrupar se revoca el permiso o se desactiva la
     * operación.</p>
     */
    private static boolean tieneAgrupacionVigente(Operation operation) {
        Set<Module> modules = operation.getModules();
        if (modules == null || modules.isEmpty()) {
            return true;
        }
        return modulosActivosDe(operation).findAny().isPresent();
    }

    /**
     * Un módulo cuenta como vigente si está activo **y** su proceso de negocio está activo para la
     * empresa (Spec 1.0). Desactivar un proceso contratado corta el acceso a sus pantallas por el
     * mismo motivo que lo hace desactivar el módulo: si solo dejara de pintarse, el endpoint
     * seguiría respondiendo a quien lo llamara por fuera del menú.
     *
     * <p>{@code processActive} viene resuelto desde el repositorio y vale {@code true} para un
     * módulo transversal, que no cuelga de ningún proceso.</p>
     */
    private static Stream<Module> modulosActivosDe(Operation operation) {
        Set<Module> modules = operation.getModules();
        if (modules == null) {
            return Stream.empty();
        }
        return modules.stream()
                .filter(module -> Boolean.TRUE.equals(module.getState()))
                .filter(module -> !Boolean.FALSE.equals(module.getProcessActive()));
    }
}
