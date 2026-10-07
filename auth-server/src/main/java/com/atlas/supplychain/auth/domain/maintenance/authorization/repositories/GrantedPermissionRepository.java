package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;


import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;
import java.util.Optional;

public interface GrantedPermissionRepository {

    void crearPermiso(GrantedPermission permiso);

    void actualizarPermiso(GrantedPermission permiso) throws EntityNotFoundException;

    GrantedPermission obtenerPermisoPorId(String idCompany, Long idGrantedPermission) throws EntityNotFoundException;

    void eliminarPermiso(String idCompany, Long idGrantedPermission) throws EntityNotFoundException;

    List<GrantedPermission> listarPermisosPorRol(String idCompany, Long idRol);

    PagedResult<GrantedPermission> listarPermisosPaginados(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    Optional<GrantedPermission> obtenerPorRolYOperacion(String idCompany, Long idRol, Long idOperation);

    boolean existsByOperation(String idCompany, Long idOperation);

}
