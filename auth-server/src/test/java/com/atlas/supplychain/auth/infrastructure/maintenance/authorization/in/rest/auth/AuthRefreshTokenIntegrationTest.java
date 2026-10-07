package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.dto.RefreshRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthRefreshTokenIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void refreshConTokenInexistenteDevuelve401ConCodeRefreshTokenInvalido() {
        var req = new RefreshRequest(UUID.randomUUID().toString());

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/auth/refresh", req, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo(BusinessRuleCode.REFRESH_TOKEN_INVALIDO.name());
    }
}
