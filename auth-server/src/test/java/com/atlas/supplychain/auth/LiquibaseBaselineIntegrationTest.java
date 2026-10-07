package com.atlas.supplychain.auth;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("liquibase-it")
class LiquibaseBaselineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withInitScript("init-auth-schema.sql");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextArrancaSobreBaseVaciaAplicandoLosChangesetsConDdlAutoValidate() {
        // Liquibase registra cada changeset del changelog maestro (baseline, catálogo RBAC y,
        // con el contexto demo del perfil liquibase-it, los usuarios de demo) como una fila propia.
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, author, filename FROM auth.databasechangelog ORDER BY orderexecuted");

        assertThat(rows).extracting(row -> row.get("id"))
                .containsExactly("001-baseline-schema", "002-seed-rbac-catalog", "003-seed-demo-users");
        assertThat(rows).allSatisfy(row -> assertThat(row.get("author")).isEqualTo("atlas"));
    }

    @Test
    void reejecutarUpdateEsIdempotenteYLosChecksumsSiguenValidando() throws Exception {
        try (var connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName("auth");

            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database)) {
                // Si algún checksum no coincidiera con lo ya aplicado, update() lanza
                // ValidationFailedException -- no reaplica nada porque los 3 changesets
                // ya están registrados.
                liquibase.update("demo");
            }
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auth.databasechangelog", Integer.class);
        assertThat(count).isEqualTo(3);
    }
}
