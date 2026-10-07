package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.GrantedPermissionDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ModuleDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.OperationDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.RoleDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
import com.atlas.supplychain.auth.testsupport.RbacSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.7 -- recorre las 30 filas de la tabla endpoint->autoridad de §4: con un usuario que
 * posee exactamente la autoridad indicada, la respuesta no es 403; con un usuario autenticado
 * sin ninguna autoridad, la respuesta sí es 403 en todas. El contenido de los bodies usa IDs
 * inexistentes a propósito -- solo importa que la autorización deje pasar o no la llamada, no
 * el resultado de negocio (200/404/409 son todos "no 403" igual de válidos aquí).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RbacEndpointAuthorityIntegrationTest {

    private static final long ID_INEXISTENTE = 999_999_999L;
    private static final String PASSWORD = "Test#1234";

    private record EndpointCase(HttpMethod method, String path, String authority, String modulo, String tag, Object body) {
        EndpointCase {
            String[] parts = authority.split(":", 2);
            if (!parts[0].equals(modulo) || !parts[1].equals(tag)) {
                throw new IllegalStateException("authority/modulo/tag inconsistentes en el caso de " + path);
            }
        }
    }

    private static Object usuarioCrearBody() {
        return UserDTO.builder()
                .idUser(UUID.randomUUID().toString())
                .name("Test").lastName("User")
                .email("rbac-crear-" + UUID.randomUUID() + "@test.com")
                .password("Clave123")
                .state(true)
                .idRole(ID_INEXISTENTE)
                .build();
    }

    private static Object usuarioActualizarBody() {
        return UserDTO.builder()
                .idUser(UUID.randomUUID().toString())
                .name("Test").lastName("User")
                .email("rbac-editar-" + UUID.randomUUID() + "@test.com")
                .state(true)
                .idRole(ID_INEXISTENTE)
                .build();
    }

    private static Object rolBody() {
        return RoleDTO.builder().idRole(ID_INEXISTENTE).name("Rol-" + UUID.randomUUID()).state(true).build();
    }

    private static Object moduloBody() {
        return ModuleDTO.builder().idModule(ID_INEXISTENTE).name("Mod-" + UUID.randomUUID()).state(true).build();
    }

    private static Object operacionCrearBody() {
        return OperationDTO.builder().resource("RES-" + UUID.randomUUID()).tag("TAG-" + UUID.randomUUID())
                .path("/x").httpMethod("GET").state(true).build();
    }

    private static Object operacionActualizarBody() {
        return OperationDTO.builder().idOperation(ID_INEXISTENTE).tag("TAG-" + UUID.randomUUID()).path("/x").httpMethod("GET").state(true).build();
    }

    private static Object permisoCrearBody() {
        return GrantedPermissionDTO.builder().idRole(ID_INEXISTENTE).idOperation(ID_INEXISTENTE).build();
    }

    private static Object permisoActualizarBody() {
        return GrantedPermissionDTO.builder().idGrantedPermission(ID_INEXISTENTE).idRole(ID_INEXISTENTE).idOperation(ID_INEXISTENTE).build();
    }

    private static Object searchBody() {
        return Map.of("filters", List.of(), "sorts", List.of());
    }

    private static final List<EndpointCase> CASOS = List.of(
            new EndpointCase(HttpMethod.POST, "/usuarios", "USUARIOS:CREAR", "USUARIOS", "CREAR", usuarioCrearBody()),
            new EndpointCase(HttpMethod.PATCH, "/usuarios", "USUARIOS:EDITAR", "USUARIOS", "EDITAR", usuarioActualizarBody()),
            new EndpointCase(HttpMethod.DELETE, "/usuarios/no-existe", "USUARIOS:ELIMINAR", "USUARIOS", "ELIMINAR", null),
            new EndpointCase(HttpMethod.GET, "/usuarios/no-existe", "USUARIOS:CONSULTAR", "USUARIOS", "CONSULTAR", null),
            new EndpointCase(HttpMethod.GET, "/usuarios", "USUARIOS:LISTAR", "USUARIOS", "LISTAR", null),
            new EndpointCase(HttpMethod.POST, "/usuarios/search", "USUARIOS:LISTAR", "USUARIOS", "LISTAR", searchBody()),

            new EndpointCase(HttpMethod.POST, "/roles", "ROLES:CREAR", "ROLES", "CREAR", rolBody()),
            new EndpointCase(HttpMethod.PATCH, "/roles", "ROLES:EDITAR", "ROLES", "EDITAR", rolBody()),
            new EndpointCase(HttpMethod.DELETE, "/roles/" + ID_INEXISTENTE, "ROLES:ELIMINAR", "ROLES", "ELIMINAR", null),
            new EndpointCase(HttpMethod.GET, "/roles/" + ID_INEXISTENTE, "ROLES:CONSULTAR", "ROLES", "CONSULTAR", null),
            new EndpointCase(HttpMethod.GET, "/roles", "ROLES:LISTAR", "ROLES", "LISTAR", null),
            new EndpointCase(HttpMethod.POST, "/roles/search", "ROLES:LISTAR", "ROLES", "LISTAR", searchBody()),

            new EndpointCase(HttpMethod.POST, "/modules", "MODULOS:CREAR", "MODULOS", "CREAR", moduloBody()),
            new EndpointCase(HttpMethod.PATCH, "/modules", "MODULOS:EDITAR", "MODULOS", "EDITAR", moduloBody()),
            new EndpointCase(HttpMethod.DELETE, "/modules/" + ID_INEXISTENTE, "MODULOS:ELIMINAR", "MODULOS", "ELIMINAR", null),
            new EndpointCase(HttpMethod.GET, "/modules/" + ID_INEXISTENTE, "MODULOS:CONSULTAR", "MODULOS", "CONSULTAR", null),
            new EndpointCase(HttpMethod.GET, "/modules/estado/true", "MODULOS:LISTAR", "MODULOS", "LISTAR", null),
            new EndpointCase(HttpMethod.POST, "/modules/search", "MODULOS:LISTAR", "MODULOS", "LISTAR", searchBody()),

            new EndpointCase(HttpMethod.POST, "/operations", "OPERACIONES:CREAR", "OPERACIONES", "CREAR", operacionCrearBody()),
            new EndpointCase(HttpMethod.PATCH, "/operations", "OPERACIONES:EDITAR", "OPERACIONES", "EDITAR", operacionActualizarBody()),
            new EndpointCase(HttpMethod.DELETE, "/operations/" + ID_INEXISTENTE, "OPERACIONES:ELIMINAR", "OPERACIONES", "ELIMINAR", null),
            new EndpointCase(HttpMethod.GET, "/operations/" + ID_INEXISTENTE, "OPERACIONES:CONSULTAR", "OPERACIONES", "CONSULTAR", null),
            new EndpointCase(HttpMethod.POST, "/operations/module/" + ID_INEXISTENTE, "OPERACIONES:LISTAR", "OPERACIONES", "LISTAR", null),
            new EndpointCase(HttpMethod.POST, "/operations/search", "OPERACIONES:LISTAR", "OPERACIONES", "LISTAR", searchBody()),

            new EndpointCase(HttpMethod.POST, "/permissions", "PERMISOS:CREAR", "PERMISOS", "CREAR", permisoCrearBody()),
            new EndpointCase(HttpMethod.PATCH, "/permissions", "PERMISOS:EDITAR", "PERMISOS", "EDITAR", permisoActualizarBody()),
            new EndpointCase(HttpMethod.DELETE, "/permissions/" + ID_INEXISTENTE, "PERMISOS:ELIMINAR", "PERMISOS", "ELIMINAR", null),
            new EndpointCase(HttpMethod.GET, "/permissions/" + ID_INEXISTENTE, "PERMISOS:CONSULTAR", "PERMISOS", "CONSULTAR", null),
            new EndpointCase(HttpMethod.GET, "/permissions/role/" + ID_INEXISTENTE, "PERMISOS:LISTAR", "PERMISOS", "LISTAR", null),
            new EndpointCase(HttpMethod.POST, "/permissions/search", "PERMISOS:LISTAR", "PERMISOS", "LISTAR", searchBody())
    );

    static Stream<EndpointCase> casos() {
        return CASOS.stream();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String hash;
    private String tokenSinAutoridades;

    @BeforeEach
    void sembrarUsuarioSinAutoridades() {
        hash = passwordEncoder.encode(PASSWORD);

        String idCompany = UUID.randomUUID().toString();
        long idRole = seedRole(idCompany);
        SeedUsuario seedUsuario = seedUsuario(idCompany, idRole);

        tokenSinAutoridades = login(seedUsuario.username());
    }

    private long seedRole(String idCompany) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"roles\" (\"id_company\", \"name\", \"state\") VALUES (?, ?, true)", new String[]{"id_rol"});
            ps.setString(1, idCompany);
            ps.setString(2, "Rol-" + UUID.randomUUID());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private record SeedUsuario(String idUser, String username) {}

    private SeedUsuario seedUsuario(String idCompany, long idRole) {
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

    private String login(String username) {
        TokenResponse login = restTemplate.postForObject(
                "/auth/login", new LoginRequest(username, PASSWORD), TokenResponse.class);
        return login.getAccessToken();
    }

    private HttpEntity<Object> withAuth(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @ParameterizedTest(name = "{0} {1} con {2}")
    @MethodSource("casos")
    void conExactamenteLaAutoridadIndicadaNoDevuelve403(EndpointCase caso) {
        String idCompany = UUID.randomUUID().toString();
        long idRole = seedRole(idCompany);
        RbacSeeder.concederUnaAutoridad(jdbcTemplate, idCompany, idRole, caso.modulo(), caso.tag());
        SeedUsuario seedUsuario = seedUsuario(idCompany, idRole);
        String token = login(seedUsuario.username());

        ResponseEntity<String> response = restTemplate.exchange(
                caso.path(), caso.method(), withAuth(token, caso.body()), String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest(name = "{0} {1} sin autoridades devuelve 403")
    @MethodSource("casos")
    void sinNingunaAutoridadDevuelve403(EndpointCase caso) {
        ResponseEntity<String> response = restTemplate.exchange(
                caso.path(), caso.method(), withAuth(tokenSinAutoridades, caso.body()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void el403DeUnEndpointRealUsaElErrorResponseDeLaSpec16() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/usuarios", HttpMethod.GET, withAuth(tokenSinAutoridades, null), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Acceso denegado. No tienes permisos para realizar esta acción.");
        assertThat(body.correlationId()).isNotBlank();

        ResponseEntity<String> raw = restTemplate.exchange(
                "/usuarios", HttpMethod.GET, withAuth(tokenSinAutoridades, null), String.class);
        assertThat(raw.getBody()).doesNotContain("backendMessage", "\"url\"", "\"method\"", "\"status\"", "\"timestamp\"");
    }

    /**
     * Spec 1.1 (tenant) + Spec 1.7 (RBAC) combinadas: una autoridad válida no sobrescribe el
     * filtro de tenant -- un usuario con USUARIOS:CONSULTAR pero de otra empresa no accede al
     * recurso de la empresa ajena.
     */
    @Test
    void autoridadValidaDeOtraEmpresaNoOtorgaAccesoAlRecursoDeLaEmpresaAjena() {
        String idCompanyDueno = UUID.randomUUID().toString();
        long idRoleDueno = seedRole(idCompanyDueno);
        SeedUsuario seedDueno = seedUsuario(idCompanyDueno, idRoleDueno);
        String idUserDueno = seedDueno.idUser();

        String idCompanyAtacante = UUID.randomUUID().toString();
        long idRoleAtacante = seedRole(idCompanyAtacante);
        RbacSeeder.concederUnaAutoridad(jdbcTemplate, idCompanyAtacante, idRoleAtacante, "USUARIOS", "CONSULTAR");
        SeedUsuario seedAtacante = seedUsuario(idCompanyAtacante, idRoleAtacante);
        String tokenAtacante = login(seedAtacante.username());

        ResponseEntity<String> response = restTemplate.exchange(
                "/usuarios/" + idUserDueno, HttpMethod.GET, withAuth(tokenAtacante, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void treintaCasosCubrenLosCincoControllers() {
        assertThat(CASOS).hasSize(30);
        assertThat(CASOS.stream().map(EndpointCase::modulo).distinct()).containsExactlyInAnyOrder(
                "USUARIOS", "ROLES", "MODULOS", "OPERACIONES", "PERMISOS");
    }
}
