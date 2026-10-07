package com.atlas.supplychain.auth.infrastructure.out.client;

import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.infrastructure.common.messaging.EventEnvelope;
import com.atlas.supplychain.auth.infrastructure.config.messaging.MessagingProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Consume el {@link AuditEventoInternoEvent} y publica {@code AuditoriaRegistrada} en el exchange
 * {@code auth.events}. Un futuro consumidor de auditoría de la plataforma se suscribe a esa routing key.
 *
 * <p>{@code @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)}, síncrono a
 * propósito: nunca se audita algo que hizo rollback, también se ejecuta sin transacción (login) y
 * mantiene disponible el {@link RequestContextHolder} para completar {@code ip}/{@code userAgent}.
 *
 * <p>Best-effort: un solo intento y ninguna excepción se propaga -- un fallo del broker nunca debe
 * romper la operación de negocio que ya se completó.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRabbitPublisher {

    static final String EVENT_TYPE = "AuditoriaRegistrada";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessagingProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRegistrarEvento(AuditEventoInternoEvent evento) {
        AuditEventCommand cmd = evento.comando();
        HttpServletRequest request = requestActual();
        String correlationId = resolverCorrelationId(request);

        ObjectNode payload = objectMapper.valueToTree(cmd);
        payload.put("sourceService", properties.getProducer());
        payload.put("ip", request != null ? request.getRemoteAddr() : null);
        payload.put("userAgent", request != null ? request.getHeader(USER_AGENT_HEADER) : null);
        payload.put("requestId", request != null ? request.getHeader(REQUEST_ID_HEADER) : null);

        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                1,
                Instant.now(),
                cmd.entidadId(),
                cmd.entidadTipo(),
                correlationId,
                null,
                properties.getProducer(),
                payload);

        try {
            rabbitTemplate.convertAndSend(properties.getEventsExchange(), EVENT_TYPE, envelope);
        } catch (Exception e) {
            log.warn("No se pudo publicar el evento de auditoría [{}] para {}: {}",
                    cmd.accionNombre(), cmd.entidadId(), e.toString());
        }
    }

    private HttpServletRequest requestActual() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private String resolverCorrelationId(HttpServletRequest request) {
        if (request != null) {
            String header = request.getHeader(CORRELATION_ID_HEADER);
            if (header != null && !header.isBlank()) {
                return header;
            }
        }
        return UUID.randomUUID().toString();
    }
}
