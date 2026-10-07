package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.OperationSelfRegistrationDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ResourceSelfRegistrationDTO;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Auto-registro RBAC: {@code POST /modules/self-register} solo lo puede llamar un JWT de servicio
 * ({@code typ="service"}, mismo mecanismo que
 * {@link com.atlas.supplychain.auth.infrastructure.config.security.ServiceTokenAuthenticationIntegrationTest})
 * -- nunca un usuario humano, ni siquiera admin.
 *
 * <p>Spec 1.2: lo que se replica en todas las empresas activas son las <b>operaciones</b>, y
 * <b>ningún módulo se crea</b> -- ese es el fondo de la corrección, así que tiene test propio
 * ({@link #elAutoRegistroNoCreaNingunModulo}) además del de replicación. También se verifica que el
 * payload del contrato viejo (con {@code name} en vez de {@code resource}) siga registrando contra
 * la misma autoridad, para no romper un {@code ms-*} que todavía no se actualizó.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ResourceSelfRegistrationControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProperties jwtProperties;

    private RSAPrivateKey privateKey;

    private String idCompanyA;
    private String idCompanyB;

    @BeforeEach
    void cargarClavePrivadaYSembrarDosEmpresas() throws Exception {
        privateKey = loadPrivateKey(jwtProperties.getPrivateKeyPath());

        idCompanyA = seedCompany();
        idCompanyB = seedCompany();
    }

    private String seedCompany() {
        long siguiente = jdbcTemplate.queryForObject("SELECT nextval('auth.company_id_seq')", Long.class);
        String idCompany = String.format("%02d", siguiente);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"company\" (\"id_company\", \"alias\", \"business_name\", \"ruc\", \"base_path\", "
                        + "\"cedula_repres_legal\", \"legal_rep_first_name\", \"legal_rep_last_name\", \"state\") "
                        + "VALUES (?, ?, ?, ?, '/x', '1234567890', 'Nombres', 'Apellidos', true)",
                idCompany, "alias-" + idCompany, "Business " + idCompany, "RUC" + idCompany);
        return idCompany;
    }

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String key = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /** Réplica del token que el {@code ApplicationRunner} de un micro nuevo emitirá al arrancar. */
    private String tokenDeServicio() {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .setSubject("svc-ms-inventario")
                .claim("typ", "service")
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(60, ChronoUnit.SECONDS)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private HttpEntity<Object> withServiceToken(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenDeServicio());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static List<OperationSelfRegistrationDTO> dosOperaciones() {
        return List.of(
                OperationSelfRegistrationDTO.builder()
                        .tag("CREAR").path("/api/v1/inventario").httpMethod("POST").permitAll(false).build(),
                OperationSelfRegistrationDTO.builder()
                        .tag("LISTAR").path("/api/v1/inventario").httpMethod("GET").permitAll(false).build());
    }

    private ResourceSelfRegistrationDTO unRecursoConDosOperaciones(String resource) {
        return ResourceSelfRegistrationDTO.builder()
                .resource(resource)
                .operations(dosOperaciones())
                .build();
    }

    private Integer contarOperaciones(String idCompany, String resource) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"operations\" WHERE \"id_company\" = ? AND \"resource\" = ?",
                Integer.class, idCompany, resource);
    }

    private Integer contarModulos(String idCompany, String name) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"modules\" WHERE \"id_company\" = ? AND \"name\" = ?",
                Integer.class, idCompany, name);
    }

    private Integer contarAsignacionesDe(String idCompany, String resource) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"auth\".\"module_operation\" mo "
                        + "JOIN \"auth\".\"operations\" o ON o.\"id_operation\" = mo.\"id_operation\" "
                        + "WHERE o.\"id_company\" = ? AND o.\"resource\" = ?",
                Integer.class, idCompany, resource);
    }

    @Test
    void tokenDeServicioAutoRegistraLasOperacionesEnTodasLasEmpresasActivas() {
        String resource = "INVENTARIO-" + UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(unRecursoConDosOperaciones(resource)), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(contarOperaciones(idCompanyA, resource)).isEqualTo(2);
        assertThat(contarOperaciones(idCompanyB, resource)).isEqualTo(2);
    }

    @Test
    void elAutoRegistroNoCreaNingunModulo() {
        String resource = "INVENTARIO-" + UUID.randomUUID();

        restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(unRecursoConDosOperaciones(resource)), Void.class);

        assertThat(contarModulos(idCompanyA, resource)).isZero();
        assertThat(contarModulos(idCompanyB, resource)).isZero();
        // Y las operaciones quedan sin asignar: no aparecen en el menú de nadie hasta que un
        // administrador de la empresa las cuelgue de algún módulo.
        assertThat(contarAsignacionesDe(idCompanyA, resource)).isZero();
        assertThat(contarAsignacionesDe(idCompanyB, resource)).isZero();
    }

    @Test
    void unModuloRenombradoAManoSobreviveAlAutoRegistro() {
        String resource = "INVENTARIO-" + UUID.randomUUID();
        restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(unRecursoConDosOperaciones(resource)), Void.class);

        // La empresa agrupa esas operaciones bajo una etiqueta propia y le pone su ícono.
        long idModule = seedModulo(idCompanyA, "Mi Inventario", "estrella");
        asignarTodasLasOperaciones(idCompanyA, resource, idModule);

        restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(unRecursoConDosOperaciones(resource)), Void.class);

        assertThat(nombreDelModulo(idModule)).isEqualTo("Mi Inventario");
        assertThat(iconoDelModulo(idModule)).isEqualTo("estrella");
        assertThat(contarModulos(idCompanyA, resource)).isZero();
        assertThat(contarAsignacionesDe(idCompanyA, resource)).isEqualTo(2);
    }

    @Test
    void elPayloadDelContratoViejoSigueRegistrandoContraLaMismaAutoridad() {
        String resource = "INVENTARIO-" + UUID.randomUUID();
        ResourceSelfRegistrationDTO viejo = ResourceSelfRegistrationDTO.builder()
                .name(resource)
                .operations(dosOperaciones())
                .build();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(viejo), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(contarOperaciones(idCompanyA, resource)).isEqualTo(2);
    }

    @Test
    void sinResourceNiNameDevuelve400() {
        ResourceSelfRegistrationDTO sinRecurso = ResourceSelfRegistrationDTO.builder()
                .operations(dosOperaciones())
                .build();

        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(sinRecurso), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void llamarDosVecesEsIdempotenteNoDuplicaOperaciones() {
        String resource = "INVENTARIO-" + UUID.randomUUID();
        ResourceSelfRegistrationDTO dto = unRecursoConDosOperaciones(resource);

        ResponseEntity<Void> primera = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(dto), Void.class);
        ResponseEntity<Void> segunda = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, withServiceToken(dto), Void.class);

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(contarOperaciones(idCompanyA, resource)).isEqualTo(2);
        assertThat(contarOperaciones(idCompanyB, resource)).isEqualTo(2);
    }

    @Test
    void unUsuarioHumanoAdminRecibe403AunqueTengaTodoElCatalogoConcedido() {
        String hash = passwordEncoder.encode("Test#1234");
        long idRoleAdmin = seedRole(idCompanyA, "Admin-" + idCompanyA);
        RbacSeeder.concederTodoElCatalogo(jdbcTemplate, idCompanyA, idRoleAdmin);
        String username = seedUsuario(idCompanyA, idRoleAdmin, hash);

        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, "Test#1234"), TokenResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(
                unRecursoConDosOperaciones("INVENTARIO-" + UUID.randomUUID()), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sinTokenDevuelve401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(
                unRecursoConDosOperaciones("INVENTARIO-" + UUID.randomUUID()), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/modules/self-register", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private long seedModulo(String idCompany, String name, String icon) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"modules\" (\"id_company\", \"name\", \"state\", \"icon\", \"display_order\") "
                            + "VALUES (?, ?, true, ?, 0)", new String[]{"id_module"});
            ps.setString(1, idCompany);
            ps.setString(2, name);
            ps.setString(3, icon);
            return ps;
        }, keyHolder);
        return ((Number) keyHolder.getKeys().get("id_module")).longValue();
    }

    private void asignarTodasLasOperaciones(String idCompany, String resource, long idModule) {
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"module_operation\" (\"id_module\", \"id_operation\", \"id_company\") "
                        + "SELECT ?, o.\"id_operation\", o.\"id_company\" FROM \"auth\".\"operations\" o "
                        + "WHERE o.\"id_company\" = ? AND o.\"resource\" = ?",
                idModule, idCompany, resource);
    }

    private String nombreDelModulo(long idModule) {
        return jdbcTemplate.queryForObject(
                "SELECT \"name\" FROM \"auth\".\"modules\" WHERE \"id_module\" = ?", String.class, idModule);
    }

    private String iconoDelModulo(long idModule) {
        return jdbcTemplate.queryForObject(
                "SELECT \"icon\" FROM \"auth\".\"modules\" WHERE \"id_module\" = ?", String.class, idModule);
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

    private String seedUsuario(String idCompany, long idRole, String hash) {
        String username = "user." + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"users\" (\"id_company\", \"username\", \"name\", \"last_name\", \"email\", "
                        + "\"password_hash\", \"failed_attempts\", \"state\", \"id_role\") "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 0, true, ?)",
                idCompany, username, username + "@test.com", hash, idRole);
        return username;
    }
}
