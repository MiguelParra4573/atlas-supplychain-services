package com.atlas.supplychain.auth.infrastructure.config.security.jwt;

import com.atlas.supplychain.auth.application.security.contracts.AccessTokenClaims;
import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JwtServiceTest {

    private static final AccessTokenClaims CLAIMS =
            new AccessTokenClaims("usuario1", "1", List.of("ADMIN"), List.of("USR_READ"));

    private JwtService jwtService(int accessTokenTtlMinutes) throws Exception {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("auth-server");
        properties.setAccessTokenTtlMinutes(accessTokenTtlMinutes);
        properties.setRefreshTokenTtlDays(7);
        properties.setPrivateKeyPath("src/test/resources/keys/private_key.pem");
        properties.setPublicKeyPath("src/test/resources/keys/public_key.pem");

        JwtService service = new JwtService(properties);
        service.initKeys();
        return service;
    }

    @ParameterizedTest
    @ValueSource(ints = {15, 5})
    void elAccessTokenExpiraExactamenteEnElTtlConfigurado(int ttlMinutes) throws Exception {
        JwtService service = jwtService(ttlMinutes);
        Instant antes = Instant.now();

        String token = service.generarAccessToken(CLAIMS);

        Claims claims = service.validarToken(token);
        Instant expiracionEsperada = antes.plusSeconds(ttlMinutes * 60L);

        assertThat(claims.getExpiration().toInstant())
                .isCloseTo(expiracionEsperada, within(2, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void accessTokenTtlSecondsReflejaElValorConfigurado() throws Exception {
        assertThat(jwtService(15).accessTokenTtlSeconds()).isEqualTo(15 * 60L);
        assertThat(jwtService(5).accessTokenTtlSeconds()).isEqualTo(5 * 60L);
    }
}
