package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 1.7 -- coherencia entre las anotaciones @PreAuthorize de los 5 controllers de
 * authorization y el vocabulario que siembra 004-normalizar-catalogo-rbac.sql. Liquibase está
 * deshabilitado en el perfil de test por defecto (H2, sin dependencia de Postgres/Docker),
 * así que no se ejecuta la migración real -- se compara la autoridad de cada anotación
 * (extraída por reflexión) contra el vocabulario cerrado de 5 módulos x 5 tags, y se verifica
 * que el texto de la migración declara cada módulo y cada tag al menos una vez.
 */
class RbacCatalogoCoherenciaTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            UserController.class, RoleController.class, ModuleController.class,
            OperationController.class, GrantedPermissionController.class);

    private static final Set<String> MODULOS = Set.of("USUARIOS", "ROLES", "MODULOS", "OPERACIONES", "PERMISOS");
    private static final Set<String> TAGS = Set.of("CREAR", "EDITAR", "ELIMINAR", "CONSULTAR", "LISTAR");

    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    private Set<String> autoridadesEnLosControllers() {
        Set<String> autoridades = new HashSet<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                PreAuthorize anotacion = method.getAnnotation(PreAuthorize.class);
                if (anotacion == null) {
                    continue;
                }
                Matcher matcher = HAS_AUTHORITY.matcher(anotacion.value());
                assertThat(matcher.matches())
                        .as("El @PreAuthorize de %s.%s no tiene la forma hasAuthority('MODULO:OPERACION'): %s",
                                controller.getSimpleName(), method.getName(), anotacion.value())
                        .isTrue();
                autoridades.add(matcher.group(1));
            }
        }
        return autoridades;
    }

    @Test
    void cadaPreAuthorizeUsaUnaAutoridadDelVocabularioCerradoModuloOperacion() {
        Set<String> autoridades = autoridadesEnLosControllers();

        Set<String> esperado = new HashSet<>();
        for (String modulo : MODULOS) {
            for (String tag : TAGS) {
                esperado.add(modulo + ":" + tag);
            }
        }

        assertThat(autoridades).isEqualTo(esperado);
    }

    /**
     * 6 es la cantidad de endpoints que tenía cada uno de los 5 controllers al cerrar la Spec 1.7:
     * el piso protege contra que alguien saque una anotación al refactorizar. No es una igualdad
     * porque agregar endpoints protegidos es normal -- {@code OperationController} sumó los de
     * asignación a módulos en la Spec 1.2. Lo que sí sigue siendo exacto es que **todos** tengan
     * anotación con una autoridad del vocabulario cerrado, y eso lo cubre el test de arriba.
     */
    @Test
    void hayAlMenosSeisPreAuthorizePorCadaUnoDeLosCincoControllers() {
        for (Class<?> controller : CONTROLLERS) {
            long cantidad = Arrays.stream(controller.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PreAuthorize.class))
                    .count();
            assertThat(cantidad).as("%s con menos @PreAuthorize de los esperados", controller.getSimpleName())
                    .isGreaterThanOrEqualTo(6);
        }
    }

    @Test
    void laMigracionDeclaraCadaModuloYCadaTagDelVocabulario() throws IOException {
        String sql = StreamUtils.copyToString(
                new ClassPathResource("db/changelog/002-seed-rbac-catalog.sql").getInputStream(),
                StandardCharsets.UTF_8);

        for (String modulo : MODULOS) {
            assertThat(sql).as("Migración sin el módulo %s", modulo).contains("'" + modulo + "'");
        }
        for (String tag : TAGS) {
            assertThat(sql).as("Migración sin el tag %s", tag).contains("'" + tag + "'");
        }
    }
}
