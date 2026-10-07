package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db;


import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QBusinessProcessEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QCompanyProcessEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QGrantedPermissionEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QModuleEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QOperationEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QRoleEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.QUserEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.UserEntity;
import com.atlas.supplychain.auth.infrastructure.common.db.AbstractQueryDslRepository;
import com.atlas.supplychain.auth.infrastructure.common.db.query.predicate.PredicateBuilderRegistry;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.UserMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
@Slf4j
public class UserRepositoryImpl
        extends AbstractQueryDslRepository<UserEntity, Long>
        implements UserRepository {

    private final UserMapper mapper;

    public UserRepositoryImpl(
            EntityManager entityManager,
            PredicateBuilderRegistry predicateRegistry,
            UserMapper mapper
    ) {
        super(UserEntity.class, entityManager, predicateRegistry);
        this.mapper = mapper;
    }

    @Override
    public void crearUsuario(User user) {
        // id_user es bigint IDENTITY (ya no lo inventa el cliente ni este código): se ignora
        // cualquier valor que venga puesto (ej. un DTO viejo que todavia mande un idUser a mano),
        // se persiste sin id, y el valor generado por Postgres se escribe de vuelta sobre el User
        // de dominio para que registrarUsuarioCreado (Outbox, llamado justo después en
        // UserServiceImpl) tenga el id real, no null.
        user.setIdUser(null);
        UserEntity saved = this.save(mapper.domainToEntity(user));
        user.setIdUser(String.valueOf(saved.getIdUser()));
    }

    @Override
    public void actualizarUsuario(User user) throws EntityNotFoundException {
        Long id = parseIdUsuario(user.getIdUser());
        if (id == null) {
            throw new EntityNotFoundException("Usuario no encontrado ID " + user.getIdUser());
        }
        UserEntity existente = findByIdAndCompany(user.getIdCompany(), id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado ID " + user.getIdUser()));
        UserEntity nuevo = mapper.domainToEntity(user);
        if (nuevo.getUsername() == null) {
            // Actualizaciones parciales (ej. solo cambia el email) arman un User de dominio sin
            // username -- domainToEntity mapea ese null tal cual y pisaria la columna NOT NULL.
            // Se preserva el username existente cuando el llamador no trae uno propio.
            nuevo.setUsername(existente.getUsername());
        }
        this.save(nuevo);
    }

    @Override
    public Optional<User> obtenerUsuarioPorId(String idCompany, String idUsuario) {
        Long id = parseIdUsuario(idUsuario);
        if (id == null) {
            return Optional.empty();
        }
        return findByIdAndCompany(idCompany, id)
                .map(mapper::entityToDomain);
    }

    /**
     * id_user es bigint: un identificador que llega desde afuera (URL, formulario de
     * forgot-password) puede perfectamente no ser numérico -- eso significa "no existe", no un
     * error 500. {@code null} si {@code idUsuario} no es un long válido.
     */
    private static Long parseIdUsuario(String idUsuario) {
        try {
            return Long.parseLong(idUsuario);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return getQueryFactory()
                .selectOne()
                .from(user())
                .where(user().username.eq(username))
                .fetchFirst() != null;
    }

    /**
     * Sin filtro de empresa -- úsese solo donde no hay tenant disponible todavía (login,
     * vía {@code CustomUserDetailsService}, que recibe un único {@code String} del contrato
     * de {@code UserDetailsService}). Cualquier caso de uso que sí conozca el {@code idCompany}
     * del solicitante debe usar el overload de dos argumentos, no este.
     */
    @Override
    public Optional<User> findByIdUsuarioWithAuthorities(String idUsuario) {
        Long id = parseIdUsuario(idUsuario);
        if (id == null) {
            return Optional.empty();
        }
        UserEntity result = queryConAutoridades()
                .where(
                        user().idUser.eq(id)
                                .and(user().state.isTrue())
                )
                .fetchOne();

        return Optional.ofNullable(conProcesosResueltos(mapper.entityToDomain(result)));
    }

    @Override
    public Optional<User> findByUsernameWithAuthorities(String username) {
        UserEntity result = queryConAutoridades()
                .where(
                        user().username.eq(username)
                                .and(user().state.isTrue())
                )
                .fetchOne();

        return Optional.ofNullable(conProcesosResueltos(mapper.entityToDomain(result)));
    }

    @Override
    public Optional<User> findByIdUsuarioWithAuthorities(String idUsuario, String idCompany) {

        if (idCompany == null) {
            // id_company es NOT NULL en el esquema: un idCompany nulo (request anónimo sin
            // el campo) nunca puede coincidir con una fila real. Evita eq(null), que QueryDSL
            // rechaza, y mantiene el comportamiento correcto (ningún usuario encontrado).
            return Optional.empty();
        }

        Long id = parseIdUsuario(idUsuario);
        if (id == null) {
            return Optional.empty();
        }

        UserEntity result = queryConAutoridades()
                .where(
                        user().idUser.eq(id)
                                .and(user().idCompany.eq(idCompany))
                                .and(user().state.isTrue())
                )
                .fetchOne();

        return Optional.ofNullable(conProcesosResueltos(mapper.entityToDomain(result)));
    }

    private QUserEntity user() {
        return QUserEntity.userEntity;
    }

    /**
     * Marca los módulos cuyo proceso de negocio no está activo para la empresa (Spec 1.0), para que
     * {@code User.esVigente} les corte el acceso.
     *
     * <p>Se resuelve acá y no descartando esos módulos de la consulta: si un módulo desapareciera
     * del agregado, una operación que solo colgara de él pasaría a verse "sin módulo", y una
     * operación sin módulo **sí** autoriza -- desactivar un proceso terminaría dando acceso en vez
     * de quitarlo.</p>
     *
     * <p>Consulta el conjunto de procesos activos, no el de inactivos, porque un proceso que la
     * empresa nunca contrató tampoco tiene fila en {@code company_process} y también debe quedar
     * cortado. Si ningún módulo del usuario cuelga de un proceso -- el caso normal hoy -- no se
     * consulta nada.</p>
     */
    private User conProcesosResueltos(User usuario) {
        if (usuario == null || usuario.getRole() == null || usuario.getRole().getGrantedPermissions() == null) {
            return usuario;
        }

        List<Module> modulosConProceso = usuario.getRole().getGrantedPermissions().stream()
                .map(GrantedPermission::getOperation)
                .filter(Objects::nonNull)
                .map(Operation::getModules)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(module -> module.getIdBusinessProcess() != null)
                .toList();

        if (modulosConProceso.isEmpty()) {
            return usuario;
        }

        Set<Long> activos = procesosActivosDe(usuario.getIdCompany());
        modulosConProceso.forEach(module -> module.setProcessActive(activos.contains(module.getIdBusinessProcess())));
        return usuario;
    }

    /** Procesos del catálogo activos y contratados por la empresa. */
    private Set<Long> procesosActivosDe(String idCompany) {
        if (idCompany == null) {
            return Set.of();
        }
        QCompanyProcessEntity companyProcess = QCompanyProcessEntity.companyProcessEntity;
        QBusinessProcessEntity businessProcess = QBusinessProcessEntity.businessProcessEntity;

        return new HashSet<>(getQueryFactory()
                .select(companyProcess.idBusinessProcess)
                .from(companyProcess)
                .join(businessProcess).on(businessProcess.idBusinessProcess.eq(companyProcess.idBusinessProcess))
                .where(companyProcess.idCompany.eq(idCompany)
                        .and(companyProcess.active.isTrue())
                        .and(businessProcess.state.isTrue()))
                .fetch());
    }

    private com.querydsl.jpa.impl.JPAQuery<UserEntity> queryConAutoridades() {
        QRoleEntity role = QRoleEntity.roleEntity;
        QGrantedPermissionEntity gp = QGrantedPermissionEntity.grantedPermissionEntity;
        QOperationEntity op = QOperationEntity.operationEntity;
        QModuleEntity module = QModuleEntity.moduleEntity;

        // `op.modules` es la N:M de la Spec 1.2: leftJoin porque una operación puede no colgar de
        // ningún módulo (recién auto-registrada) y aun así autorizar su endpoint -- con innerJoin
        // el usuario perdería esa autoridad en silencio.
        return getQueryFactory()
                .selectFrom(user())
                .leftJoin(user().roleEntity, role).fetchJoin()
                .leftJoin(role.grantedPermissionEntities, gp).fetchJoin()
                .leftJoin(gp.operationEntity, op).fetchJoin()
                .leftJoin(op.modules, module).fetchJoin()
                .distinct();
    }


    @Override
    public void eliminarUsuario(String idCompany, String idUsuario) throws EntityNotFoundException {
        Long id = parseIdUsuario(idUsuario);
        if (id == null) {
            throw new EntityNotFoundException("Usuario no encontrado ID " + idUsuario);
        }
        UserEntity entity = findByIdAndCompany(idCompany, id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado ID " + idUsuario));
        this.delete(entity);
    }

    @Override
    public List<User> listarUsuariosPorCompany(String idCompany) {
        List<UserEntity> list = findAllByCompany(idCompany);
        return list.stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public PagedResult<User> listarUsuariosPaginadosPorFiltros(
            String idCompany,
            List<FilterCriterion> filters,
            PageQuery pageQuery) {

        Page<UserEntity> result = filterAndSortPaged(filters, pageQuery.sorts(), toPageable(pageQuery), idCompany);

        return toPagedResult(result.map(mapper::entityToDomain));
    }

    @Override
    public boolean existsByRole(String idCompany, Long idRol) {
        requireCompany(idCompany);
        QUserEntity user = QUserEntity.userEntity;
        return getQueryFactory()
                .selectOne()
                .from(user)
                .where(user.idCompany.eq(idCompany).and(user.roleEntity.idRol.eq(idRol)))
                .fetchFirst() != null;
    }
}
