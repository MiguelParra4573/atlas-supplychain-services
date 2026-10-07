package com.atlas.supplychain.auth.infrastructure.out.client;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto de {@link AuditTrail} inyectada en los servicios de {@code application/}.
 * No habla con el broker: publica un evento interno y retorna de inmediato. El envío real a RabbitMQ
 * lo hace {@link AuditRabbitPublisher}, que escucha este evento después del commit.
 */
@Component
@RequiredArgsConstructor
public class AuditTrailPublisher implements AuditTrail {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void registrar(AuditEventCommand cmd) {
        eventPublisher.publishEvent(new AuditEventoInternoEvent(cmd));
    }
}
