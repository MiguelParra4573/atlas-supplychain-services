package com.atlas.supplychain.auth.infrastructure.config.security.jwt;

import org.springframework.security.core.AuthenticationException;

/**
 * {@code X-Service-Secret} ausente, de un servicio no configurado, o que no coincide con el
 * secreto esperado (Spec 6.1 §Diseño-3) -- nunca revela cuál de los tres casos fue, para no
 * filtrar si un {@code serviceName} existe en el mapa de secretos.
 */
public class InvalidServiceSecretException extends AuthenticationException {
    public InvalidServiceSecretException(String message) {
        super(message);
    }
}
