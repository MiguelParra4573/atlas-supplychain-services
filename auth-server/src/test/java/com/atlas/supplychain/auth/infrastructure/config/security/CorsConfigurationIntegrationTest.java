package com.atlas.supplychain.auth.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 1.11 punto 1: reproduce la combinación de propiedades del perfil `dev`
 * (atlas.cors.allowed-origins=http://localhost:4200) sin activar el perfil real, mismo motivo que
 * OpenApiQcAccessIntegrationTest -- los perfiles reales apuntan a Postgres.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "atlas.cors.allowed-origins=http://localhost:4200")
class CorsConfigurationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<Void> preflight(String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", origin);
        headers.set("Access-Control-Request-Method", "GET");
        return restTemplate.exchange("/usuarios", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);
    }

    @Test
    void preflightConOrigenPermitidoDevuelveAccessControlAllowOrigin() {
        ResponseEntity<Void> response = preflight("http://localhost:4200");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
    }

    @Test
    void preflightConOrigenNoPermitidoNoDevuelveAccessControlAllowOrigin() {
        ResponseEntity<Void> response = preflight("http://evil.test");

        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }
}
