package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.RoleMenuSummaryView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface RoleRepository {

    void crearRol(Role role);

    void actualizarRol(Role role) throws EntityNotFoundException;

    Role obtenerRolPorId(String idCompany, Long idRol) throws EntityNotFoundException;

    void eliminarRol(String idCompany, Long idRol) throws EntityNotFoundException;

    List<Role> listarRolesPorCompany(String idCompany);

    PagedResult<Role> listarRolesPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    boolean existsByNameAndCompany(String idCompany, String name);

    /**
     * Resumen del menú visible de **todos** los roles de la empresa, en una sola consulta (Spec
     * 1.2). Deliberadamente no hay variante por rol: existe para pintar una lista de roles, y
     * pedirlo de a uno sería una llamada por fila.
     */
    List<RoleMenuSummaryView> resumenDeMenuPorRol(String idCompany);
}
