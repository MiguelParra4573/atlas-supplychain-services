package com.atlas.supplychain.auth.infrastructure.maintenance.organization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization.AgencyDTO;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.17 -- {@code Agency} lleva {@code idCompany} obligatorio del principal (mismo patrón
 * que {@code RoleControllerIntegrationTest}): a diferencia de {@code Company}, sus lecturas
 * están scoped a la empresa del token.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AgencyControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private String idCompanyA;
    private String tokenA;
    private String idAgencySemilla;

    private String idCompanyB;
    private String idAgencyB;

    @BeforeEach
    void sembrarDosEmpresasConAgencias() {
        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = seedCompany();
        long idRoleA = seedRole(idCompanyA, "Admin-" + idCompanyA);
        RbacSeeder.concederRecursos(jdbcTemplate, idCompanyA, idRoleA, "AGENCIAS");
        SeedUsuario seedUsuarioA = seedUsuario(idCompanyA, idRoleA, hash);

        idCompanyB = seedCompany();
        idAgencyB = UUID.randomUUID().toString();
        seedAgency(idAgencyB, idCompanyB, "Agencia B");

        idAgencySemilla = UUID.randomUUID().toString();
        seedAgency(idAgencySemilla, idCompanyA, "Agencia Semilla");

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(seedUsuarioA.username(), "Test#1234"), TokenResponse.class);
        tokenA = login.getAccessToken();
    }

    private static String shortId(String prefix, int totalLen) {
        int digitLen = totalLen - prefix.length();
        return prefix + String.format("%0" + digitLen + "d", SEQ.incrementAndGet());
    }

    private String seedCompany() {
        long siguiente = jdbcTemplate.queryForObject("SELECT nextval('auth.company_id_seq')", Long.class);
        String idCompany = String.format("%02d", siguiente);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"company\" (\"id_company\", \"alias\", \"business_name\", \"ruc\", \"base_path\", \"cedula_repres_legal\", \"legal_rep_first_name\", \"legal_rep_last_name\", \"state\") " +
                        "VALUES (?, ?, ?, ?, '/x', '1234567890', 'Nombres', 'Apellidos', true)",
                idCompany, "alias-" + idCompany, "Business " + idCompany, "RUC" + idCompany);
        return idCompany;
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

    private void seedAgency(String idAgency, String idCompany, String name) {
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"agencies\" (\"id_agency\", \"id_company\", \"name\", \"state\") VALUES (?, ?, ?, true)",
                idAgency, idCompany, name);
    }

    private <T> HttpEntity<T> withAuth(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> withAuth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void listarAgenciesDeMiEmpresaSinTokenDevuelve401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/agencies", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void crearAgencyDevuelve201() {
        AgencyDTO dto = AgencyDTO.builder().name("Agencia Nueva").state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/agencies", HttpMethod.POST, withAuth(dto, tokenA), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crearAgencyConCompanyDelTokenInexistenteDevuelve404() {
        String hash = passwordEncoder.encode("Test#1234");
        String idCompanyHuerfana = UUID.randomUUID().toString();
        long idRole = seedRole(idCompanyHuerfana, "Admin-" + idCompanyHuerfana);
        RbacSeeder.concederRecursos(jdbcTemplate, idCompanyHuerfana, idRole, "AGENCIAS");
        SeedUsuario seedUsuarioHuerfana = seedUsuario(idCompanyHuerfana, idRole, hash);
        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(seedUsuarioHuerfana.username(), "Test#1234"), TokenResponse.class);

        AgencyDTO dto = AgencyDTO.builder().name("Agencia Sin Empresa").state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/agencies", HttpMethod.POST, withAuth(dto, login.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerAgencyPorIdDevuelve200ParaAgenciaDeMiEmpresa() {
        ResponseEntity<AgencyDTO> response = restTemplate.exchange(
                "/agencies/" + idAgencySemilla, HttpMethod.GET, withAuth(tokenA), AgencyDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdAgency()).isEqualTo(idAgencySemilla);
    }

    @Test
    void obtenerAgencyPorIdDeOtraEmpresaDevuelve404() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/agencies/" + idAgencyB, HttpMethod.GET, withAuth(tokenA), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarAgencyDevuelve200YPersisteElCambio() {
        AgencyDTO dto = AgencyDTO.builder().idAgency(idAgencySemilla).name("Renombrada-" + UUID.randomUUID()).state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/agencies", HttpMethod.PATCH, withAuth(dto, tokenA), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String name = jdbcTemplate.queryForObject(
                "SELECT \"name\" FROM \"auth\".\"agencies\" WHERE \"id_agency\" = ?", String.class, idAgencySemilla);
        assertThat(name).isEqualTo(dto.getName());
    }

    @Test
    void eliminarAgencySinDependenciasDevuelve200() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/agencies/" + idAgencySemilla, HttpMethod.DELETE, withAuth(tokenA), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listarAgenciesDeMiEmpresaDevuelveSoloLasDeMiEmpresa() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/agencies", HttpMethod.GET, withAuth(tokenA), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idAgency\":\"" + idAgencySemilla + "\"");
        assertThat(response.getBody()).doesNotContain("\"idAgency\":\"" + idAgencyB + "\"");
    }

    @Test
    void searchDevuelvePagedResultConAgenciesDeMiEmpresa() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/agencies/search?page=0&size=10", HttpMethod.POST, withAuth(body, tokenA), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
    }
}
