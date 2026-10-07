package com.atlas.supplychain.auth.infrastructure.config.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Declara el exchange de eventos de auth-server (idempotente: RabbitAdmin solo lo crea si no
 * existe). Las colas las declara cada consumidor, no el productor.
 *
 * <p>{@code @EnableScheduling} habilita {@code OutboxScheduler}, el poller del relay.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMessagingConfig {

    @Bean
    public TopicExchange authEventsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.getEventsExchange(), true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
