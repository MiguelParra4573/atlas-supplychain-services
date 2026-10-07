package com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuario;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrantedPermissionServiceTest {

    @Mock
    private GrantedPermissionRepository repository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private PermisosVigentesDelUsuario permisosVigentesDelUsuario;
    @Mock
    private AuditTrail auditTrail;
    @Mock
    private CurrentActor currentActor;

    private GrantedPermissionServiceImpl service() {
        return new GrantedPermissionServiceImpl(repository, roleRepository, operationRepository,
                permisosVigentesDelUsuario, auditTrail, currentActor);
    }

    @Test
    void asignarPermisoARolCreaCuandoRolYOperacionExistenYNoHayDuplicado() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        Operation operation = Operation.builder().idCompany("c1").idOperation(2L).tag("CREAR").build();
        when(roleRepository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(operationRepository.obtenerOperacionPorId("c1", 2L)).thenReturn(operation);
        when(repository.obtenerPorRolYOperacion("c1", 1L, 2L)).thenReturn(Optional.empty());
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service.asignarPermisoARol("c1", 1L, 2L);

        ArgumentCaptor<GrantedPermission> captor = ArgumentCaptor.forClass(GrantedPermission.class);
        verify(repository).crearPermiso(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(role);
        assertThat(captor.getValue().getOperation()).isEqualTo(operation);
        assertThat(captor.getValue().getIdCompany()).isEqualTo("c1");
    }

    @Test
    void asignarPermisoARolRegistraEventoDeAuditlogConCompuestoRolOperacionComoEntidadId() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        Operation operation = Operation.builder().idCompany("c1").idOperation(2L).tag("CREAR").build();
        when(roleRepository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(operationRepository.obtenerOperacionPorId("c1", 2L)).thenReturn(operation);
        when(repository.obtenerPorRolYOperacion("c1", 1L, 2L)).thenReturn(Optional.empty());
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "SUPERADMIN"));

        service.asignarPermisoARol("c1", 1L, 2L);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("CREATE");
        assertThat(cmd.accionNombre()).isEqualTo("ASIGNAR_PERMISO");
        assertThat(cmd.entidadTipo()).isEqualTo("PERMISO");
        assertThat(cmd.entidadId()).isEqualTo("ROL:1:OPERACION:2");
        assertThat(cmd.modulo()).isEqualTo("ADMINISTRACION");
        assertThat(cmd.severity()).isEqualTo("HIGH");
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isEqualTo("u1");
        assertThat(cmd.username()).isEqualTo("u1");
        assertThat(cmd.rolPrincipal()).isEqualTo("SUPERADMIN");
        assertThat(cmd.resumen()).isEqualTo("Permiso concedido: rol 1 operacion 2");
    }

    @Test
    void asignarPermisoARolUsaIdCompanyDelParametroComoFallbackSiNoHayActorEnContexto() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        Operation operation = Operation.builder().idCompany("c1").idOperation(2L).tag("CREAR").build();
        when(roleRepository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(operationRepository.obtenerOperacionPorId("c1", 2L)).thenReturn(operation);
        when(repository.obtenerPorRolYOperacion("c1", 1L, 2L)).thenReturn(Optional.empty());
        when(currentActor.actorActual()).thenReturn(null);

        service.asignarPermisoARol("c1", 1L, 2L);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isNull();
        assertThat(cmd.username()).isNull();
        assertThat(cmd.rolPrincipal()).isNull();
    }

    @Test
    void asignarPermisoARolLanzaEntityNotFoundExceptionSiElRolNoExiste() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        when(roleRepository.obtenerRolPorId("c1", 1L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service.asignarPermisoARol("c1", 1L, 2L)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).crearPermiso(any());
    }

    @Test
    void asignarPermisoARolLanzaEntityNotFoundExceptionSiLaOperacionNoExiste() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        when(roleRepository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(operationRepository.obtenerOperacionPorId("c1", 2L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service.asignarPermisoARol("c1", 1L, 2L)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).crearPermiso(any());
    }

    @Test
    void asignarPermisoARolLanzaDuplicateRecordExceptionSiElParYaEstaConcedido() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        Role role = Role.builder().idCompany("c1").idRol(1L).name("ADMIN").build();
        Operation operation = Operation.builder().idCompany("c1").idOperation(2L).tag("CREAR").build();
        when(roleRepository.obtenerRolPorId("c1", 1L)).thenReturn(role);
        when(operationRepository.obtenerOperacionPorId("c1", 2L)).thenReturn(operation);
        when(repository.obtenerPorRolYOperacion("c1", 1L, 2L))
                .thenReturn(Optional.of(GrantedPermission.builder().idGrantedPermission(9L).build()));

        assertThatThrownBy(() -> service.asignarPermisoARol("c1", 1L, 2L)).isInstanceOf(DuplicateRecordException.class);

        verify(repository, never()).crearPermiso(any());
    }

    @Test
    void revocarPermisoValidaExistenciaYDelegaEnElRepositorio() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        when(repository.obtenerPermisoPorId("c1", 5L)).thenReturn(GrantedPermission.builder().idGrantedPermission(5L).build());
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service.revocarPermiso("c1", 5L);

        verify(repository).eliminarPermiso("c1", 5L);
    }

    @Test
    void revocarPermisoRegistraEventoDeAuditlogConSeveridadAlta() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        when(repository.obtenerPermisoPorId("c1", 5L)).thenReturn(GrantedPermission.builder().idGrantedPermission(5L).build());
        when(currentActor.actorActual()).thenReturn(new Actor("u1", "c1", "u1", "ADMIN"));

        service.revocarPermiso("c1", 5L);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("DELETE");
        assertThat(cmd.accionNombre()).isEqualTo("REVOCAR_PERMISO");
        assertThat(cmd.entidadTipo()).isEqualTo("PERMISO");
        assertThat(cmd.entidadId()).isEqualTo("5");
        assertThat(cmd.severity()).isEqualTo("HIGH");
        assertThat(cmd.resumen()).isEqualTo("Permiso revocado ID 5");
    }

    @Test
    void revocarPermisoPropagaEntityNotFoundExceptionSiNoExiste() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        when(repository.obtenerPermisoPorId("c1", 5L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service.revocarPermiso("c1", 5L)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).eliminarPermiso("c1", 5L);
    }

    /**
     * Spec 1.2: se apoya en {@code autoridades} y no en {@code obtener} -- derivar las autoridades
     * del mapa agrupado por módulo perdería en silencio toda operación sin módulo asignado, que es
     * el estado normal de una operación recién auto-registrada.
     */
    @Test
    void obtenerPermisosEfectivosDelegaEnLaVistaDeAutoridadesDelColaborador() {
        GrantedPermissionServiceImpl service = service();

        when(permisosVigentesDelUsuario.autoridades("u1", "c1")).thenReturn(Set.of("USUARIOS:CREAR"));

        assertThat(service.obtenerPermisosEfectivos("u1", "c1")).containsExactly("USUARIOS:CREAR");
    }

    @Test
    void obtenerPermisosEfectivosDevuelveConjuntoVacioSiElColaboradorNoDevuelvePermisos() {
        GrantedPermissionServiceImpl service = service();
        when(permisosVigentesDelUsuario.autoridades("u1", "c1")).thenReturn(Set.of());

        assertThat(service.obtenerPermisosEfectivos("u1", "c1")).isEmpty();
    }

    @Test
    void obtenerPermisoPorIdDelegaEnElRepositorio() throws EntityNotFoundException {
        GrantedPermissionServiceImpl service = service();
        GrantedPermission permiso = GrantedPermission.builder().idCompany("c1").idGrantedPermission(1L).build();
        when(repository.obtenerPermisoPorId("c1", 1L)).thenReturn(permiso);

        assertThat(service.obtenerPermisoPorId("c1", 1L)).isEqualTo(permiso);
    }

    @Test
    void listarPermisosPorRolDelegaEnElRepositorio() {
        GrantedPermissionServiceImpl service = service();
        GrantedPermission permiso = GrantedPermission.builder().idCompany("c1").idGrantedPermission(1L).build();
        when(repository.listarPermisosPorRol("c1", 1L)).thenReturn(List.of(permiso));

        assertThat(service.listarPermisosPorRol("c1", 1L)).containsExactly(permiso);
    }

    @Test
    void listarPermisosPaginadosDelegaEnElRepositorio() {
        GrantedPermissionServiceImpl service = service();
        var pageQuery = new com.atlas.supplychain.auth.domain.shared.PageQuery(0, 10, List.of());
        var expected = new com.atlas.supplychain.auth.domain.shared.PagedResult<GrantedPermission>(List.of(), 0, 10, 0);
        when(repository.listarPermisosPaginados("c1", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service.listarPermisosPaginados("c1", List.of(), pageQuery)).isEqualTo(expected);
    }
}
