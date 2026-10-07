package com.atlas.supplychain.auth.infrastructure.config.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Secretos compartidos por servicio para {@code POST /api/v1/service-tokens} (Spec 6.1 §Diseño-3):
 * mapa {@code serviceName -> secreto}, uno por servicio que necesite pedir un JWT de servicio sin
 * tener aún manera de autenticarse con JWT (huevo-gallina que el propio endpoint resuelve). Extensible
 * a futuros servicios agregando una entrada nueva en {@code application*.yml}, sin tocar el contrato
 * ni el código de {@link com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.ServiceTokenController}.
 */
@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class ServiceSecretsProperties {
    private Map<String, String> serviceSecrets = new HashMap<>();
}
