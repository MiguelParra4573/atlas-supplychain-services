package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.11 punto 6: /actuator/health sigue público (sin el bloque "components" para un
 * anónimo, show-details=when-authorized ya lo garantiza); /actuator/info pasa a authenticated().
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
        "management.endpoints.web.exposure.include=health,info",
        "management.endpoint.health.show-details=when-authorized"
})
class ActuatorAccessIntegrationTest {

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

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, "Admin-" + idCompany);
            return ps;
        }, keyHolder);
        long idRole = keyHolder.getKey().longValue();

        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") " +
                        "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)",
                idCompany, username, username + "@test.com", passwordEncoder.encode("Test#1234"), idRole);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, "Test#1234"), TokenResponse.class);
        token = login.getAccessToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthSinTokenDevuelve200ConStatusPeroSinComponents() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/actuator/health", HttpMethod.GET, null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody()).doesNotContainKey("components");
    }

    @Test
    void infoSinTokenDevuelve401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/info", HttpMethod.GET, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void infoConJwtValidoDevuelve200() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/info", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
