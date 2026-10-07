package com.atlas.supplychain.auth.infrastructure.common.outbox;

import com.atlas.supplychain.auth.application.messaging.contracts.OutboxRepository;
import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final OutboxEventJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void append(OutboxEventCommand command) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        OutboxEventEntity entity = new OutboxEventEntity(
                UUID.randomUUID().toString(),
                command.aggregateType(),
                command.aggregateId(),
                command.eventType(),
                objectMapper.writeValueAsString(command.payload()),
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                Instant.now(),
                false,
                null);
        jpaRepository.save(entity);
    }
}
