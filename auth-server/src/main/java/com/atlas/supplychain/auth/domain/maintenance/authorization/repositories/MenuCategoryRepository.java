package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface MenuCategoryRepository {

    void crear(MenuCategory category);

    void actualizar(MenuCategory category) throws EntityNotFoundException;

    MenuCategory obtenerPorId(String idCompany, Long idMenuCategory) throws EntityNotFoundException;

    void eliminar(String idCompany, Long idMenuCategory) throws EntityNotFoundException;

    List<MenuCategory> listarPorState(String idCompany, Boolean state);

    PagedResult<MenuCategory> listarPaginado(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    boolean existsByCode(String idCompany, String code);
}
