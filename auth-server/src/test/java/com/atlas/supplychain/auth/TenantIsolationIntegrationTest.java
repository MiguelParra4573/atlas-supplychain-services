package com.atlas.supplychain.auth;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.1 -- prueba de aislamiento de tenant contra Postgres real (no mockeado): dos
 * empresas sembradas, un usuario autenticado de la Empresa A no puede leer ni borrar
 * recursos de la Empresa B en ninguno de los 5 agregados, y las rutas
 * /usuarios|roles/empresa/{idCompany} ya no existen.
 */
@Testcontainers
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("liquibase-it")
class TenantIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withInitScript("init-auth-schema.sql");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenEmpresaA;

    private String idUserB;
    private String idCompanyB;
    private Long idRoleB;
    private Long idModuleB;
    private Long idOperationB;
    private Long idPermissionB;

    @BeforeEach
    void sembrarDosEmpresas() {
        String hash = passwordEncoder.encode("Test#1234");

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        idCompanyB = "empresaB-" + sufijo;
        Seed seedA = seedEmpresa("empresaA-" + sufijo, "usuarioA-" + sufijo + "@test.com", hash);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, "empresaA-" + sufijo, seedA.idRole);
        Seed seedB = seedEmpresa(idCompanyB, "usuarioB-" + sufijo + "@test.com", hash);
        idUserB = seedB.idUser;
        idRoleB = seedB.idRole;
        idModuleB = seedB.idModule;
        idOperationB = seedB.idOperation;
        idPermissionB = seedB.idPermission;

        TokenResponse loginA = restTemplate.postForObject(
                "/auth/login", new LoginRequest(seedA.username, "Test#1234"), TokenResponse.class);
        tokenEmpresaA = loginA.getAccessToken();
    }

    private record Seed(String username, String idUser, Long idRole, Long idModule, Long idOperation, Long idPermission) {}

    private Seed seedEmpresa(String idCompany, String email, String hash) {
        Long idRole = jdbcTemplate.queryForObject(
                "INSERT INTO auth.roles (id_company, name, state) VALUES (?, ?, true) RETURNING id_rol",
                Long.class, idCompany, "Admin-" + idCompany);

        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        Long idUserGenerado = jdbcTemplate.queryForObject(
                "INSERT INTO auth.users (id_company, username, name, last_name, email, password_hash, failed_attempts, state, id_role) " +
                        "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?) RETURNING id_user",
                Long.class, idCompany, username, email, hash, idRole);
        String idUser = String.valueOf(idUserGenerado);

        Long idModule = jdbcTemplate.queryForObject(
                "INSERT INTO auth.modules (id_company, name, state) VALUES (?, ?, true) RETURNING id_module",
                Long.class, idCompany, "Modulo-" + idCompany);

        Long idOperation = jdbcTemplate.queryForObject(
                "INSERT INTO auth.operations (id_company, http_method, path, permit_all, state, resource, tag) " +
                        "VALUES (?, 'GET', '/x', false, true, ?, ?) RETURNING id_operation",
                Long.class, idCompany, "Recurso-" + idCompany, "CONSULTAR");

        jdbcTemplate.update(
                "INSERT INTO auth.module_operation (id_module, id_operation, id_company) VALUES (?, ?, ?)",
                idModule, idOperation, idCompany);

        Long idPermission = jdbcTemplate.queryForObject(
                "INSERT INTO auth.granted_permissions (id_company, id_operation, id_role) VALUES (?, ?, ?) RETURNING id_granted_permission",
                Long.class, idCompany, idOperation, idRole);

        return new Seed(username, idUser, idRole, idModule, idOperation, idPermission);
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenEmpresaA);
        return new HttpEntity<>(headers);
    }

    @Test
    void usuarioDeEmpresaARecibe404AlPedirRecursosDeEmpresaB() {
        assertThat(get("/usuarios/" + idUserB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/roles/" + idRoleB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/modules/" + idModuleB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/operations/" + idOperationB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/permissions/" + idPermissionB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCruzadoNoBorraYResponde404() {
        assertThat(restTemplate.exchange("/usuarios/" + idUserB, org.springframework.http.HttpMethod.DELETE, withAuth(), Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM auth.users WHERE id_user = CAST(? AS bigint)", Integer.class, Long.valueOf(idUserB));
        assertThat(count).isEqualTo(1);
    }

    @Test
    void rutasViejasPorEmpresaYaNoExisten() {
        assertThat(get("/usuarios/empresa/" + idCompanyB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/roles/empresa/" + idCompanyB).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUsuariosDevuelveSoloLosDeMiEmpresa() {
        ResponseEntity<String> response = get("/usuarios");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("\"idUser\":\"" + idUserB + "\"");
    }

    private ResponseEntity<String> get(String path) {
        return restTemplate.exchange(path, org.springframework.http.HttpMethod.GET, withAuth(), String.class);
    }
}
