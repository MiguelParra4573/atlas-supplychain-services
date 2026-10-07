package com.atlas.supplychain.auth.infrastructure.config.security.listener;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.infrastructure.config.security.AccountLockoutProperties;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spec 2.4: login fallido y bloqueo por intentos disparan auditlog via {@link AuditTrail}.
 * onSuccess no audita nada -- el LOGIN_EXITOSO ya se dispara en AuthenticationService.login().
 */
@ExtendWith(MockitoExtension.class)
class AuthEventsListenerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditTrail auditTrail;

    private AuthEventsListener listener() {
        return new AuthEventsListener(userRepository, auditTrail, new AccountLockoutProperties());
    }

    private static User usuario(String idUser, String idCompany, int failedAttempts) {
        User user = new User();
        user.setIdUser(idUser);
        user.setIdCompany(idCompany);
        user.setFailedAttempts(failedAttempts);
        user.setState(true);
        return user;
    }

    @Test
    void onBadCredentialsConUsuarioExistenteRegistraLoginFallidoConCompanyIdDelUsuario() {
        User user = usuario("u1", "c1", 1);
        when(userRepository.findByUsernameWithAuthorities("u1")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken("u1", "clave-incorrecta");
        listener().onBadCredentials(new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("bad creds")));

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();

        assertThat(cmd.accionTipo()).isEqualTo("LOGIN");
        assertThat(cmd.accionNombre()).isEqualTo("LOGIN_FALLIDO");
        assertThat(cmd.entidadTipo()).isEqualTo("USUARIO");
        assertThat(cmd.entidadId()).isEqualTo("u1");
        assertThat(cmd.modulo()).isEqualTo("SEGURIDAD");
        assertThat(cmd.severity()).isEqualTo("MEDIUM");
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isEqualTo("u1");
        assertThat(cmd.username()).isEqualTo("u1");
        assertThat(cmd.rolPrincipal()).isNull();
        assertThat(cmd.resumen()).isEqualTo("Inicio de sesión fallido");
    }

    @Test
    void onBadCredentialsConUsuarioInexistenteRegistraLoginFallidoConCompanyIdUnknown() {
        when(userRepository.findByUsernameWithAuthorities("fantasma")).thenReturn(Optional.empty());

        Authentication authentication = new UsernamePasswordAuthenticationToken("fantasma", "clave");
        listener().onBadCredentials(new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("bad creds")));

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();

        assertThat(cmd.accionNombre()).isEqualTo("LOGIN_FALLIDO");
        assertThat(cmd.entidadId()).isEqualTo("fantasma");
        assertThat(cmd.companyId()).isEqualTo("UNKNOWN");
        assertThat(cmd.userId()).isEqualTo("fantasma");
        assertThat(cmd.username()).isEqualTo("fantasma");
    }

    @Test
    void onBadCredentialsAlAlcanzarMaxAttemptsRegistraAdemasUsuarioBloqueado() {
        User user = usuario("u1", "c1", 4);
        when(userRepository.findByUsernameWithAuthorities("u1")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken("u1", "clave-incorrecta");
        listener().onBadCredentials(new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("bad creds")));

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail, times(2)).registrar(captor.capture());

        AuditEventCommand loginFallido = captor.getAllValues().get(0);
        assertThat(loginFallido.accionNombre()).isEqualTo("LOGIN_FALLIDO");

        AuditEventCommand bloqueo = captor.getAllValues().get(1);
        assertThat(bloqueo.accionTipo()).isEqualTo("LOGIN");
        assertThat(bloqueo.accionNombre()).isEqualTo("USUARIO_BLOQUEADO");
        assertThat(bloqueo.entidadTipo()).isEqualTo("USUARIO");
        assertThat(bloqueo.entidadId()).isEqualTo("u1");
        assertThat(bloqueo.modulo()).isEqualTo("SEGURIDAD");
        assertThat(bloqueo.severity()).isEqualTo("HIGH");
        assertThat(bloqueo.companyId()).isEqualTo("c1");
        assertThat(bloqueo.userId()).isEqualTo("u1");
        assertThat(bloqueo.username()).isEqualTo("u1");
        assertThat(bloqueo.rolPrincipal()).isNull();
        assertThat(bloqueo.resumen()).isEqualTo("Usuario bloqueado por exceso de intentos fallidos");
    }

    @Test
    void onBadCredentialsPorDebajoDelMaximoNoRegistraBloqueo() {
        User user = usuario("u1", "c1", 1);
        when(userRepository.findByUsernameWithAuthorities("u1")).thenReturn(Optional.of(user));

        Authentication authentication = new UsernamePasswordAuthenticationToken("u1", "clave-incorrecta");
        listener().onBadCredentials(new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("bad creds")));

        verify(auditTrail, times(1)).registrar(any());
    }

    @Test
    void onSuccessNoRegistraNingunEventoDeAuditlog() {
        User user = usuario("u1", "c1", 3);
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        CustomUserDetails details = new CustomUserDetails(user, Set.of());
        Authentication authentication = new UsernamePasswordAuthenticationToken(details, null, List.of());
        listener().onSuccess(new AuthenticationSuccessEvent(authentication));

        verify(auditTrail, never()).registrar(any());
    }
}
