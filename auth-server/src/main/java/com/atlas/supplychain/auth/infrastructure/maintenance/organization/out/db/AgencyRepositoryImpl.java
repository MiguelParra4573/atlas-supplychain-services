package com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Agency;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.AgencyRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities.AgencyEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.mappers.AgencyMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities.QAgencyEntity.agencyEntity;

@Repository
@Transactional
@Slf4j
public class AgencyRepositoryImpl
        extends AbstractQueryDslRepository<AgencyEntity, String>
        implements AgencyRepository {

    private final AgencyMapper mapper;

    public AgencyRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            AgencyMapper mapper
    ) {
        super(AgencyEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crearAgency(Agency agency) {
        this.save(mapper.domainToEntity(agency));
    }

    @Override
    public void actualizarAgency(Agency agency) throws EntityNotFoundException {
        if (findByIdAndCompany(agency.getIdCompany(), agency.getIdAgency()).isEmpty()) {
            throw new EntityNotFoundException("Agencia no encontrada ID " + agency.getIdAgency());
        }
        this.save(mapper.domainToEntity(agency));
    }

    @Override
    public Agency obtenerAgencyPorId(String idCompany, String idAgency) throws EntityNotFoundException {
        return findByIdAndCompany(idCompany, idAgency)
                .map(mapper::entityToDomain)
                .orElseThrow(() -> new EntityNotFoundException("Agencia no encontrada ID " + idAgency));
    }

    @Override
    public void eliminarAgency(String idCompany, String idAgency) throws EntityNotFoundException {
        AgencyEntity entity = findByIdAndCompany(idCompany, idAgency)
                .orElseThrow(() -> new EntityNotFoundException("Agencia no encontrada ID " + idAgency));
        this.delete(entity);
    }

    @Override
    public List<Agency> listarAgenciesPorCompany(String idCompany) {
        List<AgencyEntity> list = findAllByCompany(idCompany);
        return list.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public PagedResult<Agency> listarAgenciesPaginadas(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        Page<AgencyEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);
        return toPagedResult(result.map(mapper::entityToDomain));
    }

    @Override
    public boolean existsByCompany(String idCompany) {
        requireCompany(idCompany);
        return getQueryFactory().selectOne().from(agencyEntity)
                .where(agencyEntity.idCompany.eq(idCompany))
                .fetchFirst() != null;
    }
}
