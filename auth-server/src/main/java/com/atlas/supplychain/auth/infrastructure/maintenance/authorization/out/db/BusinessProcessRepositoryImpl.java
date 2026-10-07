package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.TenantScopeRequiredException;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.BusinessProcessEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.CompanyProcessEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.BusinessProcessMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QBusinessProcessEntity.businessProcessEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QCompanyProcessEntity.companyProcessEntity;

/**
 * No extiende {@code AbstractQueryDslRepository} a propósito: esa clase base garantiza aislamiento
 * de tenant exigiendo que la entidad raíz tenga {@code idCompany}, y {@code business_process} es
 * **global por diseño** -- es el catálogo que administra el proveedor del ERP, no un dato de cada
 * empresa. Heredarla hacía fallar toda consulta con {@code TenantScopeRequiredException}.
 *
 * <p>El guard sigue vigente donde sí corresponde: las consultas sobre {@code company_process}, que
 * sí es por empresa, validan {@code idCompany} explícitamente en {@link #requireCompany}.</p>
 */
@Repository
@Transactional
@Slf4j
public class BusinessProcessRepositoryImpl implements BusinessProcessRepository {

    private final BusinessProcessMapper mapper;
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public BusinessProcessRepositoryImpl(EntityManager entityManager, BusinessProcessMapper mapper) {
        this.em = entityManager;
        this.mapper = mapper;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public void crear(BusinessProcess process) {
        BusinessProcessEntity entity = mapper.domainToEntity(process);
        em.persist(entity);
        em.flush();
        process.setIdBusinessProcess(entity.getIdBusinessProcess());
    }

    @Override
    public void actualizar(BusinessProcess process) throws EntityNotFoundException {
        buscar(process.getIdBusinessProcess());
        em.merge(mapper.domainToEntity(process));
    }

    @Override
    public BusinessProcess obtenerPorId(Long idBusinessProcess) throws EntityNotFoundException {
        return mapper.entityToDomain(buscar(idBusinessProcess));
    }

    @Override
    public void eliminar(Long idBusinessProcess) throws EntityNotFoundException {
        em.remove(buscar(idBusinessProcess));
    }

    @Override
    public List<BusinessProcess> listar() {
        return queryFactory
                .selectFrom(businessProcessEntity)
                .orderBy(businessProcessEntity.name.asc())
                .fetch()
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return queryFactory
                .selectOne()
                .from(businessProcessEntity)
                .where(businessProcessEntity.code.eq(code))
                .fetchFirst() != null;
    }

    @Override
    public List<CompanyProcess> listarPorEmpresa(String idCompany) {
        requireCompany(idCompany);
        return queryFactory
                .selectFrom(companyProcessEntity)
                .where(companyProcessEntity.idCompany.eq(idCompany))
                .fetch()
                .stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    /**
     * Upsert: hay una sola fila por empresa y proceso (`uk_company_process`), así que desactivar y
     * volver a activar reusa la misma fila en vez de acumular historial. Si hace falta saber cuándo
     * se dio de baja un proceso, eso lo cubre `ms-auditoria` (Spec 1.0 §Riesgos).
     */
    @Override
    public void activar(String idCompany, Long idBusinessProcess, boolean active) throws EntityNotFoundException {
        requireCompany(idCompany);
        // Validar acá y no en el service: así la comprobación y el upsert corren en la misma
        // transacción (la capa de aplicación no puede declararlas, ver ImportControl).
        buscar(idBusinessProcess);
        CompanyProcessEntity existente = queryFactory
                .selectFrom(companyProcessEntity)
                .where(companyProcessEntity.idCompany.eq(idCompany)
                        .and(companyProcessEntity.idBusinessProcess.eq(idBusinessProcess)))
                .fetchOne();

        if (existente == null) {
            em.persist(CompanyProcessEntity.builder()
                    .idCompany(idCompany)
                    .idBusinessProcess(idBusinessProcess)
                    .active(active)
                    .activatedAt(Instant.now())
                    .build());
            return;
        }

        existente.setActive(active);
        if (active) {
            existente.setActivatedAt(Instant.now());
        }
        em.merge(existente);
    }

    /** Mismo criterio que la clase base: un idCompany ausente es un bug, no entrada de usuario. */
    private void requireCompany(String idCompany) {
        if (idCompany == null || idCompany.isBlank()) {
            throw new TenantScopeRequiredException(
                    "idCompany requerido para consultar CompanyProcessEntity, recibido: "
                            + (idCompany == null ? "null" : "en blanco"));
        }
    }

    private BusinessProcessEntity buscar(Long idBusinessProcess) throws EntityNotFoundException {
        BusinessProcessEntity entity = idBusinessProcess == null ? null : em.find(BusinessProcessEntity.class, idBusinessProcess);
        if (entity == null) {
            throw new EntityNotFoundException("Proceso de negocio no encontrado ID " + idBusinessProcess);
        }
        return entity;
    }
}
