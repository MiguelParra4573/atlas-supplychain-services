package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ScreenSummary;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ModuleEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.OperationEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QOperationEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ModuleMapper;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.OperationMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleEntity.moduleEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleOperationEntity.moduleOperationEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QOperationEntity.operationEntity;

@Repository
@Transactional
@Slf4j
public class OperationRepositoryImpl
        extends AbstractQueryDslRepository<OperationEntity, Long>
        implements OperationRepository {

    private final OperationMapper mapper;
    private final ModuleMapper moduleMapper;

    public OperationRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            OperationMapper mapper,
            ModuleMapper moduleMapper
    ) {
        super(OperationEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
        this.moduleMapper = moduleMapper;
    }

    @Override
    public void crearOperacion(Operation operation) {
        this.save(mapper.domainToEntity(operation));
    }

    @Override
    public void actualizarOperacion(Operation operation) throws EntityNotFoundException {
        if (findByIdAndCompany(operation.getIdCompany(), operation.getIdOperation()).isEmpty()) {
            throw new EntityNotFoundException("Operación no encontrada ID " + operation.getIdOperation());
        }
        this.save(mapper.domainToEntity(operation));
    }

    @Override
    public Operation obtenerOperacionPorId(String idCompany, Long idOperation) throws EntityNotFoundException {
        return findByIdAndCompany(idCompany, idOperation)
                .map(mapper::entityToDomain)
                .orElseThrow(() ->
                        new EntityNotFoundException("Operación no encontrada ID " + idOperation)
                );
    }

    @Override
    public void eliminarOperacion(String idCompany, Long idOperation) throws EntityNotFoundException {
        OperationEntity entity = findByIdAndCompany(idCompany, idOperation)
                .orElseThrow(() -> new EntityNotFoundException("Operación no encontrada ID " + idOperation));
        this.delete(entity);
    }

    @Override
    public List<Operation> listarOperacionesPorModulo(String idCompany, Long idModule) {
        requireCompany(idCompany);

        List<OperationEntity> list = getQueryFactory()
                .selectFrom(operationEntity)
                .where(operationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.idOperation.in(operacionesAsignadasA(idCompany, idModule)))
                        .and(operationEntity.state.isTrue()))
                .fetch();

        return list.stream().map(mapper::entityToDomainParaListado).toList();
    }

    @Override
    public List<Operation> listarPorRecurso(String idCompany, String resource) {
        requireCompany(idCompany);

        List<OperationEntity> list = getQueryFactory()
                .selectFrom(operationEntity)
                .where(operationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.resource.eq(resource))
                        .and(operationEntity.state.isTrue()))
                .fetch();

        return list.stream().map(mapper::entityToDomainParaListado).toList();
    }

    @Override
    public List<Operation> listarOperacionesSinModulo(String idCompany) {
        requireCompany(idCompany);

        List<OperationEntity> list = getQueryFactory()
                .selectFrom(operationEntity)
                .where(operationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.state.isTrue())
                        .and(operationEntity.idOperation.notIn(
                                getQueryFactory()
                                        .select(moduleOperationEntity.id.idOperation)
                                        .from(moduleOperationEntity)
                                        .where(moduleOperationEntity.idCompany.eq(idCompany)))))
                .orderBy(operationEntity.resource.asc(), operationEntity.tag.asc())
                .fetch();

        return list.stream().map(mapper::entityToDomainParaListado).toList();
    }

    @Override
    public PagedResult<Operation> listarOperacionesPaginadas(
            String idCompany,
            List<FilterCriterion> filters,
            PageQuery pageQuery) {

        Page<OperationEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);

        return toPagedResult(result.map(mapper::entityToDomainParaListado));
    }

    @Override
    public boolean existsByResourceAndTag(String idCompany, String resource, String tag) {
        requireCompany(idCompany);

        return getQueryFactory()
                .selectOne()
                .from(operationEntity)
                .where(operationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.resource.eq(resource))
                        .and(operationEntity.tag.eq(tag)))
                .fetchFirst() != null;
    }

    @Override
    public Optional<Operation> buscarPorRecursoYTag(String idCompany, String resource, String tag) {
        requireCompany(idCompany);

        return Optional.ofNullable(
                getQueryFactory()
                        .selectFrom(operationEntity)
                        .where(operationEntity.idCompany.eq(idCompany)
                                .and(operationEntity.resource.eq(resource))
                                .and(operationEntity.tag.eq(tag)))
                        .fetchOne()
        ).map(mapper::entityToDomain);
    }

    /**
     * 3 queries por página, no N+1: (1) página de {@code resource} distintos + count, (2) una
     * consulta en bloque para la etapa representativa de esos {@code resource}, (3) una consulta
     * en bloque (con {@code distinct}) para sus módulos asignados. El costo no crece con el tamaño
     * de página más allá de esas 3 idas a la base.
     */
    @Override
    public PagedResult<ScreenSummary> buscarPantallas(String idCompany, String resourceLike, boolean onlyUnassigned,
                                                        PageQuery pageQuery) {
        requireCompany(idCompany);

        BooleanBuilder filtro = new BooleanBuilder(operationEntity.idCompany.eq(idCompany));
        if (resourceLike != null && !resourceLike.isBlank()) {
            filtro.and(operationEntity.resource.containsIgnoreCase(resourceLike));
        }
        if (onlyUnassigned) {
            // Alias propio: es una subconsulta sobre la misma entidad que la consulta externa, y
            // QueryDSL necesita un Q-instance distinto para no confundir los dos `operationEntity`.
            QOperationEntity opAsignadas = new QOperationEntity("opAsignadas");
            filtro.and(operationEntity.resource.notIn(
                    JPAExpressions.select(opAsignadas.resource)
                            .from(moduleOperationEntity)
                            .join(opAsignadas).on(opAsignadas.idOperation.eq(moduleOperationEntity.id.idOperation))
                            .where(moduleOperationEntity.idCompany.eq(idCompany))));
        }

        long total = getQueryFactory()
                .select(operationEntity.resource.countDistinct())
                .from(operationEntity)
                .where(filtro)
                .fetchOne();

        List<String> resources = getQueryFactory()
                .select(operationEntity.resource).distinct()
                .from(operationEntity)
                .where(filtro)
                .orderBy(operationEntity.resource.asc())
                .offset((long) pageQuery.page() * pageQuery.size())
                .limit(pageQuery.size())
                .fetch();

        if (resources.isEmpty()) {
            return new PagedResult<>(List.of(), pageQuery.page(), pageQuery.size(), total);
        }

        Map<String, Long> etapaPorRecurso = getQueryFactory()
                .selectFrom(operationEntity)
                .where(operationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.resource.in(resources))
                        .and(operationEntity.idProcessStage.isNotNull()))
                .fetch()
                .stream()
                .collect(Collectors.toMap(OperationEntity::getResource, OperationEntity::getIdProcessStage, (primera, segunda) -> primera));

        // `name`/`icon` (Spec 1.4): misma idea que `etapaPorRecurso` -- se toma la primera
        // operación del recurso que los declare, en una sola consulta en bloque, no una por fila.
        List<OperationEntity> filasConNombre = getQueryFactory()
                .selectFrom(operationEntity)
                .where(operationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.resource.in(resources))
                        .and(operationEntity.name.isNotNull().or(operationEntity.icon.isNotNull())))
                .fetch();
        Map<String, String> nombrePorRecurso = filasConNombre.stream()
                .filter(op -> op.getName() != null)
                .collect(Collectors.toMap(OperationEntity::getResource, OperationEntity::getName, (primera, segunda) -> primera));
        Map<String, String> iconoPorRecurso = filasConNombre.stream()
                .filter(op -> op.getIcon() != null)
                .collect(Collectors.toMap(OperationEntity::getResource, OperationEntity::getIcon, (primera, segunda) -> primera));

        List<Tuple> filasModulos = getQueryFactory()
                .select(operationEntity.resource, moduleEntity).distinct()
                .from(moduleOperationEntity)
                .join(operationEntity).on(operationEntity.idOperation.eq(moduleOperationEntity.id.idOperation))
                .join(moduleEntity).on(moduleEntity.idModule.eq(moduleOperationEntity.id.idModule))
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(operationEntity.resource.in(resources)))
                .fetch();

        Map<String, List<Module>> modulosPorRecurso = new LinkedHashMap<>();
        for (Tuple fila : filasModulos) {
            String recurso = fila.get(operationEntity.resource);
            ModuleEntity modulo = fila.get(moduleEntity);
            modulosPorRecurso.computeIfAbsent(recurso, r -> new ArrayList<>()).add(moduleMapper.entityToDomain(modulo));
        }

        List<ScreenSummary> content = resources.stream()
                .map(recurso -> new ScreenSummary(recurso, nombrePorRecurso.get(recurso), iconoPorRecurso.get(recurso),
                        etapaPorRecurso.get(recurso), modulosPorRecurso.getOrDefault(recurso, List.of())))
                .toList();

        return new PagedResult<>(content, pageQuery.page(), pageQuery.size(), total);
    }

    private com.querydsl.jpa.JPQLQuery<Long> operacionesAsignadasA(String idCompany, Long idModule) {
        return getQueryFactory()
                .select(moduleOperationEntity.id.idOperation)
                .from(moduleOperationEntity)
                .where(moduleOperationEntity.idCompany.eq(idCompany)
                        .and(moduleOperationEntity.id.idModule.eq(idModule)));
    }
}
