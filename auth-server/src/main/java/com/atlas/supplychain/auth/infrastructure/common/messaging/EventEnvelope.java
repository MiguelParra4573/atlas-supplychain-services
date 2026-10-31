package com.atlas.supplychain.auth.infrastructure.common.messaging;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Envelope común de eventos de la plataforma (ADR-003). Mismo formato para todos los servicios,
 * Java y Python; {@code occurredAt} siempre en UTC.
 */
public record EventEnvelope(
        String eventId,
        String type,
        int version,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String correlationId,
        String causationId,
        String producer,
        JsonNode payload) {}
