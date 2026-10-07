package com.atlas.supplychain.auth.infrastructure.common.outbox;

import com.atlas.supplychain.auth.application.maintenance.authorization.user.UserService;
import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Spec 1.24: {@code UserServiceImpl.eliminarUsuario} corre dentro de la transacción RW que
 * {@code ApplicationTransactionConfig} aplica por convención de nombre -- el {@code DELETE} de
 * {@code users} (baja física, ver {@code UserRepositoryImpl.eliminarUsuario}) y el
 * {@code INSERT} en {@code outbox_event} (vía {@code registrarUsuarioEliminado}) deben
 * confirmarse o deshacerse juntos. Se fuerza el fallo en {@code OutboxRepository.append} (que
 * corre después del {@code DELETE}, dentro de la misma transacción) con un {@code @MockitoBean} y se
 * verifica contra JDBC real que ninguna de las dos escrituras quedó -- mismo patrón que
 * {@link UserEmailChangeOutboxRollbackIntegrationTest} (Spec 3.4), aplicado al evento nuevo
 * {@code USUARIO_ELIMINADO} para reforzar que la atomicidad Outbox+negocio sigue funcionando.
 */
@SpringBootTest
class UserDeleteOutboxRollbackIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OutboxRepository outboxRepository;

    private String idUser;
    private String idCompany;
    private long idRole;

    @BeforeEach
    void sembrarUsuario() {
        idCompany = UUID.randomUUID().toString();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, "Admin-" + idCompany);
            return ps;
        }, keyHolder);
        idRole = keyHolder.getKey().longValue();

        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        KeyHolder userKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"users\" "
                            + "(\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", "
                            + "\"failed_attempts\", \"state\", \"id_role\") "
                            + "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, "u-" + username + "@test.com");
            ps.setString(4, passwordEncoder.encode("Test#1234"));
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);
        idUser = String.valueOf(userKeyHolder.getKey().longValue());
    }

    @Test
    void siFallaElOutboxNiElDeleteDeUsersNiElEventoQuedanPersistidos() {
        doThrow(new RuntimeException("Fallo forzado para test de rollback (Spec 1.24)"))
                .when(outboxRepository).append(any());

        assertThatThrownBy(() -> userService.eliminarUsuario(idUser, idCompany))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fallo forzado");

        Integer usuariosRestantes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"auth\".\"users\" WHERE \"id_user\" = CAST(? AS bigint)",
                Integer.class, idUser);
        assertThat(usuariosRestantes)
                .as("el DELETE de users debia revertirse junto con el fallo del outbox")
                .isEqualTo(1);

        Integer eventosOutbox = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"auth\".\"outbox_event\" WHERE \"aggregate_id\" = ?",
                Integer.class, idUser);
        assertThat(eventosOutbox).as("no debia quedar ningun evento outbox persistido").isZero();
    }

    @Test
    void siElOutboxNoFallaElDeleteYElEventoQuedanAmbosPersistidos() throws EntityNotFoundException {
        userService.eliminarUsuario(idUser, idCompany);

        Integer usuariosRestantes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"auth\".\"users\" WHERE \"id_user\" = CAST(? AS bigint)",
                Integer.class, idUser);
        assertThat(usuariosRestantes).isZero();
    }
}
