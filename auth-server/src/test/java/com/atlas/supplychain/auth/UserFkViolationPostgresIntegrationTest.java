package com.atlas.supplychain.auth;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.6 -- criterio de aceptación literal: DataIntegrityViolationException real contra
 * Postgres (violar la FK users.id_role -> roles.id_rol) devuelve 409 sin
 * fugar nombre de constraint, org.postgresql, org.hibernate ni fragmento de SQL. El
 * complemento ejecutable en H2 (sin este bloqueo) vive en UserFkViolationIntegrationTest.
 */
@Testcontainers
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("liquibase-it")
class UserFkViolationPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withInitScript("init-auth-schema.sql");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void sembrarUsuarioYLoguear() {
        String idCompany = UUID.randomUUID().toString();
        String idUser = UUID.randomUUID().toString();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO auth.roles (id_company, name, state) VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, "Admin-" + idCompany);
            return ps;
        }, keyHolder);
        long idRole = keyHolder.getKey().longValue();
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompany, idRole);

        jdbcTemplate.update(
                "INSERT INTO auth.users (username, id_company, name, last_name, email, password_hash, failed_attempts, state, id_role) " +
                        "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)",
                idUser, idCompany, idUser + "@test.com", passwordEncoder.encode("Test#1234"), idRole);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(idUser, "Test#1234"), TokenResponse.class);
        token = login.getAccessToken();
    }

    @Test
    void crearUsuarioConIdRoleInexistenteDevuelve409SinFugarNombreDeConstraintNiSql() {
        UserDTO dto = UserDTO.builder()
                .idUser(UUID.randomUUID().toString())
                .name("Nuevo")
                .lastName("Usuario")
                .email("nuevo-" + UUID.randomUUID() + "@test.com")
                .password("Clave123")
                .state(true)
                .idRole(999_999_999L)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios", HttpMethod.POST, new HttpEntity<>(dto, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .doesNotContainIgnoringCase("constraint")
                .doesNotContain("org.postgresql")
                .doesNotContainIgnoringCase("org.hibernate")
                .doesNotContain("INSERT INTO");
    }
}
