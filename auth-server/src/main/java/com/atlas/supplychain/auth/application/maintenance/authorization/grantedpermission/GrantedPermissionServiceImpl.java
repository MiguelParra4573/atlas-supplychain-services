package com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuario;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class GrantedPermissionServiceImpl implements GrantedPermissionService {

    private static final String ENTIDAD_TIPO_PERMISO = "PERMISO";
    private static final String MODULO_ADMINISTRACION = "ADMINISTRACION";

    private final GrantedPermissionRepository repository;
    private final RoleRepository roleRepository;
    private final OperationRepository operationRepository;
    private final PermisosVigentesDelUsuario permisosVigentesDelUsuario;
    private final AuditTrail auditTrail;
    private final CurrentActor currentActor;

    @Override
    public void asignarPermisoARol(String idCompany, Long idRol, Long idOperation) throws EntityNotFoundException {
        Role role = roleRepository.obtenerRolPorId(idCompany, idRol);
        Operation operation = operationRepository.obtenerOperacionPorId(idCompany, idOperation);

        if (repository.obtenerPorRolYOperacion(idCompany, idRol, idOperation).isPresent()) {
            throw new DuplicateRecordException(
                    "El rol ID %d ya tiene concedida la operación ID %d".formatted(idRol, idOperation));
        }

        GrantedPermission permiso = GrantedPermission.builder()
                .idCompany(idCompany)
                .role(role)
                .operation(operation)
                .build();
        repository.crearPermiso(permiso);
        // GrantedPermissionRepositoryImpl#crearPermiso descarta el retorno de save(...) (idGrantedPermission
        // generado no vuelve a este objeto de dominio), y a diferencia de Role, GrantedPermission no tiene
        // un identificador natural propio: se usa la combinacion rol+operacion como mejor id disponible.
        auditar("CREATE", "ASIGNAR_PERMISO", "ROL:" + idRol + ":OPERACION:" + idOperation, "HIGH",
                "Permiso concedido: rol " + idRol + " operacion " + idOperation, idCompany);
    }

    @Override
    public void revocarPermiso(String idCompany, Long idGrantedPermission) throws EntityNotFoundException {
        repository.obtenerPermisoPorId(idCompany, idGrantedPermission);
        repository.eliminarPermiso(idCompany, idGrantedPermission);
        auditar("DELETE", "REVOCAR_PERMISO", String.valueOf(idGrantedPermission), "HIGH",
                "Permiso revocado ID " + idGrantedPermission, idCompany);
    }

    @Override
    public Set<String> obtenerPermisosEfectivos(String idUser, String idCompany) {
        return permisosVigentesDelUsuario.autoridades(idUser, idCompany);
    }

    @Override
    public GrantedPermission obtenerPermisoPorId(String idCompany, Long idGrantedPermission) throws EntityNotFoundException {
        return repository.obtenerPermisoPorId(idCompany, idGrantedPermission);
    }

    @Override
    public List<GrantedPermission> listarPermisosPorRol(String idCompany, Long idRol) {
        return repository.listarPermisosPorRol(idCompany, idRol);
    }

    @Override
    public PagedResult<GrantedPermission> listarPermisosPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarPermisosPaginados(idCompany, filters, pageQuery);
    }

    private void auditar(String accionTipo, String accionNombre, String entidadId, String severity,
                          String resumen, String fallbackCompanyId) {
        Actor actor = currentActor.actorActual();
        String companyId = actor != null ? actor.idCompany() : fallbackCompanyId;
        String userId = actor != null ? actor.idUser() : null;
        String username = actor != null ? actor.username() : null;
        String rolPrincipal = actor != null ? actor.rolPrincipal() : null;
        auditTrail.registrar(new AuditEventCommand(accionTipo, accionNombre, ENTIDAD_TIPO_PERMISO, entidadId,
                MODULO_ADMINISTRACION, severity, companyId, userId, username, rolPrincipal, resumen));
    }
}
