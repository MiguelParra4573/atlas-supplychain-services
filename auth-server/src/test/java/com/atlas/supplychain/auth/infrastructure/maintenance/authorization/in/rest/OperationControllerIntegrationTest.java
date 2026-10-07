package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.OperationDTO;
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
 * Spec 1.2.2 -- regresión de contrato REST de OperationController contra H2: mismas
 * rutas/códigos de estado que antes de extraer los casos de uso, más el cambio de forma
 * acordado en /search (Page -> PagedResult).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class OperationControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private long idModuleA;
    private long idOperationA;
    private String resourceA;
    private String tokenA;

    @BeforeEach
    void sembrarEmpresaConUsuario() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        long idRoleA = seedRole(idCompanyA, "Admin-" + idCompanyA);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompanyA, idRoleA);
        SeedUsuario seedUsuarioA = seedUsuario(idCompanyA, idRoleA, hash);
        idModuleA = seedModule(idCompanyA, "MOD-" + UUID.randomUUID());
        resourceA = "RES-" + UUID.randomUUID();
        idOperationA = seedOperation(idCompanyA, idModuleA, resourceA, "TAG-" + UUID.randomUUID());

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
    void crearOperacionDevuelve201() {
        OperationDTO dto = OperationDTO.builder()
                .resource(resourceA).tag("NUEVO-" + UUID.randomUUID()).path("/nuevo").httpMethod("GET")
                .permitAll(false).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/operations", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crearOperacionConLaMismaAutoridadDevuelve409() {
        OperationDTO dto = OperationDTO.builder()
                .resource(resourceA)
                .tag(tagDe(idOperationA))
                .path("/otra").httpMethod("GET").permitAll(false).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/operations", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Spec 1.2: el mismo tag bajo otro recurso es una autoridad distinta, no un duplicado. El
     * vocabulario cerrado de tags (CREAR/EDITAR/...) se repite por diseño en cada recurso; antes
     * {@code existsByTag} miraba solo el tag y sin empresa, así que esto devolvía 409.
     */
    @Test
    void crearOperacionConElMismoTagEnOtroRecursoDevuelve201() {
        OperationDTO dto = OperationDTO.builder()
                .resource("OTRO-" + UUID.randomUUID())
                .tag(tagDe(idOperationA))
                .path("/otra").httpMethod("GET").permitAll(false).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/operations", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void asignarYDesasignarOperacionDeUnModuloDevuelve204YPersiste() {
        long otroModulo = seedModule(idCompanyA, "OTRO-" + UUID.randomUUID());

        ResponseEntity<Void> asignar = restTemplate.exchange(
                "/operations/" + idOperationA + "/modules/" + otroModulo, HttpMethod.POST, withAuth(), Void.class);

        assertThat(asignar.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Sigue colgando también del módulo original: es N:M, no un cambio de dueño.
        assertThat(contarAsignaciones(idOperationA)).isEqualTo(2);

        ResponseEntity<Void> desasignar = restTemplate.exchange(
                "/operations/" + idOperationA + "/modules/" + otroModulo, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(desasignar.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(contarAsignaciones(idOperationA)).isEqualTo(1);
    }

    @Test
    void asignarDosVecesLaMismaOperacionAlMismoModuloEsIdempotente() {
        long otroModulo = seedModule(idCompanyA, "OTRO-" + UUID.randomUUID());
        String url = "/operations/" + idOperationA + "/modules/" + otroModulo;

        restTemplate.exchange(url, HttpMethod.POST, withAuth(), Void.class);
        ResponseEntity<Void> segunda = restTemplate.exchange(url, HttpMethod.POST, withAuth(), Void.class);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(contarAsignaciones(idOperationA)).isEqualTo(2);
    }

    @Test
    void listarOperacionesSinModuloDevuelveSoloLasQueNoEstanEnNingunMenu() {
        long idSinModulo = seedOperationSinModulo(idCompanyA, "HUERFANA-" + UUID.randomUUID(), "CREAR");

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/operations/unassigned", HttpMethod.GET, withAuth(),
                new org.springframework.core.ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(fila -> ((Number) fila.get("idOperation")).longValue())
                .contains(idSinModulo)
                .doesNotContain(idOperationA);
    }

    private String tagDe(long idOperation) {
        return jdbcTemplate.queryForObject(
                "SELECT \"tag\" FROM \"auth\".\"operations\" WHERE \"id_operation\" = ?", String.class, idOperation);
    }

    private Integer contarAsignaciones(long idOperation) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"module_operation\" WHERE \"id_operation\" = ?",
                Integer.class, idOperation);
    }

    private long seedOperationSinModulo(String idCompany, String resource, String tag) {
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
        return keyHolder.getKey().longValue();
    }

    @Test
    void obtenerOperacionPorIdDevuelve200() {
        ResponseEntity<OperationDTO> response = restTemplate.exchange(
                "/operations/" + idOperationA, HttpMethod.GET, withAuth(), OperationDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdOperation()).isEqualTo(idOperationA);
    }

    @Test
    void actualizarOperacionDevuelve200YPersisteElCambio() {
        OperationDTO dto = OperationDTO.builder()
                .idOperation(idOperationA).resource(resourceA).tag("RENOMBRADO-" + UUID.randomUUID()).path("/renombrado")
                .httpMethod("POST").permitAll(true).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/operations", HttpMethod.PATCH, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String tag = jdbcTemplate.queryForObject(
                "SELECT \"tag\" FROM \"auth\".\"operations\" WHERE \"id_operation\" = ?", String.class, idOperationA);
        assertThat(tag).isEqualTo(dto.getTag());
    }

    @Test
    void eliminarOperacionConPermisosAsociadosDevuelve409YNoLaBorra() {
        long idRoleA = seedRole(idCompanyA, "OtroRol-" + UUID.randomUUID());
        seedPermiso(idCompanyA, idRoleA, idOperationA);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/operations/" + idOperationA, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"operations\" WHERE \"id_operation\" = ?", Integer.class, idOperationA);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void listarOperacionesPorModuloDevuelveLasDelModulo() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/operations/module/" + idModuleA, HttpMethod.POST, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idOperation\":" + idOperationA);
    }

    @Test
    void searchDevuelvePagedResultConLasOperacionesDeMiEmpresa() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/operations/search?page=0&size=10", HttpMethod.POST, withAuth(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"totalElements\"");
    }
}
