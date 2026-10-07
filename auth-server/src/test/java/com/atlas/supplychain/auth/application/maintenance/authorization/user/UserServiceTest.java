package com.atlas.supplychain.auth.application.maintenance.authorization.user;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private AuditTrail auditTrail;
    @Mock
    private CurrentActor currentActor;
    @Mock
    private OutboxRepository outboxRepository;

    private UserServiceImpl service() {
        return new UserServiceImpl(repository, auditTrail, currentActor, outboxRepository);
    }

    @Test
    void crearUsuarioDelegaEnElRepositorio() {
        UserServiceImpl service = service();
        User user = User.builder().idUser("u1").idCompany("c1").name("Ana").lastName("Perez").build();

        service.crearUsuario(user);

        verify(repository).crearUsuario(user);
    }

    @Test
    void crearUsuarioPublicaOutboxUsuarioCreadoConElSnapshotCompleto() {
        UserServiceImpl service = service();
        User user = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez")
                .email("ana@atlas.local")
                .role(Role.builder().idRol(5L).build())
                .state(true)
                .build();

        service.crearUsuario(user);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.aggregateType()).isEqualTo("USER");
        assertThat(evento.aggregateId()).isEqualTo("u1");
        assertThat(evento.eventType()).isEqualTo("USUARIO_CREADO");
        assertThat(evento.payload())
                .containsEntry("idUser", "u1")
                .containsEntry("idCompany", "c1")
                .containsEntry("name", "Ana")
                .containsEntry("lastName", "Perez")
                .containsEntry("email", "ana@atlas.local")
                .containsEntry("idRole", "5")
                .containsEntry("state", "true");
    }

    @Test
    void actualizarUsuarioPreservaCamposSensiblesQueNoViajanPorElDtoYDelegaEnElRepositorio() throws EntityNotFoundException {
        UserServiceImpl service = service();

        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .passwordHash("hash-existente")
                .passwordChangedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .failedAttempts(3)
                .lockUntil(Instant.parse("2026-01-02T00:00:00Z"))
                .passwordExpiresAt(Instant.parse("2026-06-01T00:00:00Z"))
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder().idUser("u1").idCompany("c1").name("Nuevo").build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).actualizarUsuario(captor.capture());
        User guardado = captor.getValue();
        assertThat(guardado.getName()).isEqualTo("Nuevo");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-existente");
        assertThat(guardado.getFailedAttempts()).isEqualTo(3);
        assertThat(guardado.getLockUntil()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
        assertThat(guardado.getPasswordExpiresAt()).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test
    void actualizarUsuarioPublicaOutboxUsuarioEmailCambiadoCuandoElEmailCambia() throws EntityNotFoundException {
        UserServiceImpl service = service();

        User existente = User.builder().idUser("u1").idCompany("c1").email("viejo@atlas.local").build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder().idUser("u1").idCompany("c1").email("nuevo@atlas.local").build();

        service.actualizarUsuario(cambios);

        // Spec 1.24: cuando cambia el email se publican AMBOS eventos en la misma transacción --
        // el USUARIO_EMAIL_CAMBIADO histórico (Spec 3.4, que ms-notificacion sigue consumiendo) y
        // el USUARIO_ACTUALIZADO nuevo (snapshot completo para el read model de ms-reporte).
        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository, org.mockito.Mockito.times(2)).append(captor.capture());
        List<OutboxEventCommand> eventos = captor.getAllValues();

        OutboxEventCommand emailCambiado = eventos.stream()
                .filter(e -> e.eventType().equals("USUARIO_EMAIL_CAMBIADO"))
                .findFirst().orElseThrow();
        assertThat(emailCambiado.aggregateType()).isEqualTo("USER");
        assertThat(emailCambiado.aggregateId()).isEqualTo("u1");
        assertThat(emailCambiado.payload()).containsEntry("idUser", "u1").containsEntry("email", "nuevo@atlas.local");

        OutboxEventCommand usuarioActualizado = eventos.stream()
                .filter(e -> e.eventType().equals("USUARIO_ACTUALIZADO"))
                .findFirst().orElseThrow();
        assertThat(usuarioActualizado.aggregateType()).isEqualTo("USER");
        assertThat(usuarioActualizado.aggregateId()).isEqualTo("u1");
        assertThat(usuarioActualizado.payload()).containsEntry("email", "nuevo@atlas.local");
    }

    @Test
    void actualizarUsuarioNoPublicaOutboxCuandoElEmailNoCambia() throws EntityNotFoundException {
        UserServiceImpl service = service();

        User existente = User.builder().idUser("u1").idCompany("c1").email("mismo@atlas.local").build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder().idUser("u1").idCompany("c1").email("mismo@atlas.local").build();

        service.actualizarUsuario(cambios);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void actualizarUsuarioPublicaOutboxUsuarioActualizadoCuandoCambiaElName() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Viejo").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Nuevo").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.eventType()).isEqualTo("USUARIO_ACTUALIZADO");
        assertThat(evento.aggregateId()).isEqualTo("u1");
        assertThat(evento.payload()).containsEntry("name", "Nuevo").containsEntry("lastName", "Perez");
    }

    @Test
    void actualizarUsuarioPublicaOutboxUsuarioActualizadoCuandoCambiaElLastName() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Viejo").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Nuevo").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.eventType()).isEqualTo("USUARIO_ACTUALIZADO");
        assertThat(evento.payload()).containsEntry("lastName", "Nuevo");
    }

    @Test
    void actualizarUsuarioPublicaOutboxUsuarioActualizadoCuandoCambiaElRol() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(2L).build()).state(true)
                .build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.eventType()).isEqualTo("USUARIO_ACTUALIZADO");
        assertThat(evento.payload()).containsEntry("idRole", "2");
    }

    @Test
    void actualizarUsuarioPublicaOutboxUsuarioActualizadoCuandoCambiaElState() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(false)
                .build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.eventType()).isEqualTo("USUARIO_ACTUALIZADO");
        assertThat(evento.payload()).containsEntry("state", "false");
    }

    @Test
    void actualizarUsuarioPublicaAmbosEventosCuandoCambiaElEmail() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("viejo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("nuevo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository, org.mockito.Mockito.times(2)).append(captor.capture());
        List<String> tiposPublicados = captor.getAllValues().stream().map(OutboxEventCommand::eventType).toList();
        assertThat(tiposPublicados).containsExactlyInAnyOrder("USUARIO_EMAIL_CAMBIADO", "USUARIO_ACTUALIZADO");
    }

    @Test
    void actualizarUsuarioNoPublicaUsuarioActualizadoSiNingunCampoRelevanteCambia() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User existente = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder()
                .idUser("u1").idCompany("c1")
                .name("Ana").lastName("Perez").email("mismo@atlas.local")
                .role(Role.builder().idRol(1L).build()).state(true)
                .build();

        service.actualizarUsuario(cambios);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void actualizarUsuarioLanzaEntityNotFoundExceptionSiElUsuarioNoExiste() {
        UserServiceImpl service = service();
        User user = User.builder().idUser("inexistente").idCompany("c1").build();
        when(repository.obtenerUsuarioPorId("c1", "inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarUsuario(user)).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(auditTrail);
    }

    @Test
    void actualizarUsuarioRegistraAuditlogDeCambioDeRolCuandoElRolCambia() throws EntityNotFoundException {
        UserServiceImpl service = service();

        Role rolViejo = Role.builder().idRol(1L).build();
        Role rolNuevo = Role.builder().idRol(2L).build();
        User existente = User.builder().idUser("u1").idCompany("c1").role(rolViejo).build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        Actor actor = new Actor("admin1", "c1", "admin1", "ADMIN");
        when(currentActor.actorActual()).thenReturn(actor);

        User cambios = User.builder().idUser("u1").idCompany("c1").role(rolNuevo).build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("UPDATE");
        assertThat(cmd.accionNombre()).isEqualTo("CAMBIO_ROL_USUARIO");
        assertThat(cmd.entidadTipo()).isEqualTo("USUARIO");
        assertThat(cmd.entidadId()).isEqualTo("u1");
        assertThat(cmd.modulo()).isEqualTo("ADMINISTRACION");
        assertThat(cmd.severity()).isEqualTo("HIGH");
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isEqualTo("admin1");
        assertThat(cmd.username()).isEqualTo("admin1");
        assertThat(cmd.rolPrincipal()).isEqualTo("ADMIN");
        assertThat(cmd.resumen()).isEqualTo("Cambio de rol del usuario u1");
    }

    @Test
    void actualizarUsuarioRegistraAuditlogConFallbackDeCompanyIdCuandoNoHayActor() throws EntityNotFoundException {
        UserServiceImpl service = service();

        Role rolViejo = Role.builder().idRol(1L).build();
        Role rolNuevo = Role.builder().idRol(2L).build();
        User existente = User.builder().idUser("u1").idCompany("c1").role(rolViejo).build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));
        when(currentActor.actorActual()).thenReturn(null);

        User cambios = User.builder().idUser("u1").idCompany("c1").role(rolNuevo).build();

        service.actualizarUsuario(cambios);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isNull();
        assertThat(cmd.username()).isNull();
        assertThat(cmd.rolPrincipal()).isNull();
    }

    @Test
    void actualizarUsuarioNoRegistraAuditlogCuandoElRolNoCambia() throws EntityNotFoundException {
        UserServiceImpl service = service();

        Role rol = Role.builder().idRol(1L).build();
        User existente = User.builder().idUser("u1").idCompany("c1").role(rol).build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(existente));

        User cambios = User.builder().idUser("u1").idCompany("c1").role(Role.builder().idRol(1L).build()).build();

        service.actualizarUsuario(cambios);

        verify(auditTrail, never()).registrar(any());
    }

    @Test
    void eliminarUsuarioDelegaEnElRepositorioConIdCompanyEIdUsuarioEnOrdenDelPuerto() throws EntityNotFoundException {
        UserServiceImpl service = service();

        service.eliminarUsuario("u1", "c1");

        verify(repository).eliminarUsuario("c1", "u1");
    }

    @Test
    void eliminarUsuarioPropagaEntityNotFoundExceptionDelRepositorio() throws EntityNotFoundException {
        UserServiceImpl service = service();
        org.mockito.Mockito.doThrow(new EntityNotFoundException("no existe")).when(repository).eliminarUsuario("c1", "u1");

        assertThatThrownBy(() -> service.eliminarUsuario("u1", "c1")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminarUsuarioPublicaOutboxUsuarioEliminado() throws EntityNotFoundException {
        UserServiceImpl service = service();

        service.eliminarUsuario("u1", "c1");

        ArgumentCaptor<OutboxEventCommand> captor = ArgumentCaptor.forClass(OutboxEventCommand.class);
        verify(outboxRepository).append(captor.capture());
        OutboxEventCommand evento = captor.getValue();
        assertThat(evento.aggregateType()).isEqualTo("USER");
        assertThat(evento.aggregateId()).isEqualTo("u1");
        assertThat(evento.eventType()).isEqualTo("USUARIO_ELIMINADO");
        assertThat(evento.payload()).containsEntry("idUser", "u1").containsEntry("idCompany", "c1");
    }

    @Test
    void eliminarUsuarioNoPublicaOutboxSiElRepositorioLanzaExcepcion() throws EntityNotFoundException {
        UserServiceImpl service = service();
        org.mockito.Mockito.doThrow(new EntityNotFoundException("no existe")).when(repository).eliminarUsuario("c1", "u1");

        assertThatThrownBy(() -> service.eliminarUsuario("u1", "c1")).isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void obtenerUsuarioPorIdDevuelveElUsuarioSiExiste() throws EntityNotFoundException {
        UserServiceImpl service = service();
        User user = User.builder().idUser("u1").idCompany("c1").build();
        when(repository.obtenerUsuarioPorId("c1", "u1")).thenReturn(Optional.of(user));

        assertThat(service.obtenerUsuarioPorId("u1", "c1")).isEqualTo(user);
    }

    @Test
    void obtenerUsuarioPorIdLanzaEntityNotFoundExceptionSiNoExiste() {
        UserServiceImpl service = service();
        when(repository.obtenerUsuarioPorId("c1", "desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerUsuarioPorId("desconocido", "c1"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listarUsuariosPorEmpresaDelegaEnElRepositorio() {
        UserServiceImpl service = service();
        User user = User.builder().idUser("u1").idCompany("c1").build();
        when(repository.listarUsuariosPorCompany("c1")).thenReturn(List.of(user));

        assertThat(service.listarUsuariosPorEmpresa("c1")).containsExactly(user);
    }

    @Test
    void buscarUsuariosPaginadosDelegaEnElRepositorioConFiltrosPageQueryYCompany() {
        UserServiceImpl service = service();
        User user = User.builder().idUser("u1").idCompany("c1").build();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<User> expected = new PagedResult<>(List.of(user), 0, 10, 1);
        when(repository.listarUsuariosPaginadosPorFiltros("c1", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service.buscarUsuariosPaginados(List.of(), pageQuery, "c1")).isEqualTo(expected);
    }
}
