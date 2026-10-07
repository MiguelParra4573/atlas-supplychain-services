package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.application.security.dto.ResetTokenData;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordResetToken;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenStoreImplTest {

    @Mock
    private PasswordResetTokenRepository repository;

    private PasswordResetTokenStoreImpl store;

    @Test
    void invalidateAllActiveDelegaEnElRepositorio() {
        store = new PasswordResetTokenStoreImpl(repository);
        Instant now = Instant.now();

        store.invalidateAllActive("u1", "c1", now);

        verify(repository).invalidateAllActive("u1", "c1", now);
    }

    @Test
    void saveConstruyeElTokenYLoPersiste() {
        store = new PasswordResetTokenStoreImpl(repository);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);

        store.save("u1", "c1", "hash123", expiresAt);

        verify(repository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getIdUser()).isEqualTo("u1");
        assertThat(saved.getIdCompany()).isEqualTo("c1");
        assertThat(saved.getTokenHash()).isEqualTo("hash123");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void findByHashMapeaElTokenAResetTokenData() {
        store = new PasswordResetTokenStoreImpl(repository);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .idUser("u1")
                .idCompany("c1")
                .tokenHash("hash123")
                .expiresAt(expiresAt)
                .usedAt(null)
                .build();
        when(repository.findByTokenHash("hash123")).thenReturn(Optional.of(token));
        when(repository.findByTokenHash("otro")).thenReturn(Optional.empty());

        Optional<ResetTokenData> result = store.findByHash("hash123");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(new ResetTokenData(1L, "u1", "c1", expiresAt, false));
        assertThat(store.findByHash("otro")).isEmpty();
    }

    @Test
    void markUsedDevuelveTrueSoloSiSeActualizoAlgunaFila() {
        store = new PasswordResetTokenStoreImpl(repository);
        Instant usedAt = Instant.now();
        when(repository.markUsed(eq(1L), eq(usedAt))).thenReturn(1);
        when(repository.markUsed(eq(2L), eq(usedAt))).thenReturn(0);

        assertThat(store.markUsed(1L, usedAt)).isTrue();
        assertThat(store.markUsed(2L, usedAt)).isFalse();
    }
}
