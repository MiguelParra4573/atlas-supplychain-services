package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.contracts.PasswordResetTokenStore;
import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.RefreshRequest;
import com.atlas.supplychain.auth.application.security.dto.ResetPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.4 -- prueba que la clave de historial de password y la de revocación de sesiones
 * es realmente la misma (idUser) en los dos flujos de cambio de contraseña
 * (change-password autenticado y password/reset por token). Antes de esta spec no existía
 * ningún test que cruzara ambos flujos: cada uno se probaba de forma aislada.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class PasswordCrossFlowIntegrationTest {

    private static final String PASSWORD_INICIAL = "Test#1234";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenStore passwordResetTokenStore;

    private String idUser;
    private String idCompany;
    private String username;

    @BeforeEach
    void sembrarUsuario() {
        idCompany = UUID.randomUUID().toString();
        username = "user." + UUID.randomUUID().toString().substring(0, 8);

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
    }

    private TokenResponse login(String password) {
        return restTemplate.postForObject("/auth/login", new LoginRequest(username, password), TokenResponse.class);
    }

    private HttpEntity<Object> withAuth(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /** Simula el token crudo de un email de reseteo, sembrando el hash tal como lo hace PasswordResetServiceImpl.forgot(). */
    private String sembrarTokenDeReset() {
        String rawToken = UUID.randomUUID().toString();
        String hash = sha256Hex(rawToken);
        passwordResetTokenStore.save(idUser, idCompany, hash, Instant.now().plusSeconds(600));
        return rawToken;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * La regla de negocio guarda en el historial el hash que queda SUSTITUIDO en cada
     * cambio (no el nuevo). Por eso el cruce correcto es: cambiar por un flujo deja la
     * contraseña anterior en el historial, y intentar volver a ella por el otro flujo
     * debe rechazarse -- si las dos claves de historial fueran distintas (el bug latente
     * que esta spec cierra), este reset pasaría igual porque no encontraría el hash.
     */
    @Test
    void passwordSustituidaPorChangePasswordEsRechazadaAlReestablecerlaPorReset() {
        TokenResponse login = login(PASSWORD_INICIAL);

        ResponseEntity<Void> cambio = restTemplate.exchange(
                "/auth/change-password", org.springframework.http.HttpMethod.POST,
                withAuth(login.getAccessToken(), new ChangePasswordRequest(PASSWORD_INICIAL, "Cambiada#2024")), Void.class);
        assertThat(cambio.getStatusCode()).isEqualTo(HttpStatus.OK);

        String rawToken = sembrarTokenDeReset();
        ResponseEntity<String> reset = restTemplate.postForEntity(
                "/auth/password/reset", new ResetPasswordRequest(rawToken, PASSWORD_INICIAL), String.class);

        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void passwordSustituidaPorResetEsRechazadaAlEstablecerlaPorChangePassword() {
        String rawToken = sembrarTokenDeReset();
        ResponseEntity<String> reset = restTemplate.postForEntity(
                "/auth/password/reset", new ResetPasswordRequest(rawToken, "Restablecida#2024"), String.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);

        TokenResponse login = login("Restablecida#2024");
        ResponseEntity<String> cambio = restTemplate.exchange(
                "/auth/change-password", org.springframework.http.HttpMethod.POST,
                withAuth(login.getAccessToken(), new ChangePasswordRequest("Restablecida#2024", PASSWORD_INICIAL)), String.class);

        assertThat(cambio.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshTokenEmitidoAntesQuedaRevocadoTrasCambiarPasswordPorChangePassword() {
        TokenResponse login = login(PASSWORD_INICIAL);

        ResponseEntity<Void> cambio = restTemplate.exchange(
                "/auth/change-password", org.springframework.http.HttpMethod.POST,
                withAuth(login.getAccessToken(), new ChangePasswordRequest(PASSWORD_INICIAL, "Cambiada#2024")), Void.class);
        assertThat(cambio.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> refresh = restTemplate.postForEntity(
                "/auth/refresh", new RefreshRequest(login.getRefreshToken()), String.class);

        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshTokenEmitidoAntesQuedaRevocadoTrasCambiarPasswordPorReset() {
        TokenResponse login = login(PASSWORD_INICIAL);
        String rawToken = sembrarTokenDeReset();

        ResponseEntity<String> reset = restTemplate.postForEntity(
                "/auth/password/reset", new ResetPasswordRequest(rawToken, "Restablecida#2024"), String.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> refresh = restTemplate.postForEntity(
                "/auth/refresh", new RefreshRequest(login.getRefreshToken()), String.class);

        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
