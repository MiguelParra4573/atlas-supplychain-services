package com.atlas.supplychain.auth.testsupport;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;

/**
 * Siembra el catálogo RBAC completo (5 módulos x 5 operaciones) y concede todo a un rol, para
 * que los tests de integración de los 5 controllers de authorization (protegidos por
 * @PreAuthorize desde la Spec 1.7) tengan acceso sin enumerar autoridad por autoridad.
 */
public final class RbacSeeder {

    private static final List<String> MODULOS = List.of("USUARIOS", "ROLES", "MODULOS", "OPERACIONES", "PERMISOS");
    private static final List<String> TAGS = List.of("CREAR", "EDITAR", "ELIMINAR", "CONSULTAR", "LISTAR");

    private RbacSeeder() {
    }

    /**
     * Concede exactamente una autoridad "RECURSO:OPERACION" al rol, sembrando su operación y el
     * módulo homónimo que la agrupa en el menú. Desde la Spec 1.2 son dos cosas distintas: la
     * autoridad sale de {@code (resource, tag)} de la operación y el módulo es solo la etiqueta
     * del menú -- acá coinciden por comodidad del fixture, igual que los deja la migración del
     * changelog 025.
     */
    public static void concederUnaAutoridad(JdbcTemplate jdbcTemplate, String idCompany, long idRole, String recurso, String tag) {
        long idModule = insertModule(jdbcTemplate, idCompany, recurso);
        long idOperation = insertOperation(jdbcTemplate, idCompany, idModule, recurso, tag);
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"granted_permissions\" (\"id_company\", \"id_operation\", \"id_role\") VALUES (?, ?, ?)",
                idCompany, idOperation, idRole);
    }

    /**
     * Concede los 5 tags de cada recurso indicado. Existe porque los tests de organización
     * (Agency/Company) creaban rol y usuario pero nunca sembraban una sola fila de
     * {@code granted_permissions}, así que sus 22 {@code @PreAuthorize} respondían 403 y 31 tests
     * quedaron rojos sin que nadie lo viera -- no hay CI que corra la suite entera.
     */
    public static void concederRecursos(JdbcTemplate jdbcTemplate, String idCompany, long idRole, String... recursos) {
        for (String recurso : recursos) {
            long idModule = insertModule(jdbcTemplate, idCompany, recurso);
            for (String tag : TAGS) {
                long idOperation = insertOperation(jdbcTemplate, idCompany, idModule, recurso, tag);
                jdbcTemplate.update(
                        "INSERT INTO \"auth\".\"granted_permissions\" (\"id_company\", \"id_operation\", \"id_role\") VALUES (?, ?, ?)",
                        idCompany, idOperation, idRole);
            }
        }
    }

    public static void concederTodoElCatalogo(JdbcTemplate jdbcTemplate, String idCompany, long idRole) {
        for (String recurso : MODULOS) {
            long idModule = insertModule(jdbcTemplate, idCompany, recurso);
            for (String tag : TAGS) {
                long idOperation = insertOperation(jdbcTemplate, idCompany, idModule, recurso, tag);
                jdbcTemplate.update(
                        "INSERT INTO \"auth\".\"granted_permissions\" (\"id_company\", \"id_operation\", \"id_role\") VALUES (?, ?, ?)",
                        idCompany, idOperation, idRole);
            }
        }
    }

    // `display_order` es NOT NULL desde el changelog 029 (`nullable = false` en ModuleEntity, que
    // es lo que genera el esquema H2 de los tests -- H2 no aplica el `DEFAULT 0` de Liquibase, eso
    // solo corre en Postgres real). Omitirla hace fallar todo test que pase por acá con "NULL not
    // allowed". `category` ya no existe (Spec 1.4, reemplazada por `id_menu_category`, nullable).
    // `base_path` tampoco existe (changelog 031, resto sin uso real de antes de la Spec 1.2).
    private static long insertModule(JdbcTemplate jdbcTemplate, String idCompany, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"modules\" (\"id_company\", \"name\", \"state\", \"display_order\") "
                            + "VALUES (?, ?, true, 0)", new String[]{"id_module"});
            ps.setString(1, idCompany);
            ps.setString(2, name);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private static long insertOperation(JdbcTemplate jdbcTemplate, String idCompany, long idModule, String resource, String tag) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO \"auth\".\"operations\" (\"id_company\", \"http_method\", \"path\", \"permit_all\", \"state\", \"resource\", \"tag\") " +
                            "VALUES (?, 'GET', '/x', false, true, ?, ?)", new String[]{"id_operation"});
            ps.setString(1, idCompany);
            ps.setString(2, resource);
            ps.setString(3, tag);
            return ps;
        }, keyHolder);
        long idOperation = keyHolder.getKey().longValue();
        jdbcTemplate.update(
                "INSERT INTO \"auth\".\"module_operation\" (\"id_module\", \"id_operation\", \"id_company\") VALUES (?, ?, ?)",
                idModule, idOperation, idCompany);
        return idOperation;
    }
}
