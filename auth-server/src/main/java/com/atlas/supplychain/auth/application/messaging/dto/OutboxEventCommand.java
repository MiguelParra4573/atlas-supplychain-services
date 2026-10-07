package com.atlas.supplychain.auth.application.messaging.dto;

import java.util.Map;

/**
 * Comando para registrar un evento de dominio en la tabla outbox (Spec 3.4, patrón Outbox --
 * {@code services/kafka/patron_outbox.txt}). {@code payload} son pares clave/valor simples; la
 * serialización a JSON para la columna de persistencia es responsabilidad de infrastructure, no
 * de quien emite el evento.
 *
 * @param aggregateType tipo de agregado afectado, ej. {@code USER}
 * @param aggregateId   identificador del agregado afectado
 * @param eventType     tipo de evento, ej. {@code USUARIO_EMAIL_CAMBIADO}
 * @param payload       datos del evento
 */
public record OutboxEventCommand(
        String aggregateType,
        String aggregateId,
        String eventType,
        Map<String, String> payload
) {
}
