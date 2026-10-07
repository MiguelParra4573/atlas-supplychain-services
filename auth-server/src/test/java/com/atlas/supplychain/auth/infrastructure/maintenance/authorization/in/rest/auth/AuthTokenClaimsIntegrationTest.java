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
 * Spec 1.7 §6 -- el JWT de POST /auth/login lleva sub, idCompany, iss=auth-server, roles con
 * nombres de rol reales (no autoridades) y authorities en formato MODULO:OPERACION.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthTokenClaimsIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void elAccessTokenDeLoginContieneLosClaimsDefinitivosDeLaSpec17() {
        String idCompany = UUID.randomUUID().toString();
        String nombreRol = "Administrador-" + UUID.randomUUID();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, nombreRol);
            return ps;
        }, keyHolder);
        long idRole = keyHolder.getKey().longValue();
        RbacSeeder.concederUnaAutoridad(jdbcTemplate, idCompany, idRole, "USUARIOS", "CREAR");

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
        String idUser = String.valueOf(userKeyHolder.getKey().longValue());

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, "Test#1234"), TokenResponse.class);

        Claims claims = jwtService.validarToken(login.getAccessToken());

        assertThat(claims.getSubject()).isEqualTo(idUser);
        assertThat(claims.get("idCompany", String.class)).isEqualTo(idCompany);
        assertThat(claims.getIssuer()).isEqualTo("auth-server");
        assertThat(claims.get("roles", List.class)).containsExactly(nombreRol);
        assertThat(claims.get("authorities", List.class)).containsExactly("USUARIOS:CREAR");

        // El body de login también refleja el mismo contrato (TokenResponse.UserInfo).
        assertThat(login.getUser().getRoles()).containsExactly(nombreRol);
        assertThat(login.getUser().getAuthorities()).containsExactly("USUARIOS:CREAR");
        // Regresión del bug real detectado 2026-09-03: usuario != idUser en este seed a propósito,
        // para que este assert distinga el bug (devolvía idUser) del fix (username real).
        assertThat(login.getUser().getUsuario()).isEqualTo(username);
    }
}
