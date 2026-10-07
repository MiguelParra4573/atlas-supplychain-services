package com.atlas.supplychain.auth.infrastructure.common.db;

import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilder;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.domain.shared.query.SortCriterion;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.support.PageableExecutionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractQueryDslRepository<T, ID> {

    private final Class<T> clazz;
    private final EntityManager entityManager;
    private final SimpleJpaRepository<T, ID> delegate;
    private final JPAQueryFactory queryFactory;
    private final PathBuilder<T> root;
    private final Querydsl querydsl;
    private final PredicateBuilderRegistry predicateRegistry;

    protected AbstractQueryDslRepository(Class<T> domainClazz, EntityManager entityManager, PredicateBuilderRegistry predicateRegistry) {
        this.clazz = domainClazz;
        this.entityManager = entityManager;
        this.predicateRegistry = predicateRegistry;

        this.queryFactory = new JPAQueryFactory(entityManager);
        this.root = new PathBuilderFactory().create(domainClazz);
        this.querydsl = new Querydsl(entityManager, this.root);
        this.delegate = new SimpleJpaRepository<>(domainClazz, entityManager);
    }

    public Optional<T> findByIdAndCompany(String idCompany, ID id) {
        JPAQuery<T> query = forCompany(newQuery(), idCompany)
                .where(root.get("id").eq(id));

        if (hasField("estado")) {
            query.where(root.getBoolean("estado").isTrue());
        }

        return Optional.ofNullable(query.fetchOne());
    }

    public T save(T entity) {
        return delegate.save(entity);
    }

    public void delete(T entity) {
        delegate.delete(entity);
    }

    public List<T> findAllByCompany(String idCompany) {
        JPAQuery<T> query = newQuery();
        query = forCompany(query, idCompany);
        query = getActivos(query);
        return query.fetch();
    }

    // ================== QUERY FACTORY BASE ===============

    protected JPAQuery<T> newQuery() {
        return queryFactory.selectFrom(root);
    }

    protected JPAQueryFactory getQueryFactory() {
        return this.queryFactory;
    }

    // ============== FILTROS GENERALES (EMPRESA / ESTADO) ==============

    /**
     * Único punto de paso obligado para el filtro de tenant: {@link #findAllByCompany},
     * {@link #findByIdAndCompany} y {@link #filterAndSortPaged} pasan todos por acá. No hay
     * forma de obtener una query sin filtrar -- idCompany ausente, en blanco, o una entidad
     * sin campo idCompany son todos bugs de programación (endpoint sin auth, adaptador que
     * no propagó el principal), no entrada de usuario, y fallan ruidosamente en vez de
     * degradar en silencio a "todas las empresas".
     */
    protected JPAQuery<T> forCompany(JPAQuery<T> query, String idCompany) {
        requireCompany(idCompany);
        return query.where(root.getString("idCompany").eq(idCompany));
    }

    /**
     * Misma validación que {@link #forCompany}, para subclases que arman su propia query con
     * el Q-type estático de QueryDSL en vez del {@code root} genérico (no se pueden mezclar
     * ambos en un mismo query) y aun así necesitan el mismo guardia -- fallar ruidoso en vez
     * de dejar pasar un idCompany ausente que produciría 0 filas en silencio.
     */
    protected void requireCompany(String idCompany) {
        if (idCompany == null || idCompany.isBlank()) {
            throw new TenantScopeRequiredException(
                    "idCompany requerido para consultar " + clazz.getSimpleName() + ", recibido: " + (idCompany == null ? "null" : "en blanco"));
        }
        if (!hasField("idCompany")) {
            throw new TenantScopeRequiredException(
                    clazz.getSimpleName() + " no tiene campo idCompany -- no se puede garantizar el aislamiento de tenant");
        }
    }

    protected JPAQuery<T> getActivos(JPAQuery<T> query) {
        if (!hasField("estado")) return query;
        return query.where(root.getBoolean("estado").isTrue());
    }

    // ================== FILTROS DINÁMICOS ==================

    /**
     * Aplica filtros dinámicos usando los Strategy (PredicateBuilder).
     */
    protected JPAQuery<T> applyFilters(JPAQuery<T> query, List<FilterCriterion> filters) {
        if (filters == null || filters.isEmpty()) return query;

        List<BooleanExpression> predicates = filters.stream().map(this::buildPredicate).filter(Objects::nonNull).collect(Collectors.toList());

        if (predicates.isEmpty()) return query;

        return query.where(predicates.toArray(BooleanExpression[]::new));
    }

    /**
     * Construye un BooleanExpression para un filtro individual.
     */
    private BooleanExpression buildPredicate(FilterCriterion fc) {
        try {
            Class<?> fieldType = getFieldType(fc.field());
            PredicateBuilder builder = predicateRegistry.getBuilder(fieldType);
            return builder.build(root, fc);
        } catch (Exception ex) {
            log.error("Error de filtrado en el campo {}: {}", fc.field(), ex.getMessage());
            return null;
        }
    }

    private Class<?> getFieldType(String field) {
        try {
            return clazz.getDeclaredField(field).getType();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("El campo %s no existe en %s".formatted(field, clazz.getSimpleName()));
        }
    }

    // ================= ORDENAMIENTO DINÁMICO =================

    /**
     * ADR-006 ({@code docs/auditoria/11-backlog-y-adrs.md} §L): Dependabot marca
     * {@code com.querydsl:querydsl-jpa} vulnerable a HQL injection vía {@code orderBy}
     * (GHSA-6q3q-6v5j-h6vg / CVE-2024-49203) -- no explotable acá porque {@link #buildOrder}
     * valida {@code criterio.field()} con {@code Class.getDeclaredField} (ver más abajo) antes
     * de construir el {@code PathBuilder}: un nombre de campo Java real nunca contiene espacios,
     * comillas ni palabras clave HQL, que es justo lo que el PoC del CVE necesita. No quitar esa
     * validación al tocar este método.
     */
    protected JPAQuery<T> applySorting(JPAQuery<T> query, List<SortCriterion> sorts) {
        if (sorts == null || sorts.isEmpty()) return query;

        List<? extends OrderSpecifier<?>> orders = sorts.stream().map(sc -> buildOrder(root, sc)).toList();

        return query.orderBy(orders.toArray(new OrderSpecifier[0]));
    }

    private OrderSpecifier<?> buildOrder(PathBuilder<?> path, SortCriterion criterio) {
        Class<?> type = getFieldType(criterio.field());

        return switch (type.getSimpleName()) {
            case "String" -> {
                StringPath p = path.getString(criterio.field());
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            case "Integer" -> {
                NumberPath<Integer> p = path.getNumber(criterio.field(), Integer.class);
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            case "Long" -> {
                NumberPath<Long> p = path.getNumber(criterio.field(), Long.class);
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            case "BigDecimal" -> {
                NumberPath<BigDecimal> p = path.getNumber(criterio.field(), BigDecimal.class);
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            case "LocalDate" -> {
                DatePath<LocalDate> p = path.getDate(criterio.field(), LocalDate.class);
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            case "LocalDateTime" -> {
                DateTimePath<LocalDateTime> p = path.getDateTime(criterio.field(), LocalDateTime.class);
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            case "Boolean" -> {
                BooleanPath p = path.getBoolean(criterio.field());
                yield criterio.ascending() ? p.asc() : p.desc();
            }
            default -> throw new IllegalArgumentException("Tipo no soportado para ordenar: " + type.getSimpleName());
        };
    }

    // ================= PAGE GENERAL =================

    /**
     * idCompany se aplica ANTES que los filtros dinámicos del cliente: evita que un
     * filtro/búsqueda cruce el límite de tenant. No es opcional -- ver {@link #forCompany}.
     */
    public Page<T> filterAndSortPaged(List<FilterCriterion> filters, List<SortCriterion> sorts, Pageable pageable, String idCompany) {

        JPAQuery<T> query = newQuery();
        query = forCompany(query, idCompany);
        query = applyFilters(query, filters);
        query = applySorting(query, sorts);

        long total = query.fetchCount();

        List<T> content = query.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    // ============= CONVERSIÓN PageQuery/PagedResult <-> Pageable/Page =============
    // Vive acá (infrastructure/proceso/out/db/) para que domain/ no conozca Spring Data.

    protected static Pageable toPageable(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size());
    }

    protected static <X> PagedResult<X> toPagedResult(Page<X> page) {
        return new PagedResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // ================== PAGINACIÓN REUTILIZABLE ==================

    protected <P> JPQLQuery<P> cloneQuery(JPAQuery<P> query) {
        return query.clone(entityManager);
    }

    protected <Q> Page<Q> executePagedQuery(JPQLQuery<Q> query, Pageable pageable) {
        JPQLQuery<Q> countQuery = cloneQuery((JPAQuery<Q>) query);
        return PageableExecutionUtils.getPage(querydsl.applyPagination(pageable, query).fetch(), pageable, countQuery::fetchCount);
    }

    // ================== UTILITARIOS REFLECTION ==================

    private boolean hasField(String field) {
        try {
            clazz.getDeclaredField(field);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    // ================== UPDATE GENÉRICO (OUTBOX / DTO PATCH) ==================
    //TODO: Verificar si es es realmente util este metodo
    public static <T> boolean buildUpdateClause(JPAUpdateClause updateClause, T dto, PathBuilder<?> entityPath, Class<?> entityClass) {

        List<String> pkFieldNames = getPrimaryKeyFieldNames(entityClass);
        boolean hayActualizaciones = false;

        for (Field field : dto.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(dto);
                if (value != null && !pkFieldNames.contains(field.getName())) {
                    updateClause.set(entityPath.get(field.getName()), value);
                    hayActualizaciones = true;
                }
            } catch (IllegalAccessException e) {
                log.error("Error accediendo al campo {}", field.getName(), e);
            }
        }
        return hayActualizaciones;
    }

    // TODO: mejorar para claves compuestas reales (IdClass / EmbeddedId)
    private static List<String> getPrimaryKeyFieldNames(Class<?> entityClass) {
        Field idField = Arrays.stream(entityClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Id.class)).findFirst().orElseThrow(() -> new IllegalArgumentException("No se encontró campo @Id"));

        if (!idField.getType().isPrimitive() && !idField.getType().equals(String.class)) {
            return Arrays.stream(idField.getType().getDeclaredFields()).map(Field::getName).toList();
        } else {
            return Collections.singletonList(idField.getName());
        }
    }
}