package com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;
import java.util.Set;

public interface GrantedPermissionService {

    void asignarPermisoARol(String idCompany, Long idRol, Long idOperation) throws EntityNotFoundException;

    void revocarPermiso(String idCompany, Long idGrantedPermission) throws EntityNotFoundException;

    Set<String> obtenerPermisosEfectivos(String idUser, String idCompany);

    GrantedPermission obtenerPermisoPorId(String idCompany, Long idGrantedPermission) throws EntityNotFoundException;

    List<GrantedPermission> listarPermisosPorRol(String idCompany, Long idRol);

    PagedResult<GrantedPermission> listarPermisosPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);
}
