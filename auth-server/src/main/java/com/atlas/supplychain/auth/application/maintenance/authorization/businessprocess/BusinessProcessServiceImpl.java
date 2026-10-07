package com.atlas.supplychain.auth.application.maintenance.authorization.businessprocess;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BusinessProcessServiceImpl implements BusinessProcessService {

    private static final String ENTIDAD_TIPO_PROCESO = "PROCESO_NEGOCIO";
    private static final String MODULO_ADMINISTRACION = "ADMINISTRACION";

    private final BusinessProcessRepository repository;
    private final AuditTrail auditTrail;
    private final CurrentActor currentActor;

    @Override
    public void crear(BusinessProcess process) throws DuplicateRecordException {
        // `code` es la referencia estable del proceso y tiene UNIQUE en la base: se valida acá para
        // devolver 409 con mensaje propio en vez de dejar escapar la violación de constraint.
        if (repository.existsByCode(process.getCode())) {
            throw new DuplicateRecordException("Ya existe un proceso de negocio con el código " + process.getCode());
        }
        repository.crear(process);
    }

    @Override
    public void actualizar(BusinessProcess process) throws EntityNotFoundException {
        // Mismo chequeo que en `crear`, excluyendo el propio registro: sin esto, cambiarle el
        // `code` al de otro proceso viola el UNIQUE y sale como 409 genérico ("existen datos
        // relacionados"), que describe un caso distinto.
        BusinessProcess existente = repository.obtenerPorId(process.getIdBusinessProcess());
        if (!existente.getCode().equals(process.getCode()) && repository.existsByCode(process.getCode())) {
            throw new DuplicateRecordException("Ya existe un proceso de negocio con el código " + process.getCode());
        }
        repository.actualizar(process);
    }

    @Override
    public BusinessProcess obtenerPorId(Long idBusinessProcess) throws EntityNotFoundException {
        return repository.obtenerPorId(idBusinessProcess);
    }

    @Override
    public void eliminar(Long idBusinessProcess) throws EntityNotFoundException {
        repository.eliminar(idBusinessProcess);
    }

    @Override
    public List<BusinessProcess> listar() {
        return repository.listar();
    }

    @Override
    public List<CompanyProcess> listarPorEmpresa(String idCompany) {
        return repository.listarPorEmpresa(idCompany);
    }

    /**
     * Es la acción con consecuencia comercial de toda la spec -- desactivar corta el acceso a las
     * pantallas del proceso para todos los usuarios de la empresa -- así que queda registrada. La
     * auditoría en este repo es explícita, no automática: mismo patrón que
     * {@code GrantedPermissionServiceImpl}.
     */
    @Override
    public void activar(String idCompany, Long idBusinessProcess, boolean active) throws EntityNotFoundException {
        // La existencia del proceso la valida `repository.activar` dentro de su propia transacción:
        // acá solo se lee para el resumen de auditoría. Esta capa no puede declarar transacciones
        // (`ImportControl` prohíbe Spring en `application/`), así que la atomicidad del upsert vive
        // en el repositorio.
        BusinessProcess proceso = repository.obtenerPorId(idBusinessProcess);
        repository.activar(idCompany, idBusinessProcess, active);

        auditar(active ? "ACTIVAR" : "DESACTIVAR",
                active ? "PROCESO_ACTIVADO" : "PROCESO_DESACTIVADO",
                String.valueOf(idBusinessProcess),
                active ? "INFO" : "WARN",
                (active ? "Se activó" : "Se desactivó") + " el proceso " + proceso.getCode()
                        + " para la empresa " + idCompany,
                idCompany);
    }

    private void auditar(String accionTipo, String accionNombre, String entidadId, String severity,
                          String resumen, String fallbackCompanyId) {
        Actor actor = currentActor.actorActual();
        String companyId = actor != null ? actor.idCompany() : fallbackCompanyId;
        String userId = actor != null ? actor.idUser() : null;
        String username = actor != null ? actor.username() : null;
        String rolPrincipal = actor != null ? actor.rolPrincipal() : null;
        auditTrail.registrar(new AuditEventCommand(accionTipo, accionNombre, ENTIDAD_TIPO_PROCESO, entidadId,
                MODULO_ADMINISTRACION, severity, companyId, userId, username, rolPrincipal, resumen));
    }
}
