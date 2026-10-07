package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.contracts.PasswordResetTokenStore;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordResponse;
import com.atlas.supplychain.auth.application.security.dto.ResetPasswordRequest;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthPasswordResetFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PasswordResetTokenStore passwordResetTokenStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private String idUserA;
    private String usernameA;
    private String idCompanyB;

    private record UsuarioSembrado(String idUser, String username) {}

    @BeforeEach
    void sembrarUsuarioDeEmpresaA() {
        String hash = passwordEncoder.encode("Test#1234");
        idCompanyA = UUID.randomUUID().toString();
        UsuarioSembrado sembrado = seedUsuario(idCompanyA, "reset-a-" + UUID.randomUUID() + "@test.com", hash);
        idUserA = sembrado.idUser();
        usernameA = sembrado.username();
        idCompanyB = UUID.randomUUID().toString();
    }

    private UsuarioSembrado seedUsuario(String idCompany, String email, String hash) {
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
        KeyHolder userKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") " +
                            "VALUES (?, ?, 'Reset', 'Test', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setString(4, hash);
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);

        return new UsuarioSembrado(String.valueOf(userKeyHolder.getKey().longValue()), username);
    }

    @Test
    void forgotPasswordNoDevuelveTokenNiEnBodyNiEnHeaders() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/password/forgot", new ForgotPasswordRequest(usernameA, idCompanyA), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().keySet()).containsExactly("message");
        response.getHeaders().forEach((name, values) -> {
            assertThat(name.toLowerCase()).doesNotContain("token");
            values.forEach(v -> assertThat(v.toLowerCase()).doesNotContain("token"));
        });
    }

    @Test
    void usuarioExistenteYUsuarioInexistenteDevuelvenElMismoStatusYMensaje() {
        ResponseEntity<ForgotPasswordResponse> existente = restTemplate.postForEntity(
                "/auth/password/forgot", new ForgotPasswordRequest(usernameA, idCompanyA), ForgotPasswordResponse.class);
        ResponseEntity<ForgotPasswordResponse> inexistente = restTemplate.postForEntity(
                "/auth/password/forgot", new ForgotPasswordRequest("no-existe-" + UUID.randomUUID(), idCompanyA), ForgotPasswordResponse.class);

        assertThat(existente.getStatusCode()).isEqualTo(inexistente.getStatusCode());
        assertThat(existente.getBody().message()).isEqualTo(inexistente.getBody().message());
    }

    /**
     * Desde la Spec 1.11, idCompany es @NotBlank en el DTO: la ausencia se corta en
     * Bean Validation (400) antes de llegar al service. Ya no aplica el mensaje genérico
     * de 200 -- ese contrato de anti-enumeración cubre usuario inexistente con payload bien
     * formado (ver usuarioExistenteYUsuarioInexistenteDevuelvenElMismoStatusYMensaje), no
     * un campo obligatorio ausente.
     */
    @Test
    void forgotSinIdCompanyDevuelve400PorValidacionYNoGeneraToken() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/auth/password/forgot", new ForgotPasswordRequest(usernameA, null), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"auth\".\"password_reset_tokens\" WHERE \"id_user\" = CAST(? AS bigint)",
                Integer.class, idUserA);
        assertThat(count).isZero();
    }

    @Test
    void forgotConIdCompanyDeOtraEmpresaNoGeneraTokenParaElUsuarioDeLaEmpresaA() {
        ResponseEntity<ForgotPasswordResponse> response = restTemplate.postForEntity(
                "/auth/password/forgot", new ForgotPasswordRequest(usernameA, idCompanyB), ForgotPasswordResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"auth\".\"password_reset_tokens\" WHERE \"id_user\" = CAST(? AS bigint)",
                Integer.class, idUserA);
        assertThat(count).isZero();
    }

    @Test
    void forgotConIdCompanyCorrectoSiGeneraToken() {
        ResponseEntity<ForgotPasswordResponse> response = restTemplate.postForEntity(
                "/auth/password/forgot", new ForgotPasswordRequest(usernameA, idCompanyA), ForgotPasswordResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"auth\".\"password_reset_tokens\" WHERE \"id_user\" = CAST(? AS bigint) AND \"id_company\" = ?",
                Integer.class, idUserA, idCompanyA);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void forgotPasswordRespondeSinErrorDeContextoConUsuarioInexistente() {
        var req = new ForgotPasswordRequest("usuario-inexistente", "empresa-x");

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/password/forgot", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
    }

    @Test
    void resetPasswordRespondeSinErrorDeContextoConTokenInvalido() {
        var req = new ResetPasswordRequest("token-invalido", "Nueva#Clave123");

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/password/reset", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    /**
     * Único punto real que seguía devolviendo 500 en vez de 404: UserStoreImpl.findByIdUser
     * lanzaba jakarta.persistence.EntityNotFoundException sin traducir. El token de reset es
     * válido (existe, no expiró) pero apunta a un id_user que no existe en "user".
     */
    @Test
    void resetConTokenValidoQueApuntaAUsuarioInexistenteDevuelve404() {
        // id_user de password_reset_tokens es bigint (FK a users.id_user): para simular "el
        // token apunta a un usuario que ya no existe" hace falta un numero que no sea ningun
        // id real generado en este test, no un UUID (ya no seria ni siquiera un long valido).
        String idUserInexistente = String.valueOf(Math.abs(UUID.randomUUID().hashCode()) + 1_000_000_000L);
        String rawToken = UUID.randomUUID().toString();
        passwordResetTokenStore.save(idUserInexistente, "empresa-x", sha256Hex(rawToken), Instant.now().plusSeconds(600));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/auth/password/reset", new ResetPasswordRequest(rawToken, "Nueva#Clave123"), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody().code()).isNull();
        assertThat(response.getBody().correlationId()).isNotBlank();
    }

    @Test
    void tokenInexistenteYTokenExpiradoDevuelvenElMismoCodeYMensajeAlCliente() {
        ResponseEntity<ErrorResponse> inexistente = restTemplate.postForEntity(
                "/auth/password/reset", new ResetPasswordRequest(UUID.randomUUID().toString(), "Nueva#Clave123"), ErrorResponse.class);

        String idUser = String.valueOf(Math.abs(UUID.randomUUID().hashCode()) + 1_000_000_000L);
        String rawTokenVencido = UUID.randomUUID().toString();
        passwordResetTokenStore.save(idUser, "empresa-x", sha256Hex(rawTokenVencido), Instant.now().minusSeconds(600));

        ResponseEntity<ErrorResponse> vencido = restTemplate.postForEntity(
                "/auth/password/reset", new ResetPasswordRequest(rawTokenVencido, "Nueva#Clave123"), ErrorResponse.class);

        assertThat(inexistente.getStatusCode()).isEqualTo(vencido.getStatusCode());
        assertThat(inexistente.getBody().code()).isEqualTo(vencido.getBody().code());
        assertThat(inexistente.getBody().message()).isEqualTo(vencido.getBody().message());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
