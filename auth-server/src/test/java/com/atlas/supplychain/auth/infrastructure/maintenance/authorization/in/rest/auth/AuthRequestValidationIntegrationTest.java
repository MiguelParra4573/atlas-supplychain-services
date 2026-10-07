package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.AuthenticationService;
import com.atlas.supplychain.auth.application.security.PasswordResetService;
import com.atlas.supplychain.auth.application.security.PasswordService;
import com.atlas.supplychain.auth.application.security.contracts.AccessTokenClaims;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.11 punto 3: los 6 endpoints de AuthController deben rechazar con 400 un body con el
 * campo obligatorio vacío, sin invocar el caso de uso correspondiente. Se mockean los 3 services
 * de aplicación para verificar cero interacciones -- si @Valid no cortara la petición, el service
 * se invocaría igual.
 *
 * El JWT usado para los endpoints autenticados (logout, change-password) se genera directo con
 * JwtService, sin pasar por POST /auth/login -- AuthenticationService.login() está mockeado.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthRequestValidationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private PasswordService passwordService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    private String token;

    @BeforeEach
    void sembrarUsuarioYGenerarToken() {
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
            ps.setString(4, passwordEncoder.encode("Test#1234"));
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);
        String idUser = String.valueOf(userKeyHolder.getKey().longValue());

        token = jwtService.generarAccessToken(new AccessTokenClaims(idUser, idCompany, List.of(), List.of()));
    }

    static Stream<Arguments> bodiesInvalidosPorEndpoint() {
        return Stream.of(
                Arguments.of("/auth/login", "{\"usuario\":\"\",\"contrasena\":\"x\"}"),
                Arguments.of("/auth/login", "{\"usuario\":\"x\",\"contrasena\":\"\"}"),
                Arguments.of("/auth/refresh", "{\"refreshToken\":\"\"}"),
                Arguments.of("/auth/refresh", "{}"),
                Arguments.of("/auth/logout", "{\"refreshToken\":\"\"}"),
                Arguments.of("/auth/change-password", "{\"currentPassword\":\"\",\"newPassword\":\"Nueva#123456\"}"),
                Arguments.of("/auth/change-password", "{\"currentPassword\":\"Actual#123\",\"newPassword\":\"\"}"),
                Arguments.of("/auth/password/forgot", "{\"usuario\":\"\",\"idCompany\":\"empresa-x\"}"),
                Arguments.of("/auth/password/forgot", "{\"usuario\":\"user-x\",\"idCompany\":\"\"}"),
                Arguments.of("/auth/password/reset", "{\"resetToken\":\"\",\"newPassword\":\"Nueva#123456\"}"),
                Arguments.of("/auth/password/reset", "{\"resetToken\":\"token-x\",\"newPassword\":\"\"}")
        );
    }

    @ParameterizedTest(name = "{0} con campo obligatorio vacío devuelve 400 y no invoca el service")
    @MethodSource("bodiesInvalidosPorEndpoint")
    void campoObligatorioVacioDevuelve400YNoInvocaElCasoDeUso(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(authenticationService, passwordService, passwordResetService);
    }

    @Test
    void changePasswordSinAutenticarDevuelve401AntesQueValidacionDeBodyYNoInvocaElCasoDeUso() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/change-password", new HttpEntity<>("{\"currentPassword\":\"\",\"newPassword\":\"\"}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(authenticationService, passwordService, passwordResetService);
    }
}
