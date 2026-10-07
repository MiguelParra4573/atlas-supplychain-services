package com.atlas.supplychain.auth.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Reproduce la combinación de propiedades del perfil `prod` (springdoc deshabilitado) sin
 * activar el perfil `prod` real -- mismo motivo que OpenApiQcAccessIntegrationTest: `prod`
 * apunta a Postgres vía variables de entorno inexistentes en test.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false",
        "atlas.openapi.public-access=false"
})
class OpenApiProdAccessIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void apiDocsDevuelve404PorqueSpringdocNoRegistraHandlers() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/v3/api-docs", HttpMethod.GET, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
