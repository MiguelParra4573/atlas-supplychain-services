package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.RoleDTO;
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
 * Spec 1.2.2 -- regresión de contrato REST de RoleController contra H2 (mismo patrón que
 * UserControllerIntegrationTest de la Spec 1.2.1): mismas rutas/códigos de estado que antes
 * de extraer los casos de uso, más el cambio de forma acordado en /search (Page -> PagedResult).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RoleControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private long idRoleA;
    private String tokenA;

    private String idCompanyB;
    private long idRoleB;

    @BeforeEach
    void sembrarDosEmpresas() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        idRoleA = seedRole(idCompanyA, "Admin-" + idCompanyA);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompanyA, idRoleA);
        SeedUsuario seedUsuarioA = seedUsuario(idCompanyA, idRoleA, hash);

        idCompanyB = UUID.randomUUID().toString();
        idRoleB = seedRole(idCompanyB, "Admin-" + idCompanyB);

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
    private long seedOperation(String idCompany, long idModule, String tag) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"operations\" (\"id_company\", \"resource\", \"tag\", \"path\", \"http_method\", \"permit_all\", \"state\") " +
                            "VALUES (?, ?, ?, ?, 'GET', false, true)", new String[]{"id_operation"});
            ps.setString(1, idCompany);
            ps.setString(2, "RES-" + tag);
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

    private void seedPermiso(String idCompany, long idRole, long idOperation) {
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"granted_permissions\" (\"id_company\", \"id_role\", \"id_operation\") VALUES (?, ?, ?)",
                idCompany, idRole, idOperation);
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
    void crearRolDevuelve201() {
        RoleDTO dto = RoleDTO.builder().idCompany(idCompanyA).name("Ventas-" + UUID.randomUUID()).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/roles", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crearRolConNombreDuplicadoEnLaMismaEmpresaDevuelve409() {
        RoleDTO dto = RoleDTO.builder().idCompany(idCompanyA).name("Admin-" + idCompanyA).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/roles", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void obtenerRolPorIdDevuelve200ParaRolDeMiEmpresa() {
        ResponseEntity<RoleDTO> response = restTemplate.exchange(
                "/roles/" + idRoleA, HttpMethod.GET, withAuth(), RoleDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdRole()).isEqualTo(idRoleA);
    }

    @Test
    void obtenerRolPorIdDevuelve404ParaRolDeOtraEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/roles/" + idRoleB, HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarRolDevuelve200YPersisteElCambio() {
        RoleDTO dto = RoleDTO.builder().idRole(idRoleA).idCompany(idCompanyA).name("Renombrado-" + UUID.randomUUID()).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/roles", HttpMethod.PATCH, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = jdbcTemplate.queryForObject(
                "SELECT \"name\" FROM \"auth\".\"roles\" WHERE \"id_rol\" = ?", String.class, idRoleA);
        assertThat(name).isEqualTo(dto.getName());
    }

    @Test
    void eliminarRolConPermisosAsociadosDevuelve409YNoLoBorra() {
        long idModule = seedModule(idCompanyA, "MOD-" + UUID.randomUUID());
        long idOperation = seedOperation(idCompanyA, idModule, "TAG-" + UUID.randomUUID());
        seedPermiso(idCompanyA, idRoleA, idOperation);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/roles/" + idRoleA, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"roles\" WHERE \"id_rol\" = ?", Integer.class, idRoleA);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listarRolesDeMiEmpresaDevuelveSoloLosDeMiEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/roles", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idRole\":" + idRoleA);
        assertThat(response.getBody()).doesNotContain("\"idRole\":" + idRoleB);
    }

    /**
     * Spec 1.2: el resumen del menú se deduce de los permisos concedidos. El catálogo sembrado da
     * 5 módulos con 5 operaciones cada uno, todas concedidas al rol, así que cada módulo cuenta
     * **1 pantalla** -- los 5 tags comparten el mismo `resource`, y lo que se cuenta son pantallas,
     * no operaciones.
     */
    @Test
    void resumenDeMenuCuentaPantallasDistintasNoOperaciones() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/roles/menu-summary", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idRole\":" + idRoleA);
        assertThat(response.getBody()).contains("\"screenCount\":1");
        assertThat(response.getBody()).doesNotContain("\"screenCount\":5");
    }

    @Test
    void resumenDeMenuNoFiltraPorEmpresaAjena() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/roles/menu-summary", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("\"idRole\":" + idRoleB);
    }

    /**
     * Un módulo desactivado no se pinta en el menú, así que tampoco cuenta en el resumen. Se apaga
     * solo USUARIOS y no todo el catálogo: desde la Spec 1.2 esconder un módulo corta el acceso a
     * sus operaciones, así que apagarlos todos le quitaría a este mismo usuario el
     * {@code ROLES:LISTAR} que el endpoint exige, y la respuesta sería 403 en vez de 200.
     */
    @Test
    void resumenDeMenuIgnoraModulosDesactivados() {
        ResponseEntity<String> antes = restTemplate.exchange(
                "/roles/menu-summary", HttpMethod.GET, withAuth(), String.class);
        assertThat(antes.getBody()).contains("\"name\":\"USUARIOS\"");

        jdbcTemplate.update(
                "UPDATE \"auth\".\"modules\" SET \"state\" = false WHERE \"id_company\" = ? AND \"name\" = 'USUARIOS'",
                idCompanyA);

        ResponseEntity<String> despues = restTemplate.exchange(
                "/roles/menu-summary", HttpMethod.GET, withAuth(), String.class);

        assertThat(despues.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(despues.getBody()).doesNotContain("\"name\":\"USUARIOS\"");
        assertThat(despues.getBody()).contains("\"name\":\"ROLES\"");
    }

    @Test
    void searchDevuelvePagedResultConLosRolesDeMiEmpresa() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/roles/search?page=0&size=10", HttpMethod.POST, withAuth(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"totalElements\"");
    }
}
