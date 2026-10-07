package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.GrantedPermissionEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.RoleEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.GrantedPermissionMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QGrantedPermissionEntity.grantedPermissionEntity;

@Repository
@Transactional
@Slf4j
public class GrantedPermissionRepositoryImpl
        extends AbstractQueryDslRepository<GrantedPermissionEntity, Long>
        implements GrantedPermissionRepository {

    private final GrantedPermissionMapper mapper;

    public GrantedPermissionRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            GrantedPermissionMapper mapper) {
        super(GrantedPermissionEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crearPermiso(GrantedPermission permiso) {
        this.save(toEntityConRole(permiso));
    }

    @Override
    public void actualizarPermiso(GrantedPermission permiso) throws EntityNotFoundException {
        if (findByIdAndCompany(permiso.getIdCompany(), permiso.getIdGrantedPermission()).isEmpty()) {
            throw new EntityNotFoundException(String.format("No se encontró el permiso con ID %d para actualizar.", permiso.getIdGrantedPermission()));
        }
        this.save(toEntityConRole(permiso));
    }

    /**
     * GrantedPermissionMapper#domainToEntity ignora roleEntity a propósito: se invoca en
     * cascada desde RoleMapper#domainToEntity (Role.grantedPermissions -> grantedPermissionEntities)
     * y mapear el role de vuelta ahí formaría un ciclo infinito. Al guardar un GrantedPermission
     * suelto (fuera de esa cascada) hay que completar la referencia al rol a mano.
     */
    private GrantedPermissionEntity toEntityConRole(GrantedPermission permiso) {
        GrantedPermissionEntity entity = mapper.domainToEntity(permiso);
        entity.setRoleEntity(RoleEntity.builder().idRol(permiso.getRole().getIdRol()).build());
        return entity;
    }

    /**
     * GrantedPermissionMapper#entityToDomain ignora roleEntity a propósito (mismo motivo que
     * domainToEntity, ver arriba): si mapeara el Role completo vía RoleMapper, y RoleMapper mapea
     * de vuelta su Set&lt;GrantedPermission&gt;, entra en ciclo infinito. Acá se arma un Role
     * mínimo a mano (id + nombre, sin sus grantedPermissions) para que las pantallas de
     * administración puedan mostrar el nombre del rol sin ese riesgo.
     */
    private GrantedPermission entityToDomainConRole(GrantedPermissionEntity entity) {
        GrantedPermission permiso = mapper.entityToDomain(entity);
        RoleEntity roleEntity = entity.getRoleEntity();
        if (roleEntity != null) {
            permiso.setRole(Role.builder().idRol(roleEntity.getIdRol()).name(roleEntity.getName()).build());
        }
        return permiso;
    }

    @Override
    public GrantedPermission obtenerPermisoPorId(String idCompany, Long idGrantedPermission) throws EntityNotFoundException {
        return findByIdAndCompany(idCompany, idGrantedPermission)
                .map(this::entityToDomainConRole)
                .orElseThrow(() ->
                        new EntityNotFoundException("Permission not found ID " + idGrantedPermission)
                );
    }

    @Override
    public void eliminarPermiso(String idCompany, Long idGrantedPermission) throws EntityNotFoundException {
        GrantedPermissionEntity permissionEntity = findByIdAndCompany(idCompany, idGrantedPermission)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found ID " + idGrantedPermission));
        this.delete(permissionEntity);
    }

    @Override
    public List<GrantedPermission> listarPermisosPorRol(String idCompany, Long idRol) {
        requireCompany(idCompany);
        List<GrantedPermissionEntity> list = getQueryFactory()
                .selectFrom(grantedPermissionEntity)
                .where(grantedPermissionEntity.idCompany.eq(idCompany)
                        .and(grantedPermissionEntity.roleEntity.idRol.eq(idRol)))
                .fetch();

        return list.stream().map(this::entityToDomainConRole).toList();
    }

    @Override
    public PagedResult<GrantedPermission> listarPermisosPaginados(
            String idCompany,
            List<FilterCriterion> filters,
            PageQuery pageQuery) {

        Page<GrantedPermissionEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);

        return toPagedResult(result.map(this::entityToDomainConRole));
    }

    @Override
    public Optional<GrantedPermission> obtenerPorRolYOperacion(String idCompany, Long idRol, Long idOperation) {
        requireCompany(idCompany);
        GrantedPermissionEntity entity = getQueryFactory()
                .selectFrom(grantedPermissionEntity)
                .where(grantedPermissionEntity.idCompany.eq(idCompany)
                        .and(grantedPermissionEntity.roleEntity.idRol.eq(idRol))
                        .and(grantedPermissionEntity.operationEntity.idOperation.eq(idOperation)))
                .fetchFirst();
        return Optional.ofNullable(entity).map(this::entityToDomainConRole);
    }

    @Override
    public boolean existsByOperation(String idCompany, Long idOperation) {
        requireCompany(idCompany);
        return getQueryFactory()
                .selectOne()
                .from(grantedPermissionEntity)
                .where(grantedPermissionEntity.idCompany.eq(idCompany)
                        .and(grantedPermissionEntity.operationEntity.idOperation.eq(idOperation)))
                .fetchFirst() != null;
    }

}
