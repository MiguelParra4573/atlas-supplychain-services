package com.atlas.supplychain.auth.infrastructure.common.messaging.outbox;

import com.atlas.supplychain.auth.infrastructure.common.messaging.EventEnvelope;
import com.atlas.supplychain.auth.infrastructure.common.outbox.OutboxEventEntity;
import com.atlas.supplychain.auth.infrastructure.common.outbox.OutboxEventJpaRepository;
import com.atlas.supplychain.auth.infrastructure.config.messaging.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventJpaRepository jpaRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        relayService = new OutboxRelayService(jpaRepository, rabbitTemplate, objectMapper, new MessagingProperties());
    }

    @Test
    void publicaCadaEventoPendienteConElEnvelopeComunYLoMarcaComoProcesado() {
        Instant creado = Instant.parse("2026-09-23T10:00:00Z");
        OutboxEventEntity evento = new OutboxEventEntity(
                "e1", "USER", "u1", "USUARIO_EMAIL_CAMBIADO",
                "{\"idUser\":\"u1\",\"email\":\"nuevo@atlas.local\"}", "corr-1", creado, false, null);
        when(jpaRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(evento));

        relayService.relay();

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(eq("auth.events"), eq("USUARIO_EMAIL_CAMBIADO"), captor.capture());
        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.eventId()).isEqualTo("e1");
        assertThat(envelope.type()).isEqualTo("USUARIO_EMAIL_CAMBIADO");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.occurredAt()).isEqualTo(creado);
        assertThat(envelope.aggregateId()).isEqualTo("u1");
        assertThat(envelope.aggregateType()).isEqualTo("USER");
        assertThat(envelope.correlationId()).isEqualTo("corr-1");
        assertThat(envelope.producer()).isEqualTo("auth-server");
        assertThat(envelope.payload().get("idUser").asString()).isEqualTo("u1");

        ArgumentCaptor<OutboxEventEntity> savedCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(jpaRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isProcessed()).isTrue();
        assertThat(savedCaptor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void sinEventosPendientesNoPublicaNada() {
        when(jpaRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        relayService.relay();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(jpaRepository, never()).save(any());
    }
}
