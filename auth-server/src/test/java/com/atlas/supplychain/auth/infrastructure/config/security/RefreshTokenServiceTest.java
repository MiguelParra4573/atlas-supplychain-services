package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.RefreshToken;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService service(int refreshTokenTtlDays) {
        JwtProperties properties = new JwtProperties();
        properties.setRefreshTokenTtlDays(refreshTokenTtlDays);
        return new RefreshTokenService(repo, properties);
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 1})
    void elRefreshTokenExpiraExactamenteEnElTtlConfigurado(int ttlDays) {
        RefreshTokenService service = service(ttlDays);
        Instant antes = Instant.now();

        service.issueForLogin("usuario1", "1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());

        Instant expiracionEsperada = antes.plus(Duration.ofDays(ttlDays));
        assertThat(captor.getValue().getExpiresAt())
                .isCloseTo(expiracionEsperada, within(2, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void refreshTtlSecondsReflejaElValorConfigurado() {
        assertThat(service(7).refreshTtlSeconds()).isEqualTo(Duration.ofDays(7).toSeconds());
        assertThat(service(1).refreshTtlSeconds()).isEqualTo(Duration.ofDays(1).toSeconds());
    }
}
