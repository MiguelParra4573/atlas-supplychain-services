package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.2.1 -- integracion de UserController contra H2 (mismo perfil de test que
 * AuthPasswordResetFlowIntegrationTest), sin Testcontainers: crea usuario y rol vía JDBC directo
 * y ejercita las rutas leidas/actualizadas/borradas por los 6 casos de uso.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private String idUserA;
    private long idRoleA;
    private String usernameA;
    private String tokenA;

    private String idCompanyB;
    private String idUserB;

    private record Seed(String idUser, long idRole, String username) {}

    @BeforeEach
    void sembrarDosEmpresas() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        Seed seedA = seedUsuario(idCompanyA, "usuarioA-" + UUID.randomUUID() + "@test.com", hash);
        idUserA = seedA.idUser();
        idRoleA = seedA.idRole();
        usernameA = seedA.username();
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompanyA, idRoleA);

        idCompanyB = UUID.randomUUID().toString();
        idUserB = seedUsuario(idCompanyB, "usuarioB-" + UUID.randomUUID() + "@test.com", hash).idUser();

        TokenResponse login = restTemplate.postForObject(
                "/auth/login",
                new LoginRequest(seedA.username(), "Test#1234"),
                TokenResponse.class);
        tokenA = login.getAccessToken();
    }

    private Seed seedUsuario(String idCompany, String email, String hash) {
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
                            "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)", new String[]{"id_user"});
            ps.setString(1, idCompany);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setString(4, hash);
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);
        String idUser = String.valueOf(userKeyHolder.getKey().longValue());

        return new Seed(idUser, idRole, username);
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> withAuth(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void obtenerUsuarioPorIdDevuelve200ParaUsuarioDeMiEmpresa() {
        ResponseEntity<UserDTO> response = restTemplate.exchange(
                "/usuarios/" + idUserA, HttpMethod.GET, withAuth(), UserDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdUser()).isEqualTo(idUserA);
    }

    @Test
    void obtenerUsuarioPorIdDevuelve404ParaUsuarioDeOtraEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios/" + idUserB, HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Spec 1.11 punto 4: @Validated a nivel de clase activa el @NotBlank ya presente en el
     * @PathVariable -- antes era inerte sin la anotación de clase.
     */
    /**
     * URI construida a mano (no vía exchange(String, ...)) porque RestTemplate re-codifica el
     * '%' de un path template ya codificado -- "%20" pasado como String se vuelve "%2520" y deja
     * de representar un espacio en el servidor.
     */
    @Test
    void obtenerUsuarioPorIdConIdEnBlancoDevuelve400YNo500() {
        java.net.URI uri = java.net.URI.create(restTemplate.getRootUri() + "/usuarios/%20");
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listarUsuariosDeMiEmpresaDevuelveSoloLosDeMiEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idUser\":\"" + idUserA + "\"");
        assertThat(response.getBody()).doesNotContain("\"idUser\":\"" + idUserB + "\"");
    }

    @Test
    void actualizarUsuarioDevuelve202YPersisteElCambio() {
        UserDTO dto = UserDTO.builder()
                .idUser(idUserA)
                .username(usernameA)
                .name("Nuevo")
                .lastName("Nombre")
                .email("nuevo-" + UUID.randomUUID() + "@test.com")
                .state(true)
                .idRole(idRoleA)
                .build();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/usuarios", HttpMethod.PATCH, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        String name = DataAccessUtils.requiredSingleResult(jdbcTemplate.queryForList(
                "SELECT \"name\" FROM \"auth\".\"users\" WHERE \"id_user\" = CAST(? AS bigint)", String.class, idUserA));
        assertThat(name).isEqualTo("Nuevo");
    }

    @Test
    void deleteDeUsuarioDeOtraEmpresaDevuelve404YNoBorraNada() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/usuarios/" + idUserB, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"users\" WHERE \"id_user\" = CAST(? AS bigint)", Integer.class, idUserB);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteDeMiPropioUsuarioDevuelve200YLoBorra() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/usuarios/" + idUserA, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"users\" WHERE \"id_user\" = CAST(? AS bigint)", Integer.class, idUserA);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void searchDevuelvePagedResultConLosUsuariosDeMiEmpresa() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios/search?page=0&size=10", HttpMethod.POST, withAuth(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"totalElements\"");
        assertThat(response.getBody()).contains("\"idUser\":\"" + idUserA + "\"");
        assertThat(response.getBody()).doesNotContain("\"idUser\":\"" + idUserB + "\"");
    }
}
