package com.atlas.supplychain.auth.application.maintenance.authorization.menucategory;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class MenuCategoryServiceImpl implements MenuCategoryService {

    private final MenuCategoryRepository repository;

    @Override
    public void crear(MenuCategory category) throws DuplicateRecordException {
        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }
        validarCodigoUnico(category);
        repository.crear(category);
    }

    @Override
    public void actualizar(MenuCategory category) throws EntityNotFoundException, DuplicateRecordException {
        MenuCategory existente = repository.obtenerPorId(category.getIdCompany(), category.getIdMenuCategory());
        if (!existente.getCode().equals(category.getCode())) {
            validarCodigoUnico(category);
        }
        repository.actualizar(category);
    }

    @Override
    public MenuCategory obtenerPorId(String idCompany, Long idMenuCategory) throws EntityNotFoundException {
        return repository.obtenerPorId(idCompany, idMenuCategory);
    }

    @Override
    public void eliminar(String idCompany, Long idMenuCategory) throws EntityNotFoundException {
        repository.eliminar(idCompany, idMenuCategory);
    }

    @Override
    public List<MenuCategory> listarPorState(String idCompany, Boolean state) {
        return repository.listarPorState(idCompany, state);
    }

    @Override
    public PagedResult<MenuCategory> listarPaginado(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarPaginado(idCompany, filters, pageQuery);
    }

    private void validarCodigoUnico(MenuCategory category) throws DuplicateRecordException {
        if (repository.existsByCode(category.getIdCompany(), category.getCode())) {
            throw new DuplicateRecordException("Ya existe la categoría '" + category.getCode() + "' en esta empresa");
        }
    }
}
