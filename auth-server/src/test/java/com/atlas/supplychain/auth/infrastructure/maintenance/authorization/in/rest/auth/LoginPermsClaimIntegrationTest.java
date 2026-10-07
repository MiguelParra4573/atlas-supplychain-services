package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtService;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 2.3 -- el access token de POST /auth/login lleva un claim "perms" con las authorities
 * efectivas del usuario en formato MODULO:OPERACION, para que otros servicios (ms-auditoria,
 * y en el futuro ms-notificacion/ms-reporte) puedan autorizar localmente sin consultar auth.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class LoginPermsClaimIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void elAccessTokenDeLoginContieneElClaimPermsConLasAuthoritiesEfectivas() {
        String idCompany = UUID.randomUUID().toString();
        String nombreRol = "Auditor-" + UUID.randomUUID();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, nombreRol);
            return ps;
        }, keyHolder);
        long idRole = keyHolder.getKey().longValue();
        RbacSeeder.concederUnaAutoridad(jdbcTemplate, idCompany, idRole, "AUDITORIA", "CONSULTAR");

        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        KeyHolder userKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") " +
                            "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, username + "@test.com");
            ps.setString(4, passwordEncoder.encode("Test#1234"));
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, "Test#1234"), TokenResponse.class);

        Claims claims = jwtService.validarToken(login.getAccessToken());

        assertThat(claims.get("perms", List.class)).containsExactly("AUDITORIA:CONSULTAR");
        assertThat(claims.get("perms", List.class)).isEqualTo(claims.get("authorities", List.class));
    }
}
