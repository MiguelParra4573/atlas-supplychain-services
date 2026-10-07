package com.atlas.supplychain.auth.infrastructure.common.messaging.outbox;

import com.atlas.supplychain.auth.infrastructure.common.messaging.EventEnvelope;
import com.atlas.supplychain.auth.infrastructure.common.outbox.OutboxEventEntity;
import com.atlas.supplychain.auth.infrastructure.common.outbox.OutboxEventJpaRepository;
import com.atlas.supplychain.auth.infrastructure.config.messaging.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

/**
 * Publicador del patrón Outbox: lee filas no procesadas de {@code auth.outbox_event}, las publica
 * al exchange {@code auth.events} (routing key = tipo de evento) y marca {@code processed}.
 * Alcance mínimo: sin {@code SELECT ... FOR UPDATE SKIP LOCKED} -- aceptable con una sola réplica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private static final int ENVELOPE_VERSION = 1;

    private final OutboxEventJpaRepository jpaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessagingProperties properties;

    @Transactional
    public void relay() {
        List<OutboxEventEntity> pendientes = jpaRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity evento : pendientes) {
            publicar(evento);
            evento.setProcessed(true);
            evento.setProcessedAt(Instant.now());
            jpaRepository.save(evento);
        }
    }

    private void publicar(OutboxEventEntity evento) {
        EventEnvelope envelope = new EventEnvelope(
                evento.getEventId(),
                evento.getEventType(),
                ENVELOPE_VERSION,
                evento.getCreatedAt(),
                evento.getAggregateId(),
                evento.getAggregateType(),
                evento.getCorrelationId(),
                null,
                properties.getProducer(),
                objectMapper.readTree(evento.getPayload()));

        rabbitTemplate.convertAndSend(properties.getEventsExchange(), evento.getEventType(), envelope);

        log.info("Evento outbox publicado tipo={} aggregateId={}", evento.getEventType(), evento.getAggregateId());
    }
}
