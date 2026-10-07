package com.atlas.supplychain.auth.application.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.atlas.supplychain.auth.application.security.contracts.NotificationSender;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHasher;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHistoryStore;
import com.atlas.supplychain.auth.application.security.contracts.PasswordPolicy;
import com.atlas.supplychain.auth.application.security.contracts.PasswordResetTokenStore;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserStore;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ResetPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ResetTokenData;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserStore userStore;
    @Mock
    private PasswordResetTokenStore tokenStore;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private PasswordHistoryStore passwordHistoryStore;
    @Mock
    private RefreshSessionManager refreshSessionManager;
    @Mock
    private NotificationSender notificationSender;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;

    @BeforeEach
    void capturarLogs() {
        serviceLogger = (Logger) LoggerFactory.getLogger(PasswordResetServiceImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
        org.mockito.Mockito.lenient().when(passwordPolicy.resetTokenTtl()).thenReturn(Duration.ofMinutes(20));
        org.mockito.Mockito.lenient().when(passwordPolicy.historyCount()).thenReturn(5);
        org.mockito.Mockito.lenient().when(passwordPolicy.passwordExpiration()).thenReturn(Duration.ofDays(90));
    }

    @AfterEach
    void limpiarLogs() {
        serviceLogger.detachAppender(logAppender);
    }

    private PasswordResetServiceImpl service() {
        return new PasswordResetServiceImpl(userStore, tokenStore, passwordHasher, passwordPolicy,
                passwordHistoryStore, refreshSessionManager, notificationSender);
    }

    @Test
    void forgotConUsuarioYEmpresaCorrectosGeneraToken() {
        User user = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));

        var respuesta = service().forgot(new ForgotPasswordRequest("u1", "c1"));

        verify(tokenStore).invalidateAllActive(eq("u1"), eq("c1"), any());
        verify(tokenStore).save(eq("u1"), eq("c1"), anyString(), any());
        assertThat(respuesta.message()).isNotBlank();
    }

    @Test
    void forgotEnviaNotificationConDestinatarioYTokenCoherentesConElHashGuardado() {
        User user = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));

        service().forgot(new ForgotPasswordRequest("u1", "c1"));

        ArgumentCaptor<String> hashGuardado = ArgumentCaptor.forClass(String.class);
        verify(tokenStore).save(eq("u1"), eq("c1"), hashGuardado.capture(), any());

        ArgumentCaptor<String> tokenEnviado = ArgumentCaptor.forClass(String.class);
        verify(notificationSender).enviarResetPassword(eq("u1@atlas.test"), eq("u1"), tokenEnviado.capture(), eq(20L));

        assertThat(sha256(tokenEnviado.getValue())).isEqualTo(hashGuardado.getValue());
    }

    @Test
    void forgotConEmpresaEquivocadaNoGeneraTokenYDevuelveElMismoMensaje() {
        User user = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));

        var respuesta = service().forgot(new ForgotPasswordRequest("u1", "empresa-ajena"));

        verify(tokenStore, never()).invalidateAllActive(anyString(), anyString(), any());
        verify(tokenStore, never()).save(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(notificationSender);
        assertThat(respuesta.message()).isNotBlank();
    }

    @Test
    void forgotSinIdCompanyNoGeneraToken() {
        User user = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));

        var respuesta = service().forgot(new ForgotPasswordRequest("u1", null));

        verify(tokenStore, never()).save(anyString(), anyString(), anyString(), any());
        assertThat(respuesta.message()).isNotBlank();
    }

    @Test
    void forgotUsuarioInexistenteYExistenteDevuelvenElMismoMensaje() {
        User user = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));
        when(userStore.findWithAuthoritiesByUsername("desconocido")).thenReturn(Optional.empty());

        var existente = service().forgot(new ForgotPasswordRequest("u1", "c1"));
        var inexistente = service().forgot(new ForgotPasswordRequest("desconocido", "c1"));

        assertThat(existente.message()).isEqualTo(inexistente.message());
    }

    @Test
    void forgotUsuarioSinEmailNoLlamaAlSenderInvalidaElTokenYDevuelveElMismoMensaje() {
        User user = User.builder().idUser("u1").idCompany("c1").email(null).build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));

        var respuesta = service().forgot(new ForgotPasswordRequest("u1", "c1"));

        verifyNoInteractions(notificationSender);
        verify(tokenStore, times(2)).invalidateAllActive(eq("u1"), eq("c1"), any());
        assertThat(respuesta.message()).isNotBlank();
    }

    @Test
    void forgotConNotificationSenderQueFallaSigueRespondiendo200EInvalidaElTokenSinPropagarLaExcepcion() {
        User user = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("ms-notificacion caido"))
                .when(notificationSender).enviarResetPassword(anyString(), anyString(), anyString(), eq(20L));

        var respuesta = service().forgot(new ForgotPasswordRequest("u1", "c1"));

        assertThat(respuesta.message()).isNotBlank();
        verify(tokenStore, times(2)).invalidateAllActive(eq("u1"), eq("c1"), any());
    }

    @Test
    void forgotNuncaLogueaElValorDelTokenEnNingunEscenario() {
        User conEmail = User.builder().idUser("u1").idCompany("c1").email("u1@atlas.test").build();
        User sinEmail = User.builder().idUser("u2").idCompany("c1").email(null).build();
        when(userStore.findWithAuthoritiesByUsername("u1")).thenReturn(Optional.of(conEmail));
        when(userStore.findWithAuthoritiesByUsername("u2")).thenReturn(Optional.of(sinEmail));
        doThrow(new RuntimeException("ms-notificacion caido"))
                .when(notificationSender).enviarResetPassword(eq("u1@atlas.test"), anyString(), anyString(), eq(20L));

        service().forgot(new ForgotPasswordRequest("u1", "c1"));
        service().forgot(new ForgotPasswordRequest("u2", "c1"));

        ArgumentCaptor<String> hashes = ArgumentCaptor.forClass(String.class);
        verify(tokenStore).save(eq("u1"), eq("c1"), hashes.capture(), any());
        String hashGuardado = hashes.getValue();

        assertThat(logAppender.list).isNotEmpty();
        for (ILoggingEvent event : logAppender.list) {
            String mensaje = event.getFormattedMessage();
            assertThat(mensaje).doesNotContain(hashGuardado);
            assertThat(mensaje.toLowerCase()).doesNotContain("token=");
        }
    }

    @Test
    void resetLanzaTokenResetInvalidoSiHashNoExisteEnElStore() {
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.empty());

        var req = new ResetPasswordRequest("token-cualquiera", "Nueva#1234");

        assertThatThrownBy(() -> service().reset(req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.TOKEN_RESET_INVALIDO);
    }

    @Test
    void resetLanzaTokenResetExpiradoSiElTokenYaNoEsValido() {
        var tokenVencido = new ResetTokenData(1L, "u1", "c1", Instant.now().minusSeconds(10), false);
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.of(tokenVencido));

        var req = new ResetPasswordRequest("token-vencido", "Nueva#1234");

        assertThatThrownBy(() -> service().reset(req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.TOKEN_RESET_EXPIRADO);
    }

    @Test
    void tokenInexistenteYTokenExpiradoDevuelvenElMismoCodigoPublicoAunqueElCodigoInternoDifiere() {
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.empty());
        BusinessRuleException inexistente = capturarBusinessRuleException(
                new ResetPasswordRequest("token-inexistente", "Nueva#1234"));

        var tokenVencido = new ResetTokenData(1L, "u1", "c1", Instant.now().minusSeconds(10), false);
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.of(tokenVencido));
        BusinessRuleException vencido = capturarBusinessRuleException(
                new ResetPasswordRequest("token-vencido", "Nueva#1234"));

        // Códigos internos distintos (el log sí los distingue)...
        assertThat(inexistente.getCode()).isNotEqualTo(vencido.getCode());
        // ...pero el código público y el mensaje que ve el cliente colapsan a uno solo.
        assertThat(inexistente.getCode().getPublicCode()).isEqualTo(vencido.getCode().getPublicCode());
        assertThat(inexistente.getCode().getPublicMessage()).isEqualTo(vencido.getCode().getPublicMessage());
    }

    private BusinessRuleException capturarBusinessRuleException(ResetPasswordRequest req) {
        try {
            service().reset(req);
        } catch (BusinessRuleException e) {
            return e;
        } catch (EntityNotFoundException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("Se esperaba BusinessRuleException");
    }

    @Test
    void resetLanzaTokenResetInvalidoSiMarkUsedFalla() {
        var tokenValido = new ResetTokenData(1L, "u1", "c1", Instant.now().plusSeconds(600), false);
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.of(tokenValido));
        when(tokenStore.markUsed(eq(1L), any())).thenReturn(false);

        var req = new ResetPasswordRequest("token-valido", "Nueva#1234");

        assertThatThrownBy(() -> service().reset(req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.TOKEN_RESET_INVALIDO);
    }

    @Test
    void resetLanzaPasswordReutilizadaSiCoincideConHistorialReciente() throws EntityNotFoundException {
        var tokenValido = new ResetTokenData(1L, "u1", "c1", Instant.now().plusSeconds(600), false);
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.of(tokenValido));
        when(tokenStore.markUsed(eq(1L), any())).thenReturn(true);

        User user = User.builder().idUser("u1").idCompany("c1").passwordHash("hash-actual").build();
        when(userStore.findByIdUser("u1")).thenReturn(user);
        when(passwordHistoryStore.findRecentHashes("u1", "c1", 5)).thenReturn(List.of("hash-viejo"));
        when(passwordHasher.matches("Nueva#1234", "hash-viejo")).thenReturn(true);

        var req = new ResetPasswordRequest("token-valido", "Nueva#1234");

        assertThatThrownBy(() -> service().reset(req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_REUTILIZADA);
    }

    @Test
    void resetActualizaPasswordYRevocaSesionesCuandoTodoEsValido() throws EntityNotFoundException {
        var tokenValido = new ResetTokenData(1L, "u1", "c1", Instant.now().plusSeconds(600), false);
        when(tokenStore.findByHash(anyString())).thenReturn(Optional.of(tokenValido));
        when(tokenStore.markUsed(eq(1L), any())).thenReturn(true);

        User user = User.builder().idUser("u1").idCompany("c1").passwordHash("hash-actual").build();
        when(userStore.findByIdUser("u1")).thenReturn(user);
        when(passwordHistoryStore.findRecentHashes("u1", "c1", 5)).thenReturn(List.of());
        when(passwordHasher.hash(anyString())).thenReturn("hash-nuevo");

        var req = new ResetPasswordRequest("token-valido", "Nueva#1234");

        service().reset(req);

        assertThat(user.getPasswordHash()).isEqualTo("hash-nuevo");
    }

    private String sha256(String value) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(value.getBytes()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
