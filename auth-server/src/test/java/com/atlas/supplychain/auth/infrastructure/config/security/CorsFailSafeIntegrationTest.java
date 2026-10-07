package com.atlas.supplychain.auth.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.11 punto 1: un perfil que no declara atlas.cors.allowed-origins (como src/test/resources
 * no lo declara) no debe heredar ningún origen permitido -- fallo seguro, no permisivo.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CorsFailSafeIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void sinPropiedadDeclaradaNingunOrigenCruzadoEsAceptado() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:4200");
        headers.set("Access-Control-Request-Method", "GET");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/usuarios", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }
}
