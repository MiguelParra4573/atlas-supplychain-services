package com.atlas.supplychain.auth.infrastructure.out.client;

import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;

/**
 * Evento interno (dentro del mismo proceso, vía {@code ApplicationEventPublisher}) que desacopla el
 * disparo de auditoría (capa {@code application/}) del transporte hacia RabbitMQ.
 * {@link AuditTrailPublisher} lo publica; {@link AuditRabbitPublisher} lo consume después del commit.
 */
record AuditEventoInternoEvent(AuditEventCommand comando) {
}
