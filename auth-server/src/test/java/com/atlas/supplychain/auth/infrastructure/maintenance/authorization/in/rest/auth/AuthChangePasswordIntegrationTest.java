package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthChangePasswordIntegrationTest {

    private static final String PASSWORD_INICIAL = "Test#12345";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idUser;
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

        KeyHolder userKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") " +
                            "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, username + "@test.com");
            ps.setString(4, passwordEncoder.encode(PASSWORD_INICIAL));
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);
        idUser = String.valueOf(userKeyHolder.getKey().longValue());

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, PASSWORD_INICIAL), TokenResponse.class);
        token = login.getAccessToken();
    }

    private HttpEntity<Object> withAuth(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void currentPasswordIncorrectaDevuelve400ConCodePasswordActualIncorrecta() {
        var req = new ChangePasswordRequest("clave-mala", "Nueva#1234");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/auth/change-password", HttpMethod.POST, withAuth(req), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(BusinessRuleCode.PASSWORD_ACTUAL_INCORRECTA.name());
    }

    @Test
    void reutilizarLaContrasenaActualDevuelve409ConCodePasswordReutilizada() {
        String passwordIntermedia = "Cambiada#67890";

        ResponseEntity<Void> primerCambio = restTemplate.exchange(
                "/auth/change-password", HttpMethod.POST,
                withAuth(new ChangePasswordRequest(PASSWORD_INICIAL, passwordIntermedia)), Void.class);
        assertThat(primerCambio.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/auth/change-password", HttpMethod.POST,
                withAuth(new ChangePasswordRequest(passwordIntermedia, PASSWORD_INICIAL)), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(BusinessRuleCode.PASSWORD_REUTILIZADA.name());
    }

    @Test
    void nuevaContrasenaQueIncumplePoliticaDevuelve400ConCodePasswordNoCumplePoliticaYMensajeGenerico() {
        var req = new ChangePasswordRequest(PASSWORD_INICIAL, "corta1");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/auth/change-password", HttpMethod.POST, withAuth(req), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA.name());
        assertThat(response.getBody().message())
                .isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA.getPublicMessage());
        assertThat(response.getBody().message()).doesNotContain("al menos");
    }
}
