package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.security.contracts.AccessTokenGenerator;
import com.atlas.supplychain.auth.application.security.contracts.Authenticator;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserIdentityProvider;
import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.RefreshRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private Authenticator authenticator;
    @Mock
    private AccessTokenGenerator accessTokenGenerator;
    @Mock
    private RefreshSessionManager refreshSessionManager;
    @Mock
    private UserIdentityProvider userIdentityProvider;
    @Mock
    private AuditTrail auditTrail;

    private AuthenticationService service() {
        return new AuthenticationService(authenticator, accessTokenGenerator, refreshSessionManager, userIdentityProvider, auditTrail);
    }

    @ParameterizedTest
    @ValueSource(longs = {900L, 300L})
    void loginExpiresInCoincideConElTtlRealDelAccessTokenGenerator(long ttlSeconds) {
        when(authenticator.authenticate("usuario1", "clave"))
                .thenReturn(new Authenticator.AuthenticatedUser("usuario1", "miguel.parra", "Miguel", "Parra", "1", List.of("ADMIN"), List.of("USR_READ")));
        when(accessTokenGenerator.generate(any())).thenReturn("access-token");
        when(accessTokenGenerator.ttlSeconds()).thenReturn(ttlSeconds);
        when(refreshSessionManager.issueForLogin("usuario1", "1"))
                .thenReturn(new RefreshSessionManager.IssueResult("refresh-token"));
        when(refreshSessionManager.refreshTtlSeconds()).thenReturn(604800L);

        TokenResponse response = service().login(LoginRequest.builder().usuario("usuario1").contrasena("clave").build());

        assertThat(response.getExpiresIn()).isEqualTo(ttlSeconds);
    }

    @Test
    void loginPoblaUsuarioConElUsernameRealNoConElIdUser() {
        when(authenticator.authenticate("usuario1", "clave"))
                .thenReturn(new Authenticator.AuthenticatedUser("usuario1", "miguel.parra", "Miguel", "Parra", "1", List.of("ADMIN"), List.of("USR_READ")));
        when(accessTokenGenerator.generate(any())).thenReturn("access-token");
        when(accessTokenGenerator.ttlSeconds()).thenReturn(900L);
        when(refreshSessionManager.issueForLogin("usuario1", "1"))
                .thenReturn(new RefreshSessionManager.IssueResult("refresh-token"));
        when(refreshSessionManager.refreshTtlSeconds()).thenReturn(604800L);

        TokenResponse response = service().login(LoginRequest.builder().usuario("usuario1").contrasena("clave").build());

        // Regresión del bug real detectado 2026-09-03: el body de login devolvía el idUser
        // ("usuario1" acá) en vez del username visible -- ver AuthenticationService.login().
        assertThat(response.getUser().getUsuario()).isEqualTo("miguel.parra");
    }

    @Test
    void loginExitosoRegistraEventoDeAuditlogConLosCamposDeLaTablaDeMapeo() {
        when(authenticator.authenticate("usuario1", "clave"))
                .thenReturn(new Authenticator.AuthenticatedUser("usuario1", "miguel.parra", "Miguel", "Parra", "c1", List.of("ADMIN", "OTRO"), List.of("USR_READ")));
        when(accessTokenGenerator.generate(any())).thenReturn("access-token");
        when(accessTokenGenerator.ttlSeconds()).thenReturn(900L);
        when(refreshSessionManager.issueForLogin("usuario1", "c1"))
                .thenReturn(new RefreshSessionManager.IssueResult("refresh-token"));
        when(refreshSessionManager.refreshTtlSeconds()).thenReturn(604800L);

        service().login(LoginRequest.builder().usuario("usuario1").contrasena("clave").build());

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        AuditEventCommand cmd = captor.getValue();

        assertThat(cmd.accionTipo()).isEqualTo("LOGIN");
        assertThat(cmd.accionNombre()).isEqualTo("LOGIN_EXITOSO");
        assertThat(cmd.entidadTipo()).isEqualTo("USUARIO");
        assertThat(cmd.entidadId()).isEqualTo("usuario1");
        assertThat(cmd.modulo()).isEqualTo("SEGURIDAD");
        assertThat(cmd.severity()).isEqualTo("LOW");
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isEqualTo("usuario1");
        assertThat(cmd.username()).isEqualTo("usuario1");
        assertThat(cmd.rolPrincipal()).isEqualTo("ADMIN");
        assertThat(cmd.resumen()).isEqualTo("Inicio de sesión exitoso");
    }

    @Test
    void loginExitosoConUsuarioSinRolesRegistraRolPrincipalNulo() {
        when(authenticator.authenticate("usuario1", "clave"))
                .thenReturn(new Authenticator.AuthenticatedUser("usuario1", "miguel.parra", "Miguel", "Parra", "c1", List.of(), List.of()));
        when(accessTokenGenerator.generate(any())).thenReturn("access-token");
        when(accessTokenGenerator.ttlSeconds()).thenReturn(900L);
        when(refreshSessionManager.issueForLogin("usuario1", "c1"))
                .thenReturn(new RefreshSessionManager.IssueResult("refresh-token"));
        when(refreshSessionManager.refreshTtlSeconds()).thenReturn(604800L);

        service().login(LoginRequest.builder().usuario("usuario1").contrasena("clave").build());

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());
        assertThat(captor.getValue().rolPrincipal()).isNull();
    }

    @Test
    void refreshNoRegistraEventoDeAuditlog() {
        when(refreshSessionManager.rotate("refresh-token"))
                .thenReturn(new RefreshSessionManager.RotateResult("nuevo-refresh-token", "usuario1", "1"));
        when(userIdentityProvider.load("usuario1"))
                .thenReturn(new UserIdentityProvider.UserIdentity("usuario1", "miguel.parra", "Miguel", "Parra", "1", List.of("ADMIN"), List.of("USR_READ")));
        when(accessTokenGenerator.generate(any())).thenReturn("access-token");
        when(accessTokenGenerator.ttlSeconds()).thenReturn(900L);
        when(refreshSessionManager.refreshTtlSeconds()).thenReturn(604800L);

        service().refresh(RefreshRequest.builder().refreshToken("refresh-token").build());

        verify(auditTrail, never()).registrar(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {900L, 300L})
    void refreshExpiresInCoincideConElTtlRealDelAccessTokenGenerator(long ttlSeconds) {
        when(refreshSessionManager.rotate("refresh-token"))
                .thenReturn(new RefreshSessionManager.RotateResult("nuevo-refresh-token", "usuario1", "1"));
        when(userIdentityProvider.load("usuario1"))
                .thenReturn(new UserIdentityProvider.UserIdentity("usuario1", "miguel.parra", "Miguel", "Parra", "1", List.of("ADMIN"), List.of("USR_READ")));
        when(accessTokenGenerator.generate(any())).thenReturn("access-token");
        when(accessTokenGenerator.ttlSeconds()).thenReturn(ttlSeconds);
        when(refreshSessionManager.refreshTtlSeconds()).thenReturn(604800L);

        TokenResponse response = service().refresh(RefreshRequest.builder().refreshToken("refresh-token").build());

        assertThat(response.getExpiresIn()).isEqualTo(ttlSeconds);
    }
}
