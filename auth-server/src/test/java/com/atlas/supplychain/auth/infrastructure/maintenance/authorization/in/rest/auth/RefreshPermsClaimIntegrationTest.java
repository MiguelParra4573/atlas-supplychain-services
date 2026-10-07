package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.RefreshRequest;
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
 * Spec 2.3 -- el claim "perms" se propaga simétricamente en refresh: si solo se tocara el
 * login, el token renovado perdería los permisos y el usuario empezaría a recibir 403 tras
 * el primer refresh. Este test es el olvido más fácil de la spec.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RefreshPermsClaimIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void elAccessTokenRenovadoContieneElMismoClaimPermsQueElDeLogin() {
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

        Claims loginClaims = jwtService.validarToken(login.getAccessToken());
        List<String> permsDeLogin = loginClaims.get("perms", List.class);
        assertThat(permsDeLogin).containsExactly("AUDITORIA:CONSULTAR");

        TokenResponse refresh = restTemplate.postForObject(
                "/auth/refresh", new RefreshRequest(login.getRefreshToken()), TokenResponse.class);

        Claims refreshClaims = jwtService.validarToken(refresh.getAccessToken());
        assertThat(refreshClaims.get("perms", List.class)).isEqualTo(permsDeLogin);
    }
}
