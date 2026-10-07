package com.atlas.supplychain.auth.application.maintenance.authorization.module;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository repository;
    private final ModuleOperationRepository moduleOperationRepository;

    @Override
    public void crearModulo(Module module) {
        // `display_order` es NOT NULL en la base (Spec 1.3): un alta sin orden explícito va al
        // principio de su proceso, no rechazada.
        if (module.getDisplayOrder() == null) {
            module.setDisplayOrder(0);
        }
        repository.crearModulo(module);
    }

    @Override
    public void actualizarModulo(Module module) throws EntityNotFoundException {
        repository.actualizarModulo(module);
    }

    /**
     * Borrar un módulo es borrar una etiqueta del menú (Spec 1.2). Ya no se bloquea por tener
     * operaciones asignadas -- antes esa dependencia era real (la operación colgaba del módulo con
     * una FK y se habría quedado huérfana), ahora es solo agrupación: se sueltan las asignaciones y
     * las operaciones quedan sin módulo, exactamente igual que una recién auto-registrada. Siguen
     * autorizando sus endpoints; solo dejan de tener dónde pintarse hasta que alguien las reasigne.
     */
    @Override
    public void eliminarModulo(String idCompany, Long idModule) throws EntityNotFoundException {
        repository.obtenerModuloPorId(idCompany, idModule);
        moduleOperationRepository.eliminarAsignacionesDeModulo(idCompany, idModule);
        repository.eliminarModulo(idCompany, idModule);
    }

    @Override
    public Module obtenerModuloPorId(String idCompany, Long idModule) throws EntityNotFoundException {
        return repository.obtenerModuloPorId(idCompany, idModule);
    }

    @Override
    public List<Module> listarModulosPorState(String idCompany, Boolean state) {
        return repository.listarModulosPorState(idCompany, state);
    }

    @Override
    public PagedResult<Module> listarModulosPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarModulosPaginados(idCompany, filters, pageQuery);
    }
}
