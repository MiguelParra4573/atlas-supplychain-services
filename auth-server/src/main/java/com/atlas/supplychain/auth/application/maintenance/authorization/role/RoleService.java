package com.atlas.supplychain.auth.application.maintenance.authorization.role;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.RoleMenuSummaryView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface RoleService {

    void crearRol(Role role);

    void actualizarRol(Role role) throws EntityNotFoundException;

    void eliminarRol(String idCompany, Long idRol) throws EntityNotFoundException;

    Role obtenerRolPorId(String idCompany, Long idRol) throws EntityNotFoundException;

    List<Role> listarRolesPorCompany(String idCompany);

    PagedResult<Role> listarRolesPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    /** Qué módulos del menú ve cada rol de la empresa, deducido de sus permisos (Spec 1.2). */
    List<RoleMenuSummaryView> resumenDeMenuPorRol(String idCompany);
}
