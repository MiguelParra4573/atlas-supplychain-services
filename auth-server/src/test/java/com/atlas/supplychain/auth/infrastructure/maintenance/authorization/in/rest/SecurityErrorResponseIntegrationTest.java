package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Este test debe fallar contra el código previo a la Spec 1.6: SecurityConfig construía su
 * propio ErrorResponse fuera del @RestControllerAdvice y filtraba authException.getMessage()
 * / accessDeniedException.getMessage() al cliente en cada 401/403.
 *
 * Hoy no existe en la app ningún endpoint con @PreAuthorize que dispare un 403 real por HTTP
 * (ningún controller usa hasRole/hasAuthority). Para probar restAccessDeniedHandler end-to-end
 * sin tocar la superficie de autorización real de la app, TestOnlyProtectedController vive
 * solo en src/test/java (nunca se empaqueta en el jar de producción) y se registra únicamente
 * en el ApplicationContext de este test vía @Import.
 */
@Import(SecurityErrorResponseIntegrationTest.TestOnlyProtectedController.class)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class SecurityErrorResponseIntegrationTest {

    @RestController
    static class TestOnlyProtectedController {
        @GetMapping("/test-only/solo-con-autoridad-inexistente")
        @PreAuthorize("hasAuthority('TEST_ONLY_AUTORIDAD_QUE_NADIE_TIENE')")
        public String protegido() {
            return "no deberia llegar aqui";
        }
    }

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

        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") " +
                        "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)",
                idCompany, username, username + "@test.com", passwordEncoder.encode("Test#1234"), idRole);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, "Test#1234"), TokenResponse.class);
        token = login.getAccessToken();
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401SinElMensajeDeLaAuthenticationException() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/usuarios", HttpMethod.GET, null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("No autorizado");
        assertThat(body.code()).isNull();
        assertThat(body.correlationId()).isNotBlank();

        ResponseEntity<String> raw = restTemplate.exchange("/usuarios", HttpMethod.GET, null, String.class);
        assertThat(raw.getBody()).doesNotContainIgnoringCase("full authentication")
                .doesNotContain("backendMessage")
                .doesNotContain("JWT")
                .doesNotContain("Bearer");
    }

    @Test
    void endpointProtegidoPorPreAuthorizeConUsuarioAutenticadoSinLaAutoridadDevuelve403SinElMensajeDeLaAccessDeniedException() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/test-only/solo-con-autoridad-inexistente", HttpMethod.GET, withAuth(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Acceso denegado. No tienes permisos para realizar esta acción.");
        assertThat(body.code()).isNull();
        assertThat(body.correlationId()).isNotBlank();

        ResponseEntity<String> raw = restTemplate.exchange(
                "/test-only/solo-con-autoridad-inexistente", HttpMethod.GET, withAuth(), String.class);
        assertThat(raw.getBody()).doesNotContainIgnoringCase("access denied")
                .doesNotContain("backendMessage")
                .doesNotContain("TEST_ONLY_AUTORIDAD_QUE_NADIE_TIENE");
    }
}
