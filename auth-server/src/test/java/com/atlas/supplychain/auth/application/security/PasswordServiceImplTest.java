package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHasher;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHistoryStore;
import com.atlas.supplychain.auth.application.security.contracts.PasswordPolicy;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserStore;
import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

    @Mock
    private UserStore userStore;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private PasswordHistoryStore passwordHistoryStore;
    @Mock
    private RefreshSessionManager refreshSessionManager;
    @Mock
    private AuditTrail auditTrail;

    private PasswordServiceImpl service() {
        return new PasswordServiceImpl(userStore, passwordHasher, passwordPolicy, passwordHistoryStore,
                refreshSessionManager, auditTrail);
    }

    @Test
    void changeMyPasswordLanzaPasswordActualIncorrectaSiNoCoincide() throws EntityNotFoundException {
        User user = User.builder().idUser("u1").idCompany("c1").passwordHash("hash-actual").build();
        when(userStore.findByIdUser("u1")).thenReturn(user);
        when(passwordHasher.matches("mala", "hash-actual")).thenReturn(false);

        var req = new ChangePasswordRequest("mala", "Nueva#1234");

        assertThatThrownBy(() -> service().changeMyPassword("u1", "c1", req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_ACTUAL_INCORRECTA);

        verify(userStore, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changeMyPasswordLanzaPasswordReutilizadaSiCoincideConHistorialReciente() throws EntityNotFoundException {
        User user = User.builder().idUser("u1").idCompany("c1").passwordHash("hash-actual").build();
        when(userStore.findByIdUser("u1")).thenReturn(user);
        when(passwordHasher.matches("actual", "hash-actual")).thenReturn(true);
        when(passwordPolicy.historyCount()).thenReturn(5);
        when(passwordHistoryStore.findRecentHashes("u1", "c1", 5)).thenReturn(List.of("hash-viejo"));
        when(passwordHasher.matches("Nueva#1234", "hash-viejo")).thenReturn(true);

        var req = new ChangePasswordRequest("actual", "Nueva#1234");

        assertThatThrownBy(() -> service().changeMyPassword("u1", "c1", req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_REUTILIZADA);

        verify(userStore, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changeMyPasswordActualizaYRevocaSesionesCuandoTodoEsValido() throws EntityNotFoundException {
        User user = User.builder().idUser("u1").idCompany("c1").passwordHash("hash-actual").build();
        when(userStore.findByIdUser("u1")).thenReturn(user);
        when(passwordHasher.matches("actual", "hash-actual")).thenReturn(true);
        when(passwordPolicy.historyCount()).thenReturn(5);
        when(passwordPolicy.passwordExpiration()).thenReturn(Duration.ofDays(90));
        when(passwordHistoryStore.findRecentHashes("u1", "c1", 5)).thenReturn(List.of());
        when(passwordHasher.hash(anyString())).thenReturn("hash-nuevo");

        var req = new ChangePasswordRequest("actual", "Nueva#1234");

        service().changeMyPassword("u1", "c1", req);

        assertThat(user.getPasswordHash()).isEqualTo("hash-nuevo");
        verify(userStore).save(user);
        verify(refreshSessionManager).revokeAllSessions("u1", "c1");
        verify(auditTrail).registrar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changeMyPasswordRegistraAuditlogConLosCamposEsperadosTrasElSaveYAntesDeRevocarSesiones()
            throws EntityNotFoundException {
        User user = User.builder().idUser("u1").idCompany("c1").passwordHash("hash-actual").build();
        when(userStore.findByIdUser("u1")).thenReturn(user);
        when(passwordHasher.matches("actual", "hash-actual")).thenReturn(true);
        when(passwordPolicy.historyCount()).thenReturn(5);
        when(passwordPolicy.passwordExpiration()).thenReturn(Duration.ofDays(90));
        when(passwordHistoryStore.findRecentHashes("u1", "c1", 5)).thenReturn(List.of());
        when(passwordHasher.hash(anyString())).thenReturn("hash-nuevo");

        var req = new ChangePasswordRequest("actual", "Nueva#1234");

        service().changeMyPassword("u1", "c1", req);

        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditTrail).registrar(captor.capture());

        AuditEventCommand cmd = captor.getValue();
        assertThat(cmd.accionTipo()).isEqualTo("UPDATE");
        assertThat(cmd.accionNombre()).isEqualTo("CAMBIO_PASSWORD");
        assertThat(cmd.entidadTipo()).isEqualTo("USUARIO");
        assertThat(cmd.entidadId()).isEqualTo("u1");
        assertThat(cmd.modulo()).isEqualTo("SEGURIDAD");
        assertThat(cmd.severity()).isEqualTo("MEDIUM");
        assertThat(cmd.companyId()).isEqualTo("c1");
        assertThat(cmd.userId()).isEqualTo("u1");
        assertThat(cmd.username()).isEqualTo("u1");
        assertThat(cmd.rolPrincipal()).isNull();
        assertThat(cmd.resumen()).isEqualTo("Cambio de contraseña propio");

        InOrder inOrder = inOrder(userStore, auditTrail, refreshSessionManager);
        inOrder.verify(userStore).save(user);
        inOrder.verify(auditTrail).registrar(org.mockito.ArgumentMatchers.any());
        inOrder.verify(refreshSessionManager).revokeAllSessions("u1", "c1");
    }
}
