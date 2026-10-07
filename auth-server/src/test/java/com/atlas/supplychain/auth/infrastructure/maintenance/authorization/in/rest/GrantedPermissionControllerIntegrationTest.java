package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.GrantedPermissionDTO;
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
import org.springframework.http.MediaType;
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
 * Spec 1.2.2 -- regresión de contrato REST de GrantedPermissionController contra H2: mismas
 * rutas/códigos de estado que antes de extraer los casos de uso, más el cambio de forma
 * acordado en /search (Page -> PagedResult). "Actualizar" queda implementado como
 * revocar-y-reasignar (ver riesgo documentado en la spec).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class GrantedPermissionControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private long idRoleA;
    private long idModuleA;
    private long idOperationA;
    private long idGrantedPermissionA;
    private String tokenA;

    @BeforeEach
    void sembrarEmpresaConPermisoConcedido() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        idRoleA = seedRole(idCompanyA, "Admin-" + idCompanyA);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompanyA, idRoleA);
        SeedUsuario seedUsuarioA = seedUsuario(idCompanyA, idRoleA, hash);
        idModuleA = seedModule(idCompanyA, "MOD-" + UUID.randomUUID());
        idOperationA = seedOperation(idCompanyA, idModuleA, "RES-" + UUID.randomUUID(), "TAG-" + UUID.randomUUID());
        idGrantedPermissionA = seedPermiso(idCompanyA, idRoleA, idOperationA);

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

    private long seedModule(String idCompany, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"modules\" (\"id_company\", \"name\", \"state\", \"display_order\") VALUES (?, ?, true, 0)", new String[]{"id_module"});
            ps.setString(1, idCompany);
            ps.setString(2, name);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** Siembra la operación y la cuelga del módulo por la tabla puente (Spec 1.2). */
    private long seedOperation(String idCompany, long idModule, String resource, String tag) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"operations\" (\"id_company\", \"resource\", \"tag\", \"path\", \"http_method\", \"permit_all\", \"state\") " +
                            "VALUES (?, ?, ?, ?, 'GET', false, true)", new String[]{"id_operation"});
            ps.setString(1, idCompany);
            ps.setString(2, resource);
            ps.setString(3, tag);
            ps.setString(4, "/" + tag);
            return ps;
        }, keyHolder);
        long idOperation = keyHolder.getKey().longValue();
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"module_operation\" (\"id_module\", \"id_operation\", \"id_company\") VALUES (?, ?, ?)",
                idModule, idOperation, idCompany);
        return idOperation;
    }

    private long seedPermiso(String idCompany, long idRole, long idOperation) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"granted_permissions\" (\"id_company\", \"id_role\", \"id_operation\") VALUES (?, ?, ?)", new String[]{"id_granted_permission"});
            ps.setString(1, idCompany);
            ps.setLong(2, idRole);
            ps.setLong(3, idOperation);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private <T> HttpEntity<T> withAuth(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        return new HttpEntity<>(headers);
    }

    @Test
    void crearPermisoDevuelve201() {
        long idOperationNueva = seedOperation(idCompanyA, idModuleA, "RES-" + UUID.randomUUID(), "TAG-" + UUID.randomUUID());
        GrantedPermissionDTO dto = GrantedPermissionDTO.builder().idRole(idRoleA).idOperation(idOperationNueva).build();

        ResponseEntity<Void> response = restTemplate.exchange("/permissions", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crearPermisoYaConcedidoDevuelve409() {
        GrantedPermissionDTO dto = GrantedPermissionDTO.builder().idRole(idRoleA).idOperation(idOperationA).build();

        ResponseEntity<Void> response = restTemplate.exchange("/permissions", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void obtenerPermisoPorIdDevuelve200() {
        ResponseEntity<GrantedPermissionDTO> response = restTemplate.exchange(
                "/permissions/" + idGrantedPermissionA, HttpMethod.GET, withAuth(), GrantedPermissionDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdOperation()).isEqualTo(idOperationA);
        assertThat(response.getBody().getIdRole()).isEqualTo(idRoleA);
        assertThat(response.getBody().getRoleName()).isEqualTo("Admin-" + idCompanyA);
        // `resource` y no el nombre del módulo (Spec 1.2): la matriz de permisos se agrupa por la
        // identidad estable de la operación.
        assertThat(response.getBody().getResource()).startsWith("RES-");
    }

    @Test
    void eliminarPermisoDevuelve200YLoBorra() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/permissions/" + idGrantedPermissionA, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"granted_permissions\" WHERE \"id_granted_permission\" = ?",
                Integer.class, idGrantedPermissionA);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void eliminarPermisoInexistenteDevuelve404() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/permissions/999999", HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listarPermisosPorRolDevuelveLosDelRol() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/permissions/role/" + idRoleA, HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idOperation\":" + idOperationA);
        assertThat(response.getBody()).contains("\"idRole\":" + idRoleA);
        assertThat(response.getBody()).contains("\"roleName\":\"Admin-" + idCompanyA + "\"");
    }

    @Test
    void searchDevuelvePagedResultConLosPermisosDeMiEmpresa() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/permissions/search?page=0&size=10", HttpMethod.POST, withAuth(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"totalElements\"");
    }
}
