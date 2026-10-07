package com.atlas.supplychain.auth.infrastructure.config.security.jwt;

import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Emite el JWT de servicio-a-servicio (RS256, {@code sub: "svc-<serviceName>"}, {@code typ: "service"},
 * TTL configurable via {@code security.jwt.service-token-ttl-seconds}, 60s por defecto) reutilizando
 * la clave privada ya cargada por {@link JwtService}. Se emite uno por llamada: cachear el token acá
 * introduciría un modo de fallo (token cacheado ya expirado) sin ganancia medible frente al costo
 * de firmar RSA -- el llamador remoto (ej. ms-agent-runtime vía {@code POST /api/v1/service-tokens},
 * Spec 6.1) sí cachea del otro lado, porque ahí de por medio hay un round-trip HTTP.
 */
@Component
@RequiredArgsConstructor
public class ServiceTokenIssuer {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public String emitirTokenPara(String serviceName) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .header().keyId(jwtProperties.getKeyId()).and()
                .subject("svc-" + serviceName)
                .issuer(jwtProperties.getIssuer())
                .claim("typ", "service")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(jwtProperties.getServiceTokenTtlSeconds())))
                .signWith(jwtService.privateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
