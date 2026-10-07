package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ModuleDTO;
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
 * Spec 1.2.2 -- regresión de contrato REST de ModuleController contra H2: mismas
 * rutas/códigos de estado que antes de extraer los casos de uso, más el cambio de forma
 * acordado en /search (Page -> PagedResult).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ModuleControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompanyA;
    private long idModuleA;
    private String tokenA;

    @BeforeEach
    void sembrarEmpresaConUsuario() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        long idRoleA = seedRole(idCompanyA, "Admin-" + idCompanyA);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompanyA, idRoleA);
        SeedUsuario seedUsuarioA = seedUsuario(idCompanyA, idRoleA, hash);
        idModuleA = seedModule(idCompanyA, "MOD-" + UUID.randomUUID());

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
    void crearModuloDevuelve201() {
        ModuleDTO dto = ModuleDTO.builder()
                .name("Nuevo-" + UUID.randomUUID()).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/modules", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void obtenerModuloPorIdDevuelve200() {
        ResponseEntity<ModuleDTO> response = restTemplate.exchange(
                "/modules/" + idModuleA, HttpMethod.GET, withAuth(), ModuleDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdModule()).isEqualTo(idModuleA);
    }

    @Test
    void obtenerModuloPorIdDevuelve404SiNoExisteEnMiEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/999999", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarModuloDevuelve200YPersisteElCambio() {
        ModuleDTO dto = ModuleDTO.builder()
                .idModule(idModuleA).name("Renombrado-" + UUID.randomUUID()).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/modules", HttpMethod.PATCH, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = jdbcTemplate.queryForObject(
                "SELECT \"name\" FROM \"auth\".\"modules\" WHERE \"id_module\" = ?", String.class, idModuleA);
        assertThat(name).isEqualTo(dto.getName());
    }

    /**
     * Spec 1.2: tener operaciones asignadas dejó de bloquear la baja. Antes era una dependencia
     * real (la operación colgaba del módulo con una FK y se habría quedado huérfana, de ahí el
     * 409); ahora es solo agrupación de menú, así que se sueltan las asignaciones y las
     * operaciones **sobreviven** al módulo -- son catálogo RBAC de un microservicio, no datos del
     * módulo. Borrar un módulo no puede borrar autoridades.
     */
    @Test
    void eliminarModuloConOperacionesAsignadasLoBorraYDejaLasOperacionesSinModulo() {
        long idOperation = seedOperation(idCompanyA, idModuleA, "TAG-" + UUID.randomUUID());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/modules/" + idModuleA, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(contarModulos(idModuleA)).isZero();
        assertThat(contarAsignaciones(idOperation)).isZero();
        Integer operacionesVivas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"operations\" WHERE \"id_operation\" = ?", Integer.class, idOperation);
        assertThat(operacionesVivas).isEqualTo(1);
    }

    private Integer contarModulos(long idModule) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"modules\" WHERE \"id_module\" = ?", Integer.class, idModule);
    }

    private Integer contarAsignaciones(long idOperation) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"module_operation\" WHERE \"id_operation\" = ?",
                Integer.class, idOperation);
    }

    @Test
    void listarModulosPorStateDevuelveLosActivos() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/estado/true", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idModule\":" + idModuleA);
    }

    /**
     * Spec 1.2: desactivar el módulo corta el acceso a sus operaciones de verdad, no solo las
     * saca del menú -- si no, el endpoint seguiría respondiendo a quien lo llamara por fuera
     * (Postman, un enlace guardado) y la pantalla de módulos estaría mintiendo. El corte es
     * inmediato con el token ya emitido: el filtro JWT recarga las autoridades desde la base en
     * cada petición, no las lee del token.
     */
    @Test
    void desactivarElModuloQueAgrupaUnaOperacionCortaElAccesoConElMismoToken() {
        assertThat(restTemplate.exchange("/modules/estado/true", HttpMethod.GET, withAuth(), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        desactivarModuloPorNombre(idCompanyA, "MODULOS");

        ResponseEntity<String> despues = restTemplate.exchange(
                "/modules/estado/true", HttpMethod.GET, withAuth(), String.class);

        assertThat(despues.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * La otra mitad de la regla: basta con que quede un módulo activo. La misma operación colgada
     * de dos agrupaciones sigue autorizando aunque se apague una.
     */
    @Test
    void desactivarSoloUnoDeLosDosModulosDeUnaOperacionNoCortaElAcceso() {
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"module_operation\" (\"id_module\", \"id_operation\", \"id_company\") "
                        + "SELECT ?, \"id_operation\", ? FROM \"auth\".\"operations\" "
                        + "WHERE \"id_company\" = ? AND \"resource\" = 'MODULOS'",
                idModuleA, idCompanyA, idCompanyA);

        desactivarModuloPorNombre(idCompanyA, "MODULOS");

        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/estado/true", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void desactivarModuloPorNombre(String idCompany, String name) {
        int filas = jdbcTemplate.update(
                "UPDATE \"auth\".\"modules\" SET \"state\" = false WHERE \"id_company\" = ? AND \"name\" = ?",
                idCompany, name);
        assertThat(filas).isEqualTo(1);
    }

    @Test
    void searchDevuelvePagedResultConLosModulosDeMiEmpresa() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/search?page=0&size=10", HttpMethod.POST, withAuth(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"totalElements\"");
    }
}
