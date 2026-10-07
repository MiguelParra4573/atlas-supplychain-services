package com.atlas.supplychain.auth.infrastructure.config.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topología RabbitMQ propia de auth-server (ADR-002): un topic exchange por contexto; la routing
 * key de cada mensaje es el tipo de evento.
 */
@ConfigurationProperties(prefix = "atlas.messaging")
@Getter
@Setter
public class MessagingProperties {

    /** Exchange donde auth-server publica sus eventos de dominio y de auditoría. */
    private String eventsExchange = "auth.events";

    /** Nombre del productor que viaja en el envelope ({@code producer}). */
    private String producer = "auth-server";
}
