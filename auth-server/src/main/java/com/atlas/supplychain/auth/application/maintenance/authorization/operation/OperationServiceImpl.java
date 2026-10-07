package com.atlas.supplychain.auth.application.maintenance.authorization.operation;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ModuleScreenOrder;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ScreenSummary;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {

    private final OperationRepository repository;
    private final GrantedPermissionRepository grantedPermissionRepository;
    private final ModuleOperationRepository moduleOperationRepository;
    private final ModuleRepository moduleRepository;

    /**
     * La colisión se busca por {@code (idCompany, resource, tag)}, que es la identidad de una
     * autoridad (Spec 1.2). Antes se buscaba solo por {@code tag}, sin empresa: cualquier empresa
     * que ya tuviera un {@code CREAR} bloqueaba el alta de un {@code CREAR} en otra.
     */
    @Override
    public void crearOperacion(Operation operation) {
        if (repository.existsByResourceAndTag(operation.getIdCompany(), operation.getResource(), operation.getTag())) {
            throw new DuplicateRecordException(
                    "Ya existe la operación '%s:%s' en esta empresa".formatted(operation.getResource(), operation.getTag()));
        }
        repository.crearOperacion(operation);
    }

    @Override
    public void actualizarOperacion(Operation operation) throws EntityNotFoundException {
        repository.actualizarOperacion(operation);
    }

    @Override
    public void eliminarOperacion(String idCompany, Long idOperation) throws EntityNotFoundException {
        if (grantedPermissionRepository.existsByOperation(idCompany, idOperation)) {
            throw new ExistingDependenciesException(
                    "No se puede eliminar la operación ID %d: tiene permisos concedidos asociados".formatted(idOperation));
        }
        // Estar en un menú no es una dependencia que deba bloquear la baja: se sueltan las
        // asignaciones y el módulo simplemente deja de mostrarla.
        moduleOperationRepository.eliminarAsignacionesDeOperacion(idCompany, idOperation);
        repository.eliminarOperacion(idCompany, idOperation);
    }

    @Override
    public Operation obtenerOperacionPorId(String idCompany, Long idOperation) throws EntityNotFoundException {
        return repository.obtenerOperacionPorId(idCompany, idOperation);
    }

    @Override
    public List<Operation> listarOperacionesPorModulo(String idCompany, Long idModule) {
        return repository.listarOperacionesPorModulo(idCompany, idModule);
    }

    @Override
    public List<Operation> listarOperacionesSinModulo(String idCompany) {
        return repository.listarOperacionesSinModulo(idCompany);
    }

    @Override
    public PagedResult<Operation> listarOperacionesPaginadas(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarOperacionesPaginadas(idCompany, filters, pageQuery);
    }

    @Override
    public void asignarAModulo(String idCompany, Long idOperation, Long idModule) throws EntityNotFoundException {
        // Ambos lados se resuelven contra la empresa del principal antes de tocar la tabla puente:
        // sin esto se podría asignar una operación de otra empresa a un módulo propio.
        repository.obtenerOperacionPorId(idCompany, idOperation);
        moduleRepository.obtenerModuloPorId(idCompany, idModule);

        moduleOperationRepository.asignar(idCompany, idModule, idOperation);
    }

    @Override
    public void desasignarDeModulo(String idCompany, Long idOperation, Long idModule) throws EntityNotFoundException {
        repository.obtenerOperacionPorId(idCompany, idOperation);
        moduleRepository.obtenerModuloPorId(idCompany, idModule);

        moduleOperationRepository.desasignar(idCompany, idModule, idOperation);
    }

    @Override
    public List<Module> listarModulosDeOperacion(String idCompany, Long idOperation) throws EntityNotFoundException {
        repository.obtenerOperacionPorId(idCompany, idOperation);
        return moduleOperationRepository.listarModulosDeOperacion(idCompany, idOperation);
    }

    /**
     * Mueve las 5 operaciones CRUD de una pantalla juntas (Spec 1.3) -- reemplaza la asignación
     * operación-por-operación de la Spec 1.2 para el caso normal, donde las 5 siempre van al mismo
     * módulo. Idempotente por operación, vía {@code moduleOperationRepository.asignar}.
     */
    @Override
    public void asignarRecursoAModulo(String idCompany, String resource, Long idModule) throws EntityNotFoundException {
        moduleRepository.obtenerModuloPorId(idCompany, idModule);

        for (Operation operation : repository.listarPorRecurso(idCompany, resource)) {
            moduleOperationRepository.asignar(idCompany, idModule, operation.getIdOperation());
        }
    }

    @Override
    public void desasignarRecursoDeModulo(String idCompany, String resource, Long idModule) throws EntityNotFoundException {
        moduleRepository.obtenerModuloPorId(idCompany, idModule);

        for (Operation operation : repository.listarPorRecurso(idCompany, resource)) {
            moduleOperationRepository.desasignar(idCompany, idModule, operation.getIdOperation());
        }
    }

    @Override
    public void reordenarPantallasDeModulo(String idCompany, Long idModule, Map<String, Integer> displayOrderPorRecurso)
            throws EntityNotFoundException {
        moduleRepository.obtenerModuloPorId(idCompany, idModule);
        moduleOperationRepository.reordenarPantallas(idCompany, idModule, displayOrderPorRecurso);
    }

    @Override
    public void asignarCarpetaARecurso(String idCompany, String resource, Long idModule, Long idScreenGroup)
            throws EntityNotFoundException {
        moduleRepository.obtenerModuloPorId(idCompany, idModule);
        moduleOperationRepository.asignarCarpetaARecurso(idCompany, idModule, resource, idScreenGroup);
    }

    @Override
    public PagedResult<ScreenSummary> buscarPantallas(String idCompany, String resourceLike, boolean onlyUnassigned,
                                                        PageQuery pageQuery) {
        return repository.buscarPantallas(idCompany, resourceLike, onlyUnassigned, pageQuery);
    }

    /**
     * Agrupa por {@code resource} las operaciones ya asignadas al módulo y les pega el override de
     * orden que tengan -- las 5 operaciones de una pantalla comparten el mismo override porque
     * {@code reordenarPantallasDeModulo} siempre las escribe juntas, así que basta tomar cualquiera
     * no nula.
     */
    @Override
    public List<ModuleScreenOrder> pantallasDeModulo(String idCompany, Long idModule) throws EntityNotFoundException {
        moduleRepository.obtenerModuloPorId(idCompany, idModule);

        List<Operation> operaciones = repository.listarOperacionesPorModulo(idCompany, idModule);
        Map<Long, Integer> overrides = moduleOperationRepository.displayOrdersDeModulo(idCompany, idModule);
        Map<Long, Long> carpetas = moduleOperationRepository.screenGroupsDeModulo(idCompany, idModule);

        // `Map.merge` exige un valor no-nulo -- una pantalla recién asignada, sin orden/carpeta
        // todavía, tiene `override`/`idScreenGroup` `null` (caso normal, no un error) y `merge` la
        // tira con NPE. Se arman los mapas a mano para poder guardar esa ausencia en vez de
        // reventar con ella.
        Map<String, Integer> ordenPorRecurso = new java.util.LinkedHashMap<>();
        Map<String, Long> carpetaPorRecurso = new java.util.LinkedHashMap<>();
        for (Operation operacion : operaciones) {
            Integer override = overrides.get(operacion.getIdOperation());
            Long idScreenGroup = carpetas.get(operacion.getIdOperation());
            String resource = operacion.getResource();
            if (!ordenPorRecurso.containsKey(resource)) {
                ordenPorRecurso.put(resource, override);
                carpetaPorRecurso.put(resource, idScreenGroup);
            } else {
                if (ordenPorRecurso.get(resource) == null && override != null) {
                    ordenPorRecurso.put(resource, override);
                }
                if (carpetaPorRecurso.get(resource) == null && idScreenGroup != null) {
                    carpetaPorRecurso.put(resource, idScreenGroup);
                }
            }
        }

        return ordenPorRecurso.entrySet().stream()
                .sorted(java.util.Comparator
                        .<Map.Entry<String, Integer>>comparingInt(e -> e.getValue() == null ? Integer.MAX_VALUE : e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .map(e -> new ModuleScreenOrder(e.getKey(), e.getValue(), carpetaPorRecurso.get(e.getKey())))
                .toList();
    }
}
