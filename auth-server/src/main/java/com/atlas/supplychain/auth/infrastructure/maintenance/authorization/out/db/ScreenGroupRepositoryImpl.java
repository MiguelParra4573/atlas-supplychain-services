package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ScreenGroupEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ScreenGroupMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QScreenGroupEntity.screenGroupEntity;

@Repository
@Transactional
@Slf4j
public class ScreenGroupRepositoryImpl
        extends AbstractQueryDslRepository<ScreenGroupEntity, Long>
        implements ScreenGroupRepository {

    private final ScreenGroupMapper mapper;

    public ScreenGroupRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            ScreenGroupMapper mapper
    ) {
        super(ScreenGroupEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crear(ScreenGroup group) {
        this.save(mapper.domainToEntity(group));
    }

    @Override
    public void actualizar(ScreenGroup group) throws EntityNotFoundException {
        ScreenGroupEntity existente = findByIdAndCompany(group.getIdCompany(), group.getIdScreenGroup())
                .orElseThrow(() -> new EntityNotFoundException("Carpeta no encontrada ID " + group.getIdScreenGroup()));
        mapper.update(group, existente);
        this.save(existente);
    }

    @Override
    public void eliminar(String idCompany, Long idScreenGroup) throws EntityNotFoundException {
        ScreenGroupEntity entity = findByIdAndCompany(idCompany, idScreenGroup)
                .orElseThrow(() -> new EntityNotFoundException("Carpeta no encontrada ID " + idScreenGroup));
        this.delete(entity);
    }

    @Override
    public List<ScreenGroup> listarPorModulo(String idCompany, Long idModule) {
        requireCompany(idCompany);

        List<ScreenGroupEntity> list = getQueryFactory()
                .selectFrom(screenGroupEntity)
                .where(screenGroupEntity.idCompany.eq(idCompany)
                        .and(screenGroupEntity.idModule.eq(idModule))
                        .and(screenGroupEntity.state.eq(true)))
                .orderBy(screenGroupEntity.displayOrder.asc(), screenGroupEntity.name.asc())
                .fetch();

        return list.stream().map(mapper::entityToDomain).toList();
    }
}
