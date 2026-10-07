package com.atlas.supplychain.auth.infrastructure.maintenance.organization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization.CompanyDTO;
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
 * Spec 1.17 -- {@code Company} es la raíz del tenant: a diferencia de
 * {@code AgencyControllerIntegrationTest}, ningún
 * endpoint acá depende del {@code idCompany} del token (cualquier autenticado puede leer
 * cualquier empresa, ver spec §2 "Sin idCompany como filtro de tenant en las lecturas").
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CompanyControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void sembrarUsuarioAutenticado() {
        String hash = passwordEncoder.encode("Test#1234");
        String idCompany = UUID.randomUUID().toString();
        long idRole = seedRole(idCompany, "Admin-" + idCompany);
        RbacSeeder.concederRecursos(jdbcTemplate, idCompany, idRole, "EMPRESAS");
        SeedUsuario seedUsuario = seedUsuario(idCompany, idRole, hash);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(seedUsuario.username(), "Test#1234"), TokenResponse.class);
        token = login.getAccessToken();
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

    /** Inserta una Company real usando la misma secuencia que {@code CompanyRepositoryImpl}. */
    private String seedCompany() {
        long siguiente = jdbcTemplate.queryForObject("SELECT nextval('auth.company_id_seq')", Long.class);
        String idCompany = String.format("%02d", siguiente);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"company\" (\"id_company\", \"alias\", \"business_name\", \"ruc\", \"base_path\", \"cedula_repres_legal\", \"legal_rep_first_name\", \"legal_rep_last_name\", \"state\") " +
                        "VALUES (?, ?, ?, ?, '/x', '1234567890', 'Nombres', 'Apellidos', true)",
                idCompany, "alias-" + idCompany, "Business " + idCompany, "RUC" + idCompany);
        return idCompany;
    }

    private void seedAgency(String idCompany) {
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"agencies\" (\"id_agency\", \"id_company\", \"name\", \"state\") VALUES (?, ?, 'Agencia Semilla', true)",
                UUID.randomUUID().toString(), idCompany);
    }

    private <T> HttpEntity<T> withAuth(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> withAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void obtenerCompanyPorIdSinTokenDevuelve401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/companies/01", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void crearCompanyDevuelve201() {
        String unico = UUID.randomUUID().toString().substring(0, 8);
        CompanyDTO dto = CompanyDTO.builder()
                .alias("alias-" + unico).businessName("Negocio " + unico).ruc("RUC" + unico)
                .basePath("/" + unico).cedulaRepresLegal("1234567890")
                .legalRepFirstName("Juan").legalRepLastName("Perez").state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/companies", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crearCompanyConAliasDuplicadoDevuelve409() {
        String idExistente = seedCompany();
        String aliasExistente = jdbcTemplate.queryForObject(
                "SELECT \"alias\" FROM \"auth\".\"company\" WHERE \"id_company\" = ?", String.class, idExistente);

        String unico = UUID.randomUUID().toString().substring(0, 8);
        CompanyDTO dto = CompanyDTO.builder()
                .alias(aliasExistente).businessName("Negocio " + unico).ruc("RUC" + unico)
                .basePath("/" + unico).cedulaRepresLegal("1234567890")
                .legalRepFirstName("Juan").legalRepLastName("Perez").state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/companies", HttpMethod.POST, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void obtenerCompanyPorIdDevuelve200() {
        String idCompany = seedCompany();

        ResponseEntity<CompanyDTO> response = restTemplate.exchange(
                "/companies/" + idCompany, HttpMethod.GET, withAuth(), CompanyDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdCompany()).isEqualTo(idCompany);
    }

    @Test
    void obtenerCompanyPorIdInexistenteDevuelve404() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/companies/99", HttpMethod.GET, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarCompanyDevuelve200YPersisteElCambio() {
        String idCompany = seedCompany();
        CompanyDTO dto = CompanyDTO.builder()
                .idCompany(idCompany).alias("renombrada-" + idCompany).businessName("Renombrada SA")
                .ruc("RUC-" + idCompany + "-2").basePath("/renombrada").cedulaRepresLegal("1234567890")
                .legalRepFirstName("Juan").legalRepLastName("Perez").state(true).build();

        ResponseEntity<Void> response = restTemplate.exchange("/companies", HttpMethod.PATCH, withAuth(dto), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String alias = jdbcTemplate.queryForObject(
                "SELECT \"alias\" FROM \"auth\".\"company\" WHERE \"id_company\" = ?", String.class, idCompany);
        assertThat(alias).isEqualTo(dto.getAlias());
    }

    @Test
    void eliminarCompanyConAgenciaAsociadaDevuelve409YNoLaBorra() {
        String idCompany = seedCompany();
        seedAgency(idCompany);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/companies/" + idCompany, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"company\" WHERE \"id_company\" = ?", Integer.class, idCompany);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void eliminarCompanySinDependenciasDevuelve200() {
        String idCompany = seedCompany();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/companies/" + idCompany, HttpMethod.DELETE, withAuth(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"company\" WHERE \"id_company\" = ?", Integer.class, idCompany);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void searchDevuelvePagedResultConCompanies() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/companies/search?page=0&size=10", HttpMethod.POST, withAuth(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
    }
}
