package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.MenuCategoryEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.MenuCategoryMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QMenuCategoryEntity.menuCategoryEntity;

@Repository
@Transactional
@Slf4j
public class MenuCategoryRepositoryImpl
        extends AbstractQueryDslRepository<MenuCategoryEntity, Long>
        implements MenuCategoryRepository {

    private final MenuCategoryMapper mapper;

    public MenuCategoryRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            MenuCategoryMapper mapper
    ) {
        super(MenuCategoryEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crear(MenuCategory category) {
        this.save(mapper.domainToEntity(category));
    }

    @Override
    public void actualizar(MenuCategory category) throws EntityNotFoundException {
        MenuCategoryEntity existente = findByIdAndCompany(category.getIdCompany(), category.getIdMenuCategory())
                .orElseThrow(() -> new EntityNotFoundException("Categoría de menú no encontrada ID " + category.getIdMenuCategory()));
        mapper.update(category, existente);
        this.save(existente);
    }

    @Override
    public MenuCategory obtenerPorId(String idCompany, Long idMenuCategory) throws EntityNotFoundException {
        return findByIdAndCompany(idCompany, idMenuCategory)
                .map(mapper::entityToDomain)
                .orElseThrow(() -> new EntityNotFoundException("Categoría de menú no encontrada ID " + idMenuCategory));
    }

    @Override
    public void eliminar(String idCompany, Long idMenuCategory) throws EntityNotFoundException {
        MenuCategoryEntity entity = findByIdAndCompany(idCompany, idMenuCategory)
                .orElseThrow(() -> new EntityNotFoundException("Categoría de menú no encontrada ID " + idMenuCategory));
        this.delete(entity);
    }

    @Override
    public List<MenuCategory> listarPorState(String idCompany, Boolean state) {
        requireCompany(idCompany);

        List<MenuCategoryEntity> list = getQueryFactory()
                .selectFrom(menuCategoryEntity)
                .where(menuCategoryEntity.idCompany.eq(idCompany)
                        .and(menuCategoryEntity.state.eq(state)))
                .orderBy(menuCategoryEntity.displayOrder.asc(), menuCategoryEntity.name.asc())
                .fetch();

        return list.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public PagedResult<MenuCategory> listarPaginado(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        Page<MenuCategoryEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);
        return toPagedResult(result.map(mapper::entityToDomain));
    }

    @Override
    public boolean existsByCode(String idCompany, String code) {
        requireCompany(idCompany);

        return getQueryFactory()
                .selectOne()
                .from(menuCategoryEntity)
                .where(menuCategoryEntity.idCompany.eq(idCompany).and(menuCategoryEntity.code.eq(code)))
                .fetchFirst() != null;
    }
}
