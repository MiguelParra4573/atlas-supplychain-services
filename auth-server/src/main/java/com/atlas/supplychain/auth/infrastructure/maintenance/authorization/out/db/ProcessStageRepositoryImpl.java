package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ProcessStage;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ProcessStageRepository;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ProcessStageEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ProcessStageMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QProcessStageEntity.processStageEntity;

/**
 * No extiende {@code AbstractQueryDslRepository} por el mismo motivo que
 * {@code BusinessProcessRepositoryImpl}: {@code process_stage} es catálogo global del proveedor,
 * sin {@code id_company} -- heredar el guard de tenant haría fallar toda consulta.
 */
@Repository
@Transactional
@Slf4j
public class ProcessStageRepositoryImpl implements ProcessStageRepository {

    private final ProcessStageMapper mapper;
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public ProcessStageRepositoryImpl(EntityManager entityManager, ProcessStageMapper mapper) {
        this.em = entityManager;
        this.mapper = mapper;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public void crear(ProcessStage stage) {
        ProcessStageEntity entity = mapper.domainToEntity(stage);
        em.persist(entity);
        em.flush();
        stage.setIdProcessStage(entity.getIdProcessStage());
    }

    @Override
    public void actualizar(ProcessStage stage) throws EntityNotFoundException {
        buscar(stage.getIdProcessStage());
        em.merge(mapper.domainToEntity(stage));
    }

    @Override
    public ProcessStage obtenerPorId(Long idProcessStage) throws EntityNotFoundException {
        return mapper.entityToDomain(buscar(idProcessStage));
    }

    @Override
    public void eliminar(Long idProcessStage) throws EntityNotFoundException {
        em.remove(buscar(idProcessStage));
    }

    @Override
    public List<ProcessStage> listar() {
        return queryFactory
                .selectFrom(processStageEntity)
                .orderBy(processStageEntity.idBusinessProcess.asc(), processStageEntity.sequence.asc())
                .fetch()
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public List<ProcessStage> listarPorProceso(Long idBusinessProcess) {
        return queryFactory
                .selectFrom(processStageEntity)
                .where(processStageEntity.idBusinessProcess.eq(idBusinessProcess))
                .orderBy(processStageEntity.sequence.asc())
                .fetch()
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public boolean existsByCodeEnProceso(Long idBusinessProcess, String code) {
        return queryFactory
                .selectOne()
                .from(processStageEntity)
                .where(processStageEntity.idBusinessProcess.eq(idBusinessProcess)
                        .and(processStageEntity.code.eq(code)))
                .fetchFirst() != null;
    }

    @Override
    public boolean existsBySequenceEnProceso(Long idBusinessProcess, Integer sequence) {
        return queryFactory
                .selectOne()
                .from(processStageEntity)
                .where(processStageEntity.idBusinessProcess.eq(idBusinessProcess)
                        .and(processStageEntity.sequence.eq(sequence)))
                .fetchFirst() != null;
    }

    private ProcessStageEntity buscar(Long idProcessStage) throws EntityNotFoundException {
        ProcessStageEntity entity = idProcessStage == null ? null : em.find(ProcessStageEntity.class, idProcessStage);
        if (entity == null) {
            throw new EntityNotFoundException("Etapa de proceso no encontrada ID " + idProcessStage);
        }
        return entity;
    }
}
