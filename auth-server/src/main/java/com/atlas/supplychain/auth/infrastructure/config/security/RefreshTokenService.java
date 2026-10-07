package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import com.atlas.supplychain.auth.domain.common.exceptions.ServiceException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.RefreshToken;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.security.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;


@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshSessionManager {

    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final JwtProperties jwtProperties;

    private Duration refreshTtl() {
        return Duration.ofDays(jwtProperties.getRefreshTokenTtlDays());
    }

    @Override
    @Transactional
    public IssueResult issueForLogin(String idUser, String idCompany) {
        Instant now = Instant.now();
        repo.revokeAllActiveByIdUserAndCompany(idUser, idCompany, now);
        var issued = issueInternal(idUser, idCompany);
        return new IssueResult(issued.rawToken);
    }

    @Override
    @Transactional
    public RotateResult rotate(String refreshTokenRaw) {
        Instant now = Instant.now();
        String hash = sha256Hex(refreshTokenRaw);

        RefreshToken current = repo.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException(BusinessRuleCode.REFRESH_TOKEN_INVALIDO));

        if (!current.isActive(now)) {
            throw new BusinessRuleException(BusinessRuleCode.REFRESH_TOKEN_INVALIDO);
        }

        current.setRevokedAt(now);

        var next = issueInternal(current.getIdUser(), current.getIdCompany());
        current.setReplacedByHash(next.tokenHash);
        repo.save(current);

        return new RotateResult(next.rawToken, current.getIdUser(), current.getIdCompany());
    }

    @Override
    @Transactional
    public void revoke(String refreshTokenRaw) {
        String hash = sha256Hex(refreshTokenRaw);
        repo.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repo.save(token);
            }
        });
    }

    @Override
    @Transactional
    public void revokeAllSessions(String idUser, String idCompany) {
        repo.revokeAllActiveByIdUserAndCompany(idUser, idCompany, Instant.now());
    }

    @Override
    public long refreshTtlSeconds() {
        return refreshTtl().toSeconds();
    }

    private Issued issueInternal(String idUser, String idCompany) {
        String raw = generateOpaqueToken();
        String hash = sha256Hex(raw);
        Instant now = Instant.now();
        Instant exp = now.plus(refreshTtl());

        RefreshToken token = RefreshToken.builder().tokenHash(hash).idUser(idUser).idCompany(idCompany).createdAt(now).expiresAt(exp).build();

        repo.save(token);
        return new Issued(raw, hash, exp);
    }

    private static class Issued {
        final String rawToken;
        final String tokenHash;
        final Instant expiresAt;

        Issued(String rawToken, String tokenHash, Instant expiresAt) {
            this.rawToken = rawToken;
            this.tokenHash = tokenHash;
            this.expiresAt = expiresAt;
        }
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[64];
        RNG.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new ServiceException("No se pudo procesar el token", e);
        }
    }
}