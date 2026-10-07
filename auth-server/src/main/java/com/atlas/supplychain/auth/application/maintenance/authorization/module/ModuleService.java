package com.atlas.supplychain.auth.application.maintenance.authorization.module;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface ModuleService {

    void crearModulo(Module module);

    void actualizarModulo(Module module) throws EntityNotFoundException;

    void eliminarModulo(String idCompany, Long idModule) throws EntityNotFoundException;

    Module obtenerModuloPorId(String idCompany, Long idModule) throws EntityNotFoundException;

    List<Module> listarModulosPorState(String idCompany, Boolean state);

    PagedResult<Module> listarModulosPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);
}
