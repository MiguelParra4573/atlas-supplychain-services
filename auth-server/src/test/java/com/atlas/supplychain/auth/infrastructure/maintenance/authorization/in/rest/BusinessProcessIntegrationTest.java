package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.0 -- un proceso de negocio inactivo para la empresa corta el acceso a las pantallas de sus
 * módulos, aunque el usuario tenga concedido el permiso RBAC. Es el criterio de aceptación central
 * de la spec: no alcanza con que el menú deje de mostrarlo, porque el endpoint seguiría
 * respondiendo a quien lo llamara por fuera del menú.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class BusinessProcessIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String idCompany;
    private String token;
    private long idProceso;

    @BeforeEach
    void sembrar() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompany = UUID.randomUUID().toString();
        long idRole = seedRole(idCompany, "Admin-" + idCompany);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompany, idRole);
        // El catálogo base no incluye estos dos: los siembra el changelog 028, no el 004.
        // PROCESOS es el catálogo global (proveedor); PROCESOS_EMPRESA es lo que hace cada empresa.
        RbacSeeder.concederRecursos(jdbcTemplate, idCompany, idRole, "PROCESOS", "PROCESOS_EMPRESA");
        String username = seedUsuario(idCompany, idRole, hash);

        idProceso = seedProceso("VENTAS-" + UUID.randomUUID());

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, "Test#1234"), TokenResponse.class);
        token = login.getAccessToken();
    }

    private long seedRole(String company, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, company);
            ps.setString(2, name);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private String seedUsuario(String company, long idRole, String hash) {
        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", \"password_hash\", \"failed_attempts\", \"state\", \"id_role\") "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)",
                company, username, "user-" + UUID.randomUUID() + "@test.com", hash, idRole);
        return username;
    }

    private long seedProceso(String code) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"business_process\" (\"code\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_business_process"});
            ps.setString(1, code);
            ps.setString(2, "Proceso " + code);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** Cuelga del proceso el módulo que agrupa las operaciones de USUARIOS. */
    private void colgarModuloUsuariosDelProceso() {
        jdbcTemplate.update(
                "UPDATE \"auth\".\"modules\" SET \"id_business_process\" = ? WHERE \"id_company\" = ? AND \"name\" = 'USUARIOS'",
                idProceso, idCompany);
    }

    private void activarProceso(boolean active) {
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"company_process\" (\"id_company\", \"id_business_process\", \"active\", \"activated_at\") "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                idCompany, idProceso, active);
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> withAuthBody(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    private HttpStatus listarUsuarios() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios", HttpMethod.GET, withAuth(), String.class);
        return HttpStatus.valueOf(response.getStatusCode().value());
    }

    @Test
    void sinProcesoAsociadoElModuloAutorizaNormalmente() {
        assertThat(listarUsuarios()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void procesoActivoParaLaEmpresaAutoriza() {
        colgarModuloUsuariosDelProceso();
        activarProceso(true);

        assertThat(listarUsuarios()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void procesoDesactivadoCortaElAccesoAunqueElPermisoSigaConcedido() {
        colgarModuloUsuariosDelProceso();
        activarProceso(false);

        assertThat(listarUsuarios()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Un proceso que la empresa nunca contrató no tiene fila en {@code company_process}. No
     * contratado y desactivado son lo mismo de cara al acceso: si esto autorizara, colgar una
     * pantalla de un proceso sin vender la dejaría abierta.
     */
    @Test
    void procesoNuncaContratadoPorLaEmpresaTampocoAutoriza() {
        colgarModuloUsuariosDelProceso();

        assertThat(listarUsuarios()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * El catálogo es **global**, sin `id_company`: lo administra el proveedor del ERP. Esto se
     * escapó al primer intento -- el repositorio heredaba el guard de tenant de
     * `AbstractQueryDslRepository`, que exige `idCompany` en la entidad raíz, y cada llamada moría
     * con 500 (`TenantScopeRequiredException`). Los tests de acceso no lo detectaban porque nunca
     * llegaban a listar el catálogo.
     */
    @Test
    void listarElCatalogoDevuelve200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/business-processes", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idBusinessProcess\":" + idProceso);
    }

    @Test
    void elCatalogoDeMiEmpresaTraeElEstadoDeActivacion() {
        activarProceso(true);

        ResponseEntity<String> response = restTemplate.exchange(
                "/business-processes/my-company", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"active\":true");
    }

    /** Un proceso que la empresa nunca contrató sale con active=false, no ausente. */
    @Test
    void unProcesoNoContratadoSaleComoInactivoParaMiEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/business-processes/my-company", HttpMethod.GET, withAuth(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idBusinessProcess\":" + idProceso);
        assertThat(response.getBody()).contains("\"active\":false");
    }

    @Test
    void activarPorEndpointQuedaPersistidoParaLaEmpresaDelToken() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/business-processes/" + idProceso + "/activation",
                HttpMethod.PUT, withAuthBody("{\"active\":true}"), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer activos = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"company_process\" WHERE \"id_company\" = ? AND \"active\" = true",
                Integer.class, idCompany);
        assertThat(activos).isEqualTo(1);
    }

    /**
     * La empresa sale del token y nunca de la URL. Si viniera por path, el administrador de una
     * empresa podría apagarle un proceso contratado a otra -- y como desactivar corta acceso, eso
     * deja a los usuarios de la otra en 403 sobre pantallas que sí pagaron.
     */
    @Test
    void activarSoloAfectaALaEmpresaDelToken() {
        String otraEmpresa = UUID.randomUUID().toString();

        restTemplate.exchange("/business-processes/" + idProceso + "/activation",
                HttpMethod.PUT, withAuthBody("{\"active\":true}"), Void.class);

        Integer enLaOtra = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"company_process\" WHERE \"id_company\" = ?",
                Integer.class, otraEmpresa);
        Integer enLaMia = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"company_process\" WHERE \"id_company\" = ?",
                Integer.class, idCompany);

        assertThat(enLaOtra).isZero();
        assertThat(enLaMia).isEqualTo(1);
    }

    @Test
    void crearProcesoConCodigoRepetidoDevuelve409() {
        String body = "{\"code\":\"DUPLICADO-" + idProceso + "\",\"name\":\"Uno\",\"state\":true}";

        ResponseEntity<String> primera = restTemplate.exchange(
                "/business-processes", HttpMethod.POST, withAuthBody(body), String.class);
        ResponseEntity<String> segunda = restTemplate.exchange(
                "/business-processes", HttpMethod.POST, withAuthBody(body), String.class);

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elMenuNoMuestraLosModulosDeUnProcesoInactivo() {
        colgarModuloUsuariosDelProceso();
        activarProceso(false);

        ResponseEntity<String> menu = restTemplate.exchange("/menu", HttpMethod.GET, withAuth(), String.class);

        assertThat(menu.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(menu.getBody()).doesNotContain("\"name\":\"USUARIOS\"");
        assertThat(menu.getBody()).contains("\"name\":\"ROLES\"");
    }
}
