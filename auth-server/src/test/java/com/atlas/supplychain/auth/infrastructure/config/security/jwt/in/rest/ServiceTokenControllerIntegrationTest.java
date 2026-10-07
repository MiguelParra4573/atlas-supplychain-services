package com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest;

import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.dto.ServiceTokenRequest;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.dto.ServiceTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Spec 6.1 §Diseño-3: {@code POST /service-tokens} (context-path {@code /api/v1}) es el único
 * endpoint de {@code auth-server} que un servicio sin JWT propio (ej. {@code ms-agent-runtime})
 * puede llamar para conseguir su primer JWT de servicio -- autenticado con {@code X-Service-Secret}
 * en vez de JWT, en tiempo constante ({@link com.atlas.supplychain.auth.infrastructure.config.security.jwt.ServiceTokenController}).
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "security.service-secrets.ms-agent-runtime=test-secret-6-1")
class ServiceTokenControllerIntegrationTest {

    private static final String SERVICE_NAME = "ms-agent-runtime";
    private static final String SECRETO_CORRECTO = "test-secret-6-1";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JwtProperties jwtProperties;

    private RSAPublicKey publicKey;

    private RSAPublicKey publicKey() throws Exception {
        if (publicKey == null) {
            String key = Files.readString(Path.of(jwtProperties.getPublicKeyPath()))
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        }
        return publicKey;
    }

    private ResponseEntity<ServiceTokenResponse> pedirToken(String serviceName, String secreto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (secreto != null) {
            headers.set("X-Service-Secret", secreto);
        }
        return restTemplate.exchange("/service-tokens", HttpMethod.POST,
                new HttpEntity<>(new ServiceTokenRequest(serviceName), headers), ServiceTokenResponse.class);
    }

    @Test
    void secretoCorrectoDevuelve200ConJwtValidoTypService() throws Exception {
        ResponseEntity<ServiceTokenResponse> response = pedirToken(SERVICE_NAME, SECRETO_CORRECTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ServiceTokenResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.token()).isNotBlank();
        assertThat(body.expiresAt()).isNotNull();

        Claims claims = Jwts.parser().verifyWith(publicKey()).build()
                .parseSignedClaims(body.token()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("svc-ms-agent-runtime");
        assertThat(claims.get("typ", String.class)).isEqualTo("service");
    }

    @Test
    void secretoIncorrectoDevuelve401() {
        ResponseEntity<ServiceTokenResponse> response = pedirToken(SERVICE_NAME, "secreto-equivocado");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sinHeaderDevuelve401() {
        ResponseEntity<ServiceTokenResponse> response = pedirToken(SERVICE_NAME, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void servicioNoConfiguradoDevuelve401() {
        ResponseEntity<ServiceTokenResponse> response = pedirToken("ms-servicio-inexistente", SECRETO_CORRECTO);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
