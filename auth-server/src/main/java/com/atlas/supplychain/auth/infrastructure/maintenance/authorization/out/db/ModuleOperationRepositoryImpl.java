package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ModuleEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ModuleOperationEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ModuleMapper;
import com.querydsl.jpa.JPAExpressions;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleEntity.moduleEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleOperationEntity.moduleOperationEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QOperationEntity.operationEntity;

@Repository
@Transactional
@Slf4j
public class ModuleOperationRepositoryImpl
        extends AbstractQueryDslRepository<ModuleOperationEntity, ModuleOperationEntity.ModuleOperationId>
        implements ModuleOperationRepository {

    private final ModuleMapper moduleMapper;

    public ModuleOperationRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            ModuleMapper moduleMapper
    ) {
        super(ModuleOperationEntity.class, entityManager, predicateRegistry);
        this.moduleMapper = moduleMapper;
    }

    @Override
    public void asignar(String idCompany, Long idModule, Long idOperation) {
        requireCompany(idCompany);

        if (existeAsignacion(idCompany, idModule, idOperation)) {
            return;
        }

        this.save(ModuleOperationEntity.builder()
                .id(ModuleOperationEntity.ModuleOperationId.builder()
                        .idModule(idModule)
                        .idOperation(idOperation)
                        .build())
                .idCompany(idCompany)
                .build());
    }

    @Override
    public void desasignar(String idCompany, Long idModule, Long idOperation) {
        requireCompany(idCompany);

        getQueryFactory()
                .delete(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule))
                        .and(moduleOperationEntity.id.idOperation.eq(idOperation)))
                .execute();
    }

    @Override
    public List<Module> listarModulosDeOperacion(String idCompany, Long idOperation) {
        requireCompany(idCompany);

        List<ModuleEntity> list = getQueryFactory()
                .select(moduleEntity)
                .from(moduleOperationEntity)
                .join(moduleEntity).on(moduleEntity.idModule.eq(moduleOperationEntity.id.idModule))
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idOperation.eq(idOperation)))
                .orderBy(moduleEntity.name.asc())
                .fetch();

        return list.stream().map(moduleMapper::entityToDomain).toList();
    }

    @Override
    public void eliminarAsignacionesDeModulo(String idCompany, Long idModule) {
        requireCompany(idCompany);

        getQueryFactory()
                .delete(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule)))
                .execute();
    }

    @Override
    public void eliminarAsignacionesDeOperacion(String idCompany, Long idOperation) {
        requireCompany(idCompany);

        getQueryFactory()
                .delete(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idOperation.eq(idOperation)))
                .execute();
    }

    @Override
    public Map<Long, Integer> displayOrdersDeModulo(String idCompany, Long idModule) {
        requireCompany(idCompany);

        List<ModuleOperationEntity> filas = getQueryFactory()
                .selectFrom(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule))
                        .and(moduleOperationEntity.displayOrder.isNotNull()))
                .fetch();

        return filas.stream().collect(java.util.stream.Collectors.toMap(
                fila -> fila.getId().getIdOperation(), ModuleOperationEntity::getDisplayOrder));
    }

    @Override
    public void reordenarPantallas(String idCompany, Long idModule, Map<String, Integer> displayOrderPorRecurso) {
        requireCompany(idCompany);

        displayOrderPorRecurso.forEach((resource, displayOrder) ->
                getQueryFactory()
                        .update(moduleOperationEntity)
                        .set(moduleOperationEntity.displayOrder, displayOrder)
                        .where(moduleOperationEntity.idCompany.eq(idCompany)
                                .and(moduleOperationEntity.id.idModule.eq(idModule))
                                .and(moduleOperationEntity.id.idOperation.in(
                                        JPAExpressions.select(operationEntity.idOperation)
                                                .from(operationEntity)
                                                .where(operationEntity.idCompany.eq(idCompany)
                                                        .and(operationEntity.resource.eq(resource))))))
                        .execute());
    }

    @Override
    public void asignarCarpetaARecurso(String idCompany, Long idModule, String resource, Long idScreenGroup) {
        requireCompany(idCompany);

        getQueryFactory()
                .update(moduleOperationEntity)
                .set(moduleOperationEntity.idScreenGroup, idScreenGroup)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule))
                        .and(moduleOperationEntity.id.idOperation.in(
                                JPAExpressions.select(operationEntity.idOperation)
                                        .from(operationEntity)
                                        .where(operationEntity.idCompany.eq(idCompany)
                                                .and(operationEntity.resource.eq(resource))))))
                .execute();
    }

    @Override
    public Map<Long, Long> screenGroupsDeModulo(String idCompany, Long idModule) {
        requireCompany(idCompany);

        List<ModuleOperationEntity> filas = getQueryFactory()
                .selectFrom(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule))
                        .and(moduleOperationEntity.idScreenGroup.isNotNull()))
                .fetch();

        return filas.stream().collect(java.util.stream.Collectors.toMap(
                fila -> fila.getId().getIdOperation(), ModuleOperationEntity::getIdScreenGroup));
    }

    private boolean existeAsignacion(String idCompany, Long idModule, Long idOperation) {
        return getQueryFactory()
                .selectOne()
                .from(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule))
                        .and(moduleOperationEntity.id.idOperation.eq(idOperation)))
                .fetchFirst() != null;
    }
}
