package com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest;

import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.InvalidServiceSecretException;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.ServiceSecretsProperties;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.ServiceTokenIssuer;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.dto.ServiceTokenRequest;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest.dto.ServiceTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Emite un JWT de servicio-a-servicio ({@code typ: "service"}) para un servicio que todavía no
 * tiene manera de autenticarse con JWT (Spec 6.1 §Diseño-3 -- caso de origen: {@code ms-agent-runtime}
 * pidiéndole un token para llamar a {@code ms-ai-gateway}). No puede exigir JWT de entrada -- es
 * precisamente lo que el llamador no tiene todavía -- por eso queda {@code permitAll()} en
 * {@link com.atlas.supplychain.auth.infrastructure.config.security.SecurityConfig} y la autenticación
 * real la hace este controller a mano contra {@code X-Service-Secret}, en tiempo constante.
 */
@RestController
@RequestMapping("/service-tokens")
@RequiredArgsConstructor
public class ServiceTokenController {

    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";

    private final ServiceTokenIssuer serviceTokenIssuer;
    private final ServiceSecretsProperties serviceSecretsProperties;
    private final JwtProperties jwtProperties;

    @PostMapping
    public ServiceTokenResponse emitirToken(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String secretRecibido,
            @Valid @RequestBody ServiceTokenRequest request) {
        validarSecreto(request.serviceName(), secretRecibido);

        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.getServiceTokenTtlSeconds());
        String token = serviceTokenIssuer.emitirTokenPara(request.serviceName());
        return new ServiceTokenResponse(token, expiresAt);
    }

    private void validarSecreto(String serviceName, String secretRecibido) {
        String secretEsperado = serviceSecretsProperties.getServiceSecrets().get(serviceName);
        if (secretEsperado == null || secretEsperado.isBlank()
                || secretRecibido == null
                || !MessageDigest.isEqual(
                        secretEsperado.getBytes(StandardCharsets.UTF_8),
                        secretRecibido.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidServiceSecretException("Servicio o secreto inválido");
        }
    }
}
