package com.atlas.supplychain.auth.infrastructure.common.outbox;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxRepositoryImplTest {

    @Mock
    private OutboxEventJpaRepository jpaRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void appendSerializaElPayloadAJsonYGuardaSinMarcarComoProcesado() {
        OutboxRepositoryImpl repository = new OutboxRepositoryImpl(jpaRepository, objectMapper);

        repository.append(new OutboxEventCommand(
                "USER", "u1", "USUARIO_EMAIL_CAMBIADO", Map.of("idUser", "u1", "email", "nuevo@atlas.local")));

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(jpaRepository).save(captor.capture());
        OutboxEventEntity entity = captor.getValue();

        assertThat(entity.getEventId()).isNotBlank();
        assertThat(entity.getAggregateType()).isEqualTo("USER");
        assertThat(entity.getAggregateId()).isEqualTo("u1");
        assertThat(entity.getEventType()).isEqualTo("USUARIO_EMAIL_CAMBIADO");
        assertThat(entity.getPayload()).contains("\"idUser\":\"u1\"").contains("\"email\":\"nuevo@atlas.local\"");
        assertThat(entity.isProcessed()).isFalse();
        assertThat(entity.getProcessedAt()).isNull();
        assertThat(entity.getCreatedAt()).isNotNull();
    }
}
