package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.RoleMenuSummaryView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.RoleEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.RoleMapper;
import com.querydsl.core.Tuple;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QGrantedPermissionEntity.grantedPermissionEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleEntity.moduleEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QOperationEntity.operationEntity;
import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QRoleEntity.roleEntity;


@Repository
@Transactional
@Slf4j
public class RoleRepositoryImpl
        extends AbstractQueryDslRepository<RoleEntity, Long>
        implements RoleRepository {

    private final RoleMapper mapper;

    public RoleRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            RoleMapper mapper
    ) {
        super(RoleEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crearRol(Role rol) {
        // Spec 1.24: se propaga el id autogenerado (IDENTITY) de vuelta al objeto de dominio --
        // antes se descartaba el retorno de save(...), y RoleServiceImpl.registrarRolCreado
        // (Outbox) necesita un idRole real para el evento ROL_CREADO, no null.
        RoleEntity guardado = this.save(mapper.domainToEntity(rol));
        rol.setIdRol(guardado.getIdRol());
    }

    @Override
    public void actualizarRol(Role rol) throws EntityNotFoundException {
        if (findByIdAndCompany(rol.getIdCompany(), rol.getIdRol()).isEmpty()) {
            throw new EntityNotFoundException(
                    "No se encontró el rol con ID: " + rol.getIdRol()
            );
        }
        this.save(mapper.domainToEntity(rol));
    }

    @Override
    public Role obtenerRolPorId(String idCompany, Long idRol) throws EntityNotFoundException {
        return findByIdAndCompany(idCompany, idRol)
                .map(mapper::entityToDomain)
                .orElseThrow(() ->
                        new EntityNotFoundException("Rol no encontrado ID " + idRol)
                );
    }

    @Override
    public void eliminarRol(String idCompany, Long idRol) throws EntityNotFoundException {
        RoleEntity rolEntity = findByIdAndCompany(idCompany, idRol)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado ID " + idRol));
        this.delete(rolEntity);
    }

    @Override
    public PagedResult<Role> listarRolesPaginados(
            String idCompany,
            List<FilterCriterion> filters,
            PageQuery pageQuery
    ) {
        Page<RoleEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);
        return toPagedResult(result.map(mapper::entityToDomain));
    }

    @Override
    public List<Role> listarRolesPorCompany(String idCompany) {
        requireCompany(idCompany);
        List<RoleEntity> list = getQueryFactory()
                .selectFrom(roleEntity)
                .where(roleEntity.idCompany.eq(idCompany))
                .fetch();

        return list.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public boolean existsByNameAndCompany(String idCompany, String name) {
        requireCompany(idCompany);
        return getQueryFactory()
                .selectOne()
                .from(roleEntity)
                .where(roleEntity.idCompany.eq(idCompany).and(roleEntity.name.eq(name)))
                .fetchFirst() != null;
    }

    /**
     * Una sola consulta agrupada para toda la empresa: la alternativa era pedir los permisos de
     * cada rol por separado, o sea una llamada por fila de la lista de roles.
     *
     * <p>Filtra por operación activa y módulo activo, el mismo criterio de vigencia que
     * {@code User.esVigente} -- si divergieran, la lista prometería un menú distinto del que el
     * usuario termina viendo. {@code countDistinct} sobre {@code resource} cuenta pantallas, no
     * operaciones.</p>
     */
    @Override
    public List<RoleMenuSummaryView> resumenDeMenuPorRol(String idCompany) {
        requireCompany(idCompany);

        List<Tuple> filas = getQueryFactory()
                .select(roleEntity.idRol, moduleEntity.idModule, moduleEntity.name, operationEntity.resource.countDistinct())
                .from(grantedPermissionEntity)
                .join(grantedPermissionEntity.roleEntity, roleEntity)
                .join(grantedPermissionEntity.operationEntity, operationEntity)
                .join(operationEntity.modules, moduleEntity)
                .where(grantedPermissionEntity.idCompany.eq(idCompany)
                        .and(operationEntity.state.isTrue())
                        .and(moduleEntity.state.isTrue()))
                .groupBy(roleEntity.idRol, moduleEntity.idModule, moduleEntity.name)
                .orderBy(roleEntity.idRol.asc(), moduleEntity.name.asc())
                .fetch();

        Map<Long, List<RoleMenuSummaryView.RoleMenuModuleView>> porRol = new LinkedHashMap<>();
        for (Tuple fila : filas) {
            Long idRol = fila.get(roleEntity.idRol);
            Long pantallas = fila.get(operationEntity.resource.countDistinct());
            porRol.computeIfAbsent(idRol, k -> new ArrayList<>())
                    .add(new RoleMenuSummaryView.RoleMenuModuleView(
                            fila.get(moduleEntity.idModule),
                            fila.get(moduleEntity.name),
                            pantallas == null ? 0 : pantallas.intValue()));
        }

        return porRol.entrySet().stream()
                .map(entry -> new RoleMenuSummaryView(entry.getKey(), entry.getValue()))
                .toList();
    }
}
