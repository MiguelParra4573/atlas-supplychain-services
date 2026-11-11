package com.atlas.supplychain.platform.commons.messaging;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Envelope común de eventos de la plataforma (ADR-003). Mismo formato para todos los servicios,
 * Java y Python; {@code occurredAt} siempre en UTC. {@code version} es un entero por tipo de
 * evento, no un valor constante del transporte: cada productor lo sube cuando cambia de forma
 * incompatible el esquema de ese tipo.
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
