package com.atlas.supplychain.auth.infrastructure.config.security.jwt.in.rest;

import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Publica la clave pública RS256 como JWK Set (RFC 7517). Los demás servicios se configuran como
 * OAuth2 Resource Server con {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}
 * apuntando aquí, sin compartir el archivo de la clave.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey key = jwtService.publicKey();
        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", jwtProperties.getKeyId(),
                "n", base64Url(key.getModulus()),
                "e", base64Url(key.getPublicExponent()));
        return Map.of("keys", List.of(jwk));
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        // toByteArray agrega un byte 0x00 de signo cuando el bit alto está encendido; JWK no lo lleva.
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] sinSigno = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, sinSigno, 0, sinSigno.length);
            bytes = sinSigno;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
