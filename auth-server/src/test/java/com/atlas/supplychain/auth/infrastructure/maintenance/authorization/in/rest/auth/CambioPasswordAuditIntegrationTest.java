package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.PasswordService;
import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.infrastructure.common.messaging.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Verifica el disparo AFTER_COMMIT (Spec 2.4 SS4) para el cambio de password propio.
 *
 * <p>{@code PasswordServiceImpl.changeMyPassword(...)} no lleva su propio {@code @Transactional}
 * (el paquete {@code application.security} nunca lo tuvo -- ver el Javadoc de
 * {@code ApplicationTransactionConfig}, que solo cubre {@code application.maintenance.**}). La
 * transaccion real de la escritura vive mas abajo, en {@code UserRepositoryImpl} (clase completa
 * anotada {@code @Transactional}, propagacion REQUIRED por defecto): sin una transaccion activa
 * alrededor, esa transaccion se abre y confirma dentro de la propia llamada a
 * {@code userStore.save(user)}, antes de que {@code changeMyPassword} siquiera llegue a la linea
 * de {@code auditTrail.registrar(...)}.
 *
 * <p>Para poder observar un rollback real que tambien alcance al evento de auditlog -- y no solo
 * comprobar que si el guardado falla nunca se llega a publicar el evento, que seria una obviedad
 * de control de flujo -- este test envuelve la llamada al servicio en un {@code @Transactional}
 * propio ({@link RollbackForzadoConfig}). Al unirse por propagacion REQUIRED a esa misma
 * transaccion, tanto {@code UserRepositoryImpl.actualizarUsuario(...)} como el
 * {@code ApplicationEvent} publicado por {@code AuditTrailPublisher} quedan sincronizados a ella;
 * el {@code @TransactionalEventListener(phase = AFTER_COMMIT)} de {@code AuditRabbitPublisher}
 * nunca se dispara si esa transaccion hace rollback. El {@link RabbitTemplate} es un mock: se
 * verifica que se publique (o no) {@code AuditoriaRegistrada}, sin broker real.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(CambioPasswordAuditIntegrationTest.RollbackForzadoConfig.class)
class CambioPasswordAuditIntegrationTest {

    private static final String PASSWORD_INICIAL = "Test#12345";

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private RollbackForzadoConfig.CambioPasswordConRollbackForzado wrapperConRollback;

    private String idUser;
    private String idCompany;

    @BeforeEach
    void sembrarUsuario() {
        idCompany = UUID.randomUUID().toString();
        String username = "user." + UUID.randomUUID().toString().substring(0, 8);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, "Admin-" + idCompany);
            return ps;
        }, keyHolder);
        long idRole = keyHolder.getKey().longValue();

        KeyHolder userKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"users\" "
                            + "(\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", "
                            + "\"failed_attempts\", \"state\", \"id_role\") "
                            + "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, username + "@test.com");
            ps.setString(4, passwordEncoder.encode(PASSWORD_INICIAL));
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);
        idUser = String.valueOf(userKeyHolder.getKey().longValue());
    }

    @Test
    void seAuditaCuandoLaOperacionSeCompletaSinErrores() throws EntityNotFoundException {
        var req = new ChangePasswordRequest(PASSWORD_INICIAL, "Nueva#98765");

        passwordService.changeMyPassword(idUser, idCompany, req);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(eq("auth.events"), eq("AuditoriaRegistrada"), captor.capture());
        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.aggregateId()).isEqualTo(idUser);
        assertThat(envelope.payload().get("accionNombre").asString()).isEqualTo("CAMBIO_PASSWORD");
        assertThat(envelope.payload().get("companyId").asString()).isEqualTo(idCompany);
    }

    @Test
    void noSeAuditaSiLaTransaccionHizoRollback() {
        var req = new ChangePasswordRequest(PASSWORD_INICIAL, "Nueva#98765");

        assertThatThrownBy(() -> wrapperConRollback.cambiarYForzarRollback(idUser, idCompany, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fallo forzado");

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @TestConfiguration
    static class RollbackForzadoConfig {

        @Bean
        CambioPasswordConRollbackForzado cambioPasswordConRollbackForzado(PasswordService passwordService) {
            return new CambioPasswordConRollbackForzado(passwordService);
        }

        /**
         * Envuelve el caso de uso en su propio {@code @Transactional} (REQUIRED, sin transaccion
         * activa alrededor: abre una nueva) para que {@code UserRepositoryImpl.actualizarUsuario(...)}
         * se una a ella en vez de abrir y confirmar la suya propia, y para que el
         * {@code ApplicationEvent} publicado por {@code AuditTrailPublisher} quede sincronizado a la
         * misma transaccion. Al lanzar una excepcion no chequeada justo despues de llamar al
         * servicio, toda la transaccion -- guardado del password y evento de auditlog incluidos --
         * hace rollback antes de confirmar.
         */
        @RequiredArgsConstructor
        static class CambioPasswordConRollbackForzado {
            private final PasswordService passwordService;

            @Transactional
            void cambiarYForzarRollback(String idUser, String idCompany, ChangePasswordRequest req)
                    throws EntityNotFoundException {
                passwordService.changeMyPassword(idUser, idCompany, req);
                throw new RuntimeException("Fallo forzado para test de rollback (Spec 2.4)");
            }
        }
    }
}
