package com.atlas.supplychain.auth.application.maintenance.authorization.role;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.RoleMenuSummaryView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final String ENTIDAD_TIPO_ROL = "ROL";
    private static final String MODULO_ADMINISTRACION = "ADMINISTRACION";
    private static final String AGGREGATE_TYPE_ROLE = "ROLE";

    private final RoleRepository repository;
    private final GrantedPermissionRepository grantedPermissionRepository;
    private final UserRepository userRepository;
    private final AuditTrail auditTrail;
    private final CurrentActor currentActor;
    private final OutboxRepository outboxRepository;

    @Override
    public void crearRol(Role role) {
        if (repository.existsByNameAndCompany(role.getIdCompany(), role.getName())) {
            throw new DuplicateRecordException(
                    "Ya existe un rol con nombre '%s' en la empresa %s".formatted(role.getName(), role.getIdCompany()));
        }
        repository.crearRol(role);
        // El nombre del rol (unico por empresa, ver validacion de arriba) es el identificador de
        // negocio para el audit log; RoleRepositoryImpl#crearRol propaga el idRol autogenerado de
        // vuelta a `role` (Spec 1.24) para que registrarRolCreado(role) tenga un id real.
        auditar("CREATE", "CREAR_ROL", role.getName(), "MEDIUM", "Rol creado: " + role.getName(), role.getIdCompany());
        registrarRolCreado(role);
    }

    @Override
    public void actualizarRol(Role role) throws EntityNotFoundException {
        repository.obtenerRolPorId(role.getIdCompany(), role.getIdRol());
        repository.actualizarRol(role);
        auditar("UPDATE", "ACTUALIZAR_ROL", String.valueOf(role.getIdRol()), "MEDIUM",
                "Rol actualizado: " + role.getName(), role.getIdCompany());
        registrarRolActualizado(role);
    }

    @Override
    public void eliminarRol(String idCompany, Long idRol) throws EntityNotFoundException {
        if (!grantedPermissionRepository.listarPermisosPorRol(idCompany, idRol).isEmpty()) {
            throw new ExistingDependenciesException(
                    "No se puede eliminar el rol ID %d: tiene permisos concedidos asociados".formatted(idRol));
        }
        if (userRepository.existsByRole(idCompany, idRol)) {
            throw new ExistingDependenciesException(
                    "No se puede eliminar el rol ID %d: tiene usuarios asignados".formatted(idRol));
        }
        repository.eliminarRol(idCompany, idRol);
        auditar("DELETE", "ELIMINAR_ROL", String.valueOf(idRol), "HIGH", "Rol eliminado ID " + idRol, idCompany);
        registrarRolEliminado(idCompany, idRol);
    }

    /**
     * Spec 1.24: publica {@code ROL_CREADO} vía Outbox en la misma transacción que el
     * {@code INSERT} de {@link #crearRol(Role)} -- alimenta el read model CQRS de
     * {@code ms-reporte} (Spec 4.4 revisada).
     */
    private void registrarRolCreado(Role role) {
        outboxRepository.append(new OutboxEventCommand(
                AGGREGATE_TYPE_ROLE,
                String.valueOf(role.getIdRol()),
                "ROL_CREADO",
                snapshotRol(role)));
    }

    /**
     * Spec 1.24: publica {@code ROL_ACTUALIZADO} vía Outbox con el snapshot completo post-update,
     * en la misma transacción que el {@code UPDATE} de {@link #actualizarRol(Role)}.
     */
    private void registrarRolActualizado(Role role) {
        outboxRepository.append(new OutboxEventCommand(
                AGGREGATE_TYPE_ROLE,
                String.valueOf(role.getIdRol()),
                "ROL_ACTUALIZADO",
                snapshotRol(role)));
    }

    /**
     * Spec 1.24: {@code RoleRepositoryImpl.eliminarRol} hace baja física ({@code this.delete}),
     * al igual que {@code UserRepositoryImpl.eliminarUsuario} -- se publica {@code ROL_ELIMINADO}
     * con el mismo criterio que {@code USUARIO_ELIMINADO} (la spec no lo daba por sentado: pedía
     * verificar si la baja era física o lógica antes de decidir).
     */
    private void registrarRolEliminado(String idCompany, Long idRol) {
        outboxRepository.append(new OutboxEventCommand(
                AGGREGATE_TYPE_ROLE,
                String.valueOf(idRol),
                "ROL_ELIMINADO",
                Map.of("idRole", String.valueOf(idRol), "idCompany", idCompany)));
    }

    // Map mutable (no Map.of) porque idCompany/name podrian venir null en construcciones
    // parciales del dominio -- Map.of lanza NullPointerException ante cualquier valor null
    // (mismo criterio que UserServiceImpl#snapshotUsuario, Spec 1.24).
    private static Map<String, String> snapshotRol(Role role) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("idRole", String.valueOf(role.getIdRol()));
        snapshot.put("idCompany", role.getIdCompany());
        snapshot.put("name", role.getName());
        snapshot.put("state", String.valueOf(role.isState()));
        return snapshot;
    }

    @Override
    public Role obtenerRolPorId(String idCompany, Long idRol) throws EntityNotFoundException {
        return repository.obtenerRolPorId(idCompany, idRol);
    }

    @Override
    public List<Role> listarRolesPorCompany(String idCompany) {
        return repository.listarRolesPorCompany(idCompany);
    }

    @Override
    public PagedResult<Role> listarRolesPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarRolesPaginados(idCompany, filters, pageQuery);
    }

    @Override
    public List<RoleMenuSummaryView> resumenDeMenuPorRol(String idCompany) {
        return repository.resumenDeMenuPorRol(idCompany);
    }

    private void auditar(String accionTipo, String accionNombre, String entidadId, String severity,
                          String resumen, String fallbackCompanyId) {
        Actor actor = currentActor.actorActual();
        String companyId = actor != null ? actor.idCompany() : fallbackCompanyId;
        String userId = actor != null ? actor.idUser() : null;
        String username = actor != null ? actor.username() : null;
        String rolPrincipal = actor != null ? actor.rolPrincipal() : null;
        auditTrail.registrar(new AuditEventCommand(accionTipo, accionNombre, ENTIDAD_TIPO_ROL, entidadId,
                MODULO_ADMINISTRACION, severity, companyId, userId, username, rolPrincipal, resumen));
    }
}
