package com.atlas.supplychain.auth.infrastructure.config.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "outbox")
@Getter
@Setter
public class OutboxProperties {

    /**
     * Intervalo del poller de relay (Spec 3.4) -- cada cuánto {@code OutboxScheduler} dispara
     * {@code OutboxRelayService}. 5s por defecto: suficientemente rápido para que la sincronización
     * de otros servicios no se sienta como "atrasada", sin generar carga innecesaria.
     */
    private long pollIntervalMs = 5000;
}
