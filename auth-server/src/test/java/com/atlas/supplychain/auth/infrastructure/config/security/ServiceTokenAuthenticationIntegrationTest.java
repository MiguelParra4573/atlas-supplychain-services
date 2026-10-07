package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 4.2 §5 -- lado auth-server de la autenticación servicio-a-servicio de ms-reporte: un JWT
 * firmado con la misma clave privada RS256 que usa auth-server, {@code sub="svc-ms-reporte"},
 * claim {@code typ="service"}, debe autenticar en los endpoints de lectura que ms-reporte
 * consume (POST /usuarios/search, GET /roles) SIN que JwtAuthenticationFilter consulte
 * "auth"."users" -- el subject "svc-ms-reporte" no existe en esa tabla; si el filtro
 * intentara loadUserByUsername(...), la request caería a 401 y estos tests fallarían -- y debe
 * recibir 403 en un endpoint de escritura protegido con @PreAuthorize sobre autoridad efectiva
 * (POST /usuarios): un token de servicio nunca hereda permisos de usuario.
 *
 * <p>El token se firma acá mismo con "security.jwt.private-key-path" (mismo par de claves que
 * JwtService, ver JwtProperties) en vez de reusar JwtService -- su método de firma vive en
 * application/, sin claim "typ", y este test necesita ese claim exacto para simular el token que
 * emitirá ms-reporte (Spec 4.2, ServiceTokenProvider, en el otro repo).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ServiceTokenAuthenticationIntegrationTest {

    private static final String SERVICE_SUBJECT = "svc-ms-reporte";

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
    private String idUserA;

    private String idCompanyB;
    private String idUserB;

    @BeforeEach
    void cargarClavePrivadaYSembrarDosEmpresas() throws Exception {
        privateKey = loadPrivateKey(jwtProperties.getPrivateKeyPath());

        String hash = passwordEncoder.encode("Test#1234");

        idCompanyA = UUID.randomUUID().toString();
        idUserA = seedUsuario(idCompanyA, hash);

        idCompanyB = UUID.randomUUID().toString();
        idUserB = seedUsuario(idCompanyB, hash);
    }

    private String seedUsuario(String idCompany, String hash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, "Rol-" + idCompany);
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
            ps.setString(3, username + "@test.com");
            ps.setString(4, hash);
            ps.setLong(5, idRole);
            return ps;
        }, userKeyHolder);
        return String.valueOf(userKeyHolder.getKey().longValue());
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

    /** Réplica del token de servicio que emitirá ms-reporte: sub, typ="service", idCompany, TTL 60s. */
    private String tokenDeServicio(String idCompany) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .setSubject(SERVICE_SUBJECT)
                .claim("typ", "service")
                .claim("idCompany", idCompany)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(60, ChronoUnit.SECONDS)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private HttpEntity<Object> withServiceToken(String idCompany, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenDeServicio(idCompany));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void tokenDeServicioAutenticaEnSearchSinConsultarLaTablaDeUsuariosYFiltraPorIdCompanyDelToken() {
        Map<String, Object> body = Map.of("filters", List.of(), "sorts", List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios/search?page=0&size=50", HttpMethod.POST, withServiceToken(idCompanyA, body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"idUser\":\"" + idUserA + "\"");
        // Comparar el campo exacto, no un substring suelto: idUserB es un entero secuencial
        // corto (ej. "2"), y ese dígito puede coincidir por azar con otro valor del JSON (el
        // username random de idUserA, "page", "size", "totalElements") sin que haya ninguna
        // filtración real -- doesNotContain(idUserB) a secas es un assert roto, no una regresión.
        assertThat(response.getBody()).doesNotContain("\"idUser\":\"" + idUserB + "\"");
    }

    @Test
    void tokenDeServicioAutenticaEnGetRolesFiltradoPorIdCompanyDelToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/roles", HttpMethod.GET, withServiceToken(idCompanyA, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void tokenDeServicioRecibe403EnUnEndpointDeEscrituraProtegidoPorPermiso() {
        UserDTO body = UserDTO.builder()
                .idUser(UUID.randomUUID().toString())
                .name("Intento")
                .lastName("Servicio")
                .email("intento-servicio-" + UUID.randomUUID() + "@test.com")
                .password("Clave123")
                .state(true)
                .idRole(999_999_999L)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios", HttpMethod.POST, withServiceToken(idCompanyA, body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
