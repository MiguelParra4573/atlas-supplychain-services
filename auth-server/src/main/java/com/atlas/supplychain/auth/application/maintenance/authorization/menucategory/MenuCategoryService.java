package com.atlas.supplychain.auth.application.maintenance.authorization.menucategory;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface MenuCategoryService {

    void crear(MenuCategory category) throws DuplicateRecordException;

    void actualizar(MenuCategory category) throws EntityNotFoundException, DuplicateRecordException;

    MenuCategory obtenerPorId(String idCompany, Long idMenuCategory) throws EntityNotFoundException;

    void eliminar(String idCompany, Long idMenuCategory) throws EntityNotFoundException;

    List<MenuCategory> listarPorState(String idCompany, Boolean state);

    PagedResult<MenuCategory> listarPaginado(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);
}
