package com.atlas.supplychain.auth;

import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.GrantedPermissionController;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.ModuleController;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.OperationController;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.RoleController;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 1.7 §criterio de aceptación línea 273 -- versión literal: aplica de verdad
 * 004-normalizar-catalogo-rbac.sql contra Postgres real (vía Liquibase, perfil liquibase-it) y
 * compara el catálogo resultante contra las autoridades de los @PreAuthorize, en vez de la
 * aproximación estática de texto que hace RbacCatalogoCoherenciaTest (que sí corre en H2 sin
 * este bloqueo).
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("liquibase-it")
class RbacCatalogoCoherenciaPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withInitScript("init-auth-schema.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<Class<?>> CONTROLLERS = List.of(
            UserController.class, RoleController.class, ModuleController.class,
            OperationController.class, GrantedPermissionController.class);

    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    @Test
    void elCatalogoAplicadoPorLiquibaseContieneExactamenteLasAutoridadesDeLosPreAuthorize() {
        Set<String> autoridadesEnCodigo = new HashSet<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                PreAuthorize anotacion = method.getAnnotation(PreAuthorize.class);
                if (anotacion == null) {
                    continue;
                }
                Matcher matcher = HAS_AUTHORITY.matcher(anotacion.value());
                if (matcher.matches()) {
                    autoridadesEnCodigo.add(matcher.group(1));
                }
            }
        }

        // Spec 1.2: la autoridad sale de (resource, tag) de la propia operación -- ya no hay que
        // pasar por `modules` para reconstruirla. Que esta consulta siga devolviendo exactamente
        // las mismas cadenas que antes de la migración es justamente lo que prueba que el backfill
        // del changelog 025 no cambió ninguna autoridad vigente.
        List<String> filas = jdbcTemplate.queryForList(
                "SELECT o.resource || ':' || o.tag FROM auth.operations o " +
                        "WHERE o.resource IN ('USUARIOS','ROLES','MODULOS','OPERACIONES','PERMISOS')",
                String.class);

        assertThat(new HashSet<>(filas)).isEqualTo(autoridadesEnCodigo);
    }
}
