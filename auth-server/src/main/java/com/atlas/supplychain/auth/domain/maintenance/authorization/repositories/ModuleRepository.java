package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;


import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;
import java.util.Optional;

public interface ModuleRepository {

    void crearModulo(Module module);

    void actualizarModulo(Module module) throws EntityNotFoundException;

    Module obtenerModuloPorId(String idCompany, Long idModule) throws EntityNotFoundException;

    void eliminarModulo(String idCompany, Long idModule) throws EntityNotFoundException;

    List<Module> listarModulosPorState(String idCompany, Boolean state);

    PagedResult<Module> listarModulosPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    /**
     * Auto-registro de módulo (POST /modules/self-register): único punto de entrada para
     * detectar si el módulo ya existe para esta empresa (mismo par name+idCompany que protege
     * {@code uk_modules_name_id_company}, changelog 017) antes de decidir crear vs. hacer
     * upsert de sus operaciones.
     */
    Optional<Module> buscarPorNombre(String idCompany, String name);

}
