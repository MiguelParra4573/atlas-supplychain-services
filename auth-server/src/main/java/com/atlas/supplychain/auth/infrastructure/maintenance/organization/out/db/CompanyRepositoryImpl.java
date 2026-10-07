package com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities.CompanyEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.mappers.CompanyMapper;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities.QCompanyEntity.companyEntity;

/**
 * A diferencia de {@link com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.db.AgencyRepositoryImpl},
 * NO hereda el filtro de tenant obligatorio de
 * {@link AbstractQueryDslRepository} (Spec 1.1): {@code Company} es la raíz del árbol de
 * tenant, no un nodo scoped -- forzar el filtro acá sería una contradicción lógica (¿bajo qué
 * empresa se filtra la tabla que define las empresas?). Por eso todas las consultas de esta
 * clase usan el {@code QCompanyEntity} estático y {@code getQueryFactory()} directamente, en
 * vez de {@code findByIdAndCompany}/{@code findAllByCompany}/{@code filterAndSortPaged}
 * heredados (que exigen {@code idCompany} vía {@code forCompany}).
 */
@Repository
@Transactional
@Slf4j
public class CompanyRepositoryImpl
        extends AbstractQueryDslRepository<CompanyEntity, String>
        implements CompanyRepository {

    private final EntityManager entityManager;
    private final CompanyMapper mapper;

    public CompanyRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            CompanyMapper mapper
    ) {
        super(CompanyEntity.class, entityManager, predicateRegistry);
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    @Override
    public void crearCompany(Company company) {
        // idCompany (Spec 1.17): código corto "01".."99" generado por auth.company_id_seq,
        // no un @GeneratedValue de la entidad -- se resuelve acá, antes del persist, vía
        // nextval() nativo (la secuencia no tiene equivalente portable en JPA/QueryDSL).
        Number siguiente = (Number) entityManager
                .createNativeQuery("SELECT nextval('auth.company_id_seq')")
                .getSingleResult();
        company.setIdCompany(String.format("%02d", siguiente.longValue()));
        this.save(mapper.domainToEntity(company));
    }

    @Override
    public void actualizarCompany(Company company) throws EntityNotFoundException {
        if (findById(company.getIdCompany()).isEmpty()) {
            throw new EntityNotFoundException("Empresa no encontrada ID " + company.getIdCompany());
        }
        this.save(mapper.domainToEntity(company));
    }

    @Override
    public Company obtenerCompanyPorId(String idCompany) throws EntityNotFoundException {
        return findById(idCompany)
                .map(mapper::entityToDomain)
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada ID " + idCompany));
    }

    @Override
    public void eliminarCompany(String idCompany) throws EntityNotFoundException {
        CompanyEntity entity = findById(idCompany)
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada ID " + idCompany));
        this.delete(entity);
    }

    @Override
    public List<Company> listarCompaniesPorState(boolean state) {
        List<CompanyEntity> list = getQueryFactory()
                .selectFrom(companyEntity)
                .where(companyEntity.state.eq(state))
                .fetch();
        return list.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public PagedResult<Company> listarCompaniesPaginadas(List<FilterCriterion> filters, PageQuery pageQuery) {
        JPAQuery<CompanyEntity> query = applySorting(applyFilters(newQuery(), filters), pageQuery.sorts());

        long total = query.fetchCount();
        Pageable pageable = toPageable(pageQuery);
        List<Company> content = query.offset(pageable.getOffset()).limit(pageable.getPageSize())
                .fetch().stream().map(mapper::entityToDomain).toList();

        return new PagedResult<>(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    @Override
    public boolean existsByAlias(String alias) {
        return getQueryFactory().selectOne().from(companyEntity)
                .where(companyEntity.alias.eq(alias))
                .fetchFirst() != null;
    }

    @Override
    public boolean existsByRuc(String ruc) {
        return getQueryFactory().selectOne().from(companyEntity)
                .where(companyEntity.ruc.eq(ruc))
                .fetchFirst() != null;
    }

    private Optional<CompanyEntity> findById(String idCompany) {
        return Optional.ofNullable(
                getQueryFactory().selectFrom(companyEntity)
                        .where(companyEntity.idCompany.eq(idCompany)).fetchOne());
    }
}
