package com.atlas.supplychain.auth.infrastructure.config.security.jwt;

import com.atlas.supplychain.auth.application.security.contracts.AccessTokenClaims;
import com.atlas.supplychain.auth.infrastructure.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    public void initKeys() throws Exception {
        String privatePath = jwtProperties.getPrivateKeyPath();
        String publicPath = jwtProperties.getPublicKeyPath();

        this.privateKey = loadPrivateKey(privatePath);
        this.publicKey = loadPublicKey(publicPath);
    }

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String key = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String key = Files.readString(Path.of(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }


    public long accessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenTtlMinutes() * 60L;
    }

    public String generarAccessToken(AccessTokenClaims claims) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .header().keyId(jwtProperties.getKeyId()).and()
                .subject(claims.username())
                .claim("idCompany", claims.idCompany())
                .claim("roles", claims.roles())
                .claim("authorities", claims.authorities())
                // "perms" (Spec 2.3): mismo valor que "authorities", bajo el nombre canónico que
                // consumen los demás servicios de la plataforma para
                // autorizar localmente sin acceso a auth. "authorities" se conserva tal cual
                // para no romper el contrato existente con auth-server mismo.
                .claim("perms", claims.authorities())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(jwtProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    RSAPrivateKey privateKey() {
        return privateKey;
    }

    public Claims validarToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
