package com.atlas.supplychain.auth.application.maintenance.authorization.role;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository repository;
    @Mock
    private GrantedPermissionRepository grantedPermissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditTrail auditTrail;
    @Mock
    private CurrentActor currentActor;
    @Mock
    private OutboxRepository outboxRepository;

    private RoleServiceImpl service() {
        return new RoleServiceImpl(repository, grantedPermissionRepository, userRepository, auditTrail, currentActor,
                outboxRepository);
    }

    @Test
    void crearRolCreaCuandoNoExisteRolConMismoNombreEnLaEmpresa() {
        Role role = Role.builder().idCompany("c1").name("ADMIN").build();
        when(repository.existsByNameAndCompany("c1", "ADMIN")).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().crearRol(role);

        verify(repository).crearRol(role);
    }

    @Test
    void crearRolRegistraEventoDeAuditlogConDatosDelActor() {
        Role role = Role.builder().idCompany("c1").name("ADMIN").build();
        when(repository.existsByNameAndCompany("c1", "ADMIN")).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "SUPERADMIN"));

        service().crearRol(role);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("CREATE");
        assertThat(cmd.accionNombre()).isEqualTo("CREAR_ROL");
        assertThat(cmd.entidadTipo()).isEqualTo("ROL");
        assertThat(cmd.entidadId()).isEqualTo("ADMIN");
        assertThat(cmd.modulo()).isEqualTo("ADMINISTRACION");
        assertThat(cmd.severity()).isEqualTo("MEDIUM");
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isEqualTo("u1");
        assertThat(cmd.username()).isEqualTo("u1");
        assertThat(cmd.rolPrincipal()).isEqualTo("SUPERADMIN");
        assertThat(cmd.resumen()).isEqualTo("Rol creado: ADMIN");
    }

    @Test
    void crearRolUsaIdCompanyDeLaEntidadComoFallbackSiNoHayActorEnContexto() {
        Role role = Role.builder().idCompany("c1").name("ADMIN").build();
        when(repository.existsByNameAndCompany("c1", "ADMIN")).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(null);

        service().crearRol(role);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isNull();
        assertThat(cmd.username()).isNull();
        assertThat(cmd.rolPrincipal()).isNull();
    }

    @Test
    void crearRolPublicaOutboxRolCreadoConElSnapshotDelRol() {
        // El idRol ya viene poblado en el objeto de dominio porque, en producción,
        // RoleRepositoryImpl#crearRol lo propaga de vuelta tras el INSERT (Spec 1.24) --
        // acá se simula ese efecto de lado ya aplicado, dado que repository es un mock.
        Role role = Role.builder().idCompany("c1").idRol(10L).name("ADMIN").state(true).build();
        when(repository.existsByNameAndCompany("c1", "ADMIN")).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().crearRol(role);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.aggregateType()).isEqualTo("ROLE");
        assertThat(evento.aggregateId()).isEqualTo("10");
        assertThat(evento.eventType()).isEqualTo("ROL_CREADO");
        assertThat(evento.payload())
                .containsEntry("idRole", "10")
                .containsEntry("idCompany", "c1")
                .containsEntry("name", "ADMIN")
                .containsEntry("state", "true");
    }

    @Test
    void crearRolLanzaDuplicateRecordExceptionSiYaExisteRolConMismoNombreEnLaEmpresa() {
        Role role = Role.builder().idCompany("c1").name("ADMIN").build();
        when(repository.existsByNameAndCompany("c1", "ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service().crearRol(role)).isInstanceOf(DuplicateRecordException.class);

        verify(repository, never()).crearRol(role);
    }

    @Test
    void actualizarRolVerificaExistenciaYPertenenciaAlTenantAntesDeEscribir() throws EntityNotFoundException {
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        when(repository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().actualizarRol(role);

        verify(repository).obtenerRolPorId("c1", 1L);
        verify(repository).actualizarRol(role);
    }

    @Test
    void actualizarRolRegistraEventoDeAuditlogConIdDelRol() throws EntityNotFoundException {
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        when(repository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().actualizarRol(role);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("UPDATE");
        assertThat(cmd.accionNombre()).isEqualTo("ACTUALIZAR_ROL");
        assertThat(cmd.entidadTipo()).isEqualTo("ROL");
        assertThat(cmd.entidadId()).isEqualTo("1");
        assertThat(cmd.severity()).isEqualTo("MEDIUM");
        assertThat(cmd.resumen()).isEqualTo("Rol actualizado: ADMIN");
    }

    @Test
    void actualizarRolPublicaOutboxRolActualizadoConElSnapshotDelRol() throws EntityNotFoundException {
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN-2").state(false).build();
        when(repository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().actualizarRol(role);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.aggregateType()).isEqualTo("ROLE");
        assertThat(evento.aggregateId()).isEqualTo("1");
        assertThat(evento.eventType()).isEqualTo("ROL_ACTUALIZADO");
        assertThat(evento.payload())
                .containsEntry("idRole", "1")
                .containsEntry("idCompany", "c1")
                .containsEntry("name", "ADMIN-2")
                .containsEntry("state", "false");
    }

    @Test
    void actualizarRolPropagaEntityNotFoundExceptionSiElRolNoExisteEnLaEmpresa() throws EntityNotFoundException {
        Role role = Role.builder().idCompany("c1").idRol(99L).name("ADMIN").build();
        when(repository.obtenerRolPorId("c1", 99L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service().actualizarRol(role)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).actualizarRol(role);
    }

    @Test
    void eliminarRolEliminaCuandoNoHayDependencias() throws EntityNotFoundException {
        when(grantedPermissionRepository.listarPermisosPorRol("c1", 1L)).thenReturn(List.of());
        when(userRepository.existsByRole("c1", 1L)).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().eliminarRol("c1", 1L);

        verify(repository).eliminarRol("c1", 1L);
    }

    @Test
    void eliminarRolRegistraEventoDeAuditlogConSeveridadAlta() throws EntityNotFoundException {
        when(grantedPermissionRepository.listarPermisosPorRol("c1", 1L)).thenReturn(List.of());
        when(userRepository.existsByRole("c1", 1L)).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().eliminarRol("c1", 1L);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("DELETE");
        assertThat(cmd.accionNombre()).isEqualTo("ELIMINAR_ROL");
        assertThat(cmd.entidadTipo()).isEqualTo("ROL");
        assertThat(cmd.entidadId()).isEqualTo("1");
        assertThat(cmd.severity()).isEqualTo("HIGH");
        assertThat(cmd.resumen()).isEqualTo("Rol eliminado ID 1");
    }

    @Test
    void eliminarRolPublicaOutboxRolEliminadoCuandoNoHayDependencias() throws EntityNotFoundException {
        when(grantedPermissionRepository.listarPermisosPorRol("c1", 1L)).thenReturn(List.of());
        when(userRepository.existsByRole("c1", 1L)).thenReturn(false);
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service().eliminarRol("c1", 1L);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.aggregateType()).isEqualTo("ROLE");
        assertThat(evento.aggregateId()).isEqualTo("1");
        assertThat(evento.eventType()).isEqualTo("ROL_ELIMINADO");
        assertThat(evento.payload()).containsEntry("idRole", "1").containsEntry("idCompany", "c1");
    }

    @Test
    void eliminarRolNoPublicaOutboxSiTienePermisosConcedidosAsociados() throws EntityNotFoundException {
        when(grantedPermissionRepository.listarPermisosPorRol("c1", 1L))
                .thenReturn(List.of(GrantedPermission.builder().idGrantedPermission(1L).build()));

        assertThatThrownBy(() -> service().eliminarRol("c1", 1L)).isInstanceOf(ExistingDependenciesException.class);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void eliminarRolLanzaExistingDependenciesExceptionSiTienePermisosConcedidosAsociados() throws EntityNotFoundException {
        when(grantedPermissionRepository.listarPermisosPorRol("c1", 1L))
                .thenReturn(List.of(GrantedPermission.builder().idGrantedPermission(1L).build()));

        assertThatThrownBy(() -> service().eliminarRol("c1", 1L)).isInstanceOf(ExistingDependenciesException.class);

        verify(repository, never()).eliminarRol("c1", 1L);
    }

    @Test
    void eliminarRolLanzaExistingDependenciesExceptionSiTieneUsuariosAsignados() throws EntityNotFoundException {
        when(grantedPermissionRepository.listarPermisosPorRol("c1", 1L)).thenReturn(List.of());
        when(userRepository.existsByRole("c1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service().eliminarRol("c1", 1L)).isInstanceOf(ExistingDependenciesException.class);

        verify(repository, never()).eliminarRol("c1", 1L);
    }

    @Test
    void obtenerRolPorIdDelegaEnElRepositorio() throws EntityNotFoundException {
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        when(repository.obtenerRolPorId("c1", 1L)).thenReturn(role);

        assertThat(service().obtenerRolPorId("c1", 1L)).isEqualTo(role);
    }

    @Test
    void listarRolesPorCompanyDelegaEnElRepositorio() {
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        when(repository.listarRolesPorCompany("c1")).thenReturn(List.of(role));

        assertThat(service().listarRolesPorCompany("c1")).containsExactly(role);
    }

    @Test
    void listarRolesPaginadosDelegaEnElRepositorioConFiltrosPageQueryYCompany() {
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<Role> expected = new PagedResult<>(List.of(role), 0, 10, 1);
        when(repository.listarRolesPaginados("c1", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service().listarRolesPaginados("c1", List.of(), pageQuery)).isEqualTo(expected);
    }
}
