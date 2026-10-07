package com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.db;

import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 1.17, criterio de aceptación: "crear 3 Company consecutivas produce idCompany
 * '01','02','03' en orden". Corre contra su propia base Postgres (Testcontainers, nombre de BD
 * distinto al de {@code application.yml} de test) en vez del contexto de Spring compartido con
 * el resto de la suite -- ese contexto reutiliza la misma BD entre clases, así que
 * {@code company_id_seq} ya podría estar adelantada por otras pruebas de {@code organization}
 * al llegar acá. Con su propia BD, la secuencia arranca garantizado en 1 (creada por
 * {@code src/test/resources/schema.sql}).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:17:///companyidseq?TC_INITSCRIPT=file:src/test/resources/schema.sql"
})
class CompanyIdSequenceIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void crearTresCompaniesConsecutivasProduceIdsEnOrden() {
        Company primera = nuevaCompany("1");
        Company segunda = nuevaCompany("2");
        Company tercera = nuevaCompany("3");

        companyRepository.crearCompany(primera);
        companyRepository.crearCompany(segunda);
        companyRepository.crearCompany(tercera);

        assertThat(primera.getIdCompany()).isEqualTo("01");
        assertThat(segunda.getIdCompany()).isEqualTo("02");
        assertThat(tercera.getIdCompany()).isEqualTo("03");
    }

    private Company nuevaCompany(String sufijo) {
        String unico = UUID.randomUUID().toString().substring(0, 8) + sufijo;
        return Company.builder()
                .alias("seq-" + unico).businessName("Secuencial " + unico).ruc("SEQ" + unico)
                .basePath("/" + unico).cedulaRepresLegal("1234567890")
                .legalRepFirstName("Juan").legalRepLastName("Perez").state(true).build();
    }
}
