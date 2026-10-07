package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.18 -- contrato REST de {@code GET /menu} contra H2: 200 con el árbol para un usuario
 * autenticado, 401 sin token (heredado del filtro JWT existente, sin cambios en SecurityConfig).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class MenuControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private String tokenA;

    @BeforeEach
    void sembrarEmpresaConUsuarioYPermisos() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        long idRoleA = seedRole(idCompanyA, "Admin-" + idCompanyA);

        // concederUnaAutoridad siembra un Module nuevo por llamada -- una autoridad por
        // módulo alcanza para probar el contrato REST (200/orden), el detalle de "varias
        // operaciones en el mismo módulo" ya está cubierto en MenuServiceImplTest.
        RbacSeeder.concederUnaAutoridad(jdbcTemplate, idCompanyA, idRoleA, "USUARIOS", "CREAR");
        RbacSeeder.concederUnaAutoridad(jdbcTemplate, idCompanyA, idRoleA, "ROLES", "LISTAR");

        SeedUsuario seedUsuarioA = seedUsuario(idCompanyA, idRoleA, hash);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(seedUsuarioA.username(), "Test#1234"), TokenResponse.class);
        tokenA = login.getAccessToken();
    }

    private long seedRole(String idCompany, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, name);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private record SeedUsuario(String idUser, String username) {}

    private SeedUsuario seedUsuario(String idCompany, long idRole, String hash) {
        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") " +
                            "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, "user-" + UUID.randomUUID() + "@test.com");
            ps.setString(4, hash);
            ps.setLong(5, idRole);
            return ps;
        }, keyHolder);
        String idUser = String.valueOf(keyHolder.getKey().longValue());
        return new SeedUsuario(idUser, username);
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        return new HttpEntity<>(headers);
    }

    @Test
    void obtenerMenuDevuelve200ConElArbolDeModulosYOperacionesConcedidas() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/menu", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"ROLES\"");
        assertThat(response.getBody()).contains("\"name\":\"USUARIOS\"");
        assertThat(response.getBody()).contains("\"tag\":\"CREAR\"");
        assertThat(response.getBody()).contains("\"tag\":\"LISTAR\"");
        // ROLES antes que USUARIOS -- orden alfabetico por name
        assertThat(response.getBody().indexOf("\"name\":\"ROLES\""))
                .isLessThan(response.getBody().indexOf("\"name\":\"USUARIOS\""));
    }

    @Test
    void obtenerMenuSinTokenDevuelve401() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/menu", HttpMethod.GET, null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
