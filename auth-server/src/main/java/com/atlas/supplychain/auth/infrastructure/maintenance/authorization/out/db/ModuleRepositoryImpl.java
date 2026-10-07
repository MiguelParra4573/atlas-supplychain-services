package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ModuleEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ModuleMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleEntity.moduleEntity;

@Repository
@Transactional
@Slf4j
public class ModuleRepositoryImpl
        extends AbstractQueryDslRepository<ModuleEntity, Long>
        implements ModuleRepository {

    private final ModuleMapper mapper;

    public ModuleRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            ModuleMapper mapper
    ) {
        super(ModuleEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crearModulo(Module module) {
        this.save(mapper.domainToEntity(module));
    }

    /**
     * Merge campo a campo ignorando nulos, no un {@code save} de la entidad completa: el DTO trae
     * {@code idBusinessProcess} como opcional (Spec 1.0), así que editar solo el ícono o la
     * categoría sin reenviarlo dejaba el módulo sin proceso. Y un módulo sin proceso está siempre
     * disponible, o sea que el control de acceso por proceso se desarmaba en silencio y en la
     * dirección peligrosa.
     */
    @Override
    public void actualizarModulo(Module module) throws EntityNotFoundException {
        ModuleEntity existente = findByIdAndCompany(module.getIdCompany(), module.getIdModule())
                .orElseThrow(() -> new EntityNotFoundException("Módulo no encontrado ID " + module.getIdModule()));
        mapper.update(module, existente);
        this.save(existente);
    }

    @Override
    public Module obtenerModuloPorId(String idCompany, Long idModule) throws EntityNotFoundException {
        return findByIdAndCompany(idCompany, idModule)
                .map(mapper::entityToDomain)
                .orElseThrow(() ->
                        new EntityNotFoundException("Módulo no encontrado ID " + idModule)
                );
    }

    @Override
    public void eliminarModulo(String idCompany, Long idModule) throws EntityNotFoundException {
        ModuleEntity entity = findByIdAndCompany(idCompany, idModule)
                .orElseThrow(() -> new EntityNotFoundException("Módulo no encontrado ID " + idModule));
        this.delete(entity);
    }

    @Override
    public List<Module> listarModulosPorState(String idCompany, Boolean state) {
        requireCompany(idCompany);

        List<ModuleEntity> list = getQueryFactory()
                .selectFrom(moduleEntity)
                .where(moduleEntity.idCompany.eq(idCompany)
                        .and(moduleEntity.state.eq(state)))
                .fetch();

        return list.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public PagedResult<Module> listarModulosPaginados(
            String idCompany,
            List<FilterCriterion> filters,
            PageQuery pageQuery) {

        Page<ModuleEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);

        return toPagedResult(result.map(mapper::entityToDomain));
    }

    @Override
    public Optional<Module> buscarPorNombre(String idCompany, String name) {
        requireCompany(idCompany);

        return Optional.ofNullable(
                getQueryFactory()
                        .selectFrom(moduleEntity)
                        .where(moduleEntity.idCompany.eq(idCompany).and(moduleEntity.name.eq(name)))
                        .fetchOne()
        ).map(mapper::entityToDomain);
    }
}
