package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.security.contracts.PasswordHasher;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHistoryStore;
import com.atlas.supplychain.auth.application.security.contracts.PasswordPolicy;
import com.atlas.supplychain.auth.application.security.contracts.NotificationSender;
import com.atlas.supplychain.auth.application.security.contracts.PasswordResetTokenStore;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserStore;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordResponse;
import com.atlas.supplychain.auth.application.security.dto.GenericMessageResponse;
import com.atlas.supplychain.auth.application.security.dto.ResetPasswordRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ServiceException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom RNG = new SecureRandom();

    private final UserStore userStore;
    private final PasswordResetTokenStore tokenStore;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHistoryStore passwordHistoryStore;
    private final RefreshSessionManager refreshSessionManager;
    private final NotificationSender notificationSender;

    @Override
    public ForgotPasswordResponse forgot(ForgotPasswordRequest req) {

        String genericMsg = "Si el usuario existe, se enviaron instrucciones para restablecer la contraseña.";

        // Resolución por username (credencial real que el usuario conoce), no por idUser.
        // username es único global: el filtro de empresa se aplica como post-filtro sobre el
        // resultado ya obtenido, y un mismatch de idCompany se trata igual que "no encontrado"
        // para no revelar que la cuenta existe en otro tenant.
        var userOpt = userStore.findWithAuthoritiesByUsername(req.usuario())
                .filter(u -> req.idCompany() != null && req.idCompany().equals(u.getIdCompany()));
        if (userOpt.isEmpty()) {
            return new ForgotPasswordResponse(genericMsg);
        }

        var user = userOpt.get();
        Instant now = Instant.now();

        tokenStore.invalidateAllActive(user.getIdUser(), user.getIdCompany(), now);

        Duration resetTtl = passwordPolicy.resetTokenTtl();

        String rawToken = generateToken();
        String hash = sha256(rawToken);

        tokenStore.save(user.getIdUser(), user.getIdCompany(), hash, now.plus(resetTtl));

        boolean enviado = enviarNotification(user, rawToken, resetTtl);
        if (!enviado) {
            tokenStore.invalidateAllActive(user.getIdUser(), user.getIdCompany(), Instant.now());
        }

        log.info("reset-password solicitado: usuario={} empresa={} expira={} enviado={}",
                user.getIdUser(), user.getIdCompany(), now.plus(resetTtl), enviado);

        return new ForgotPasswordResponse(genericMsg);
    }

    private boolean enviarNotification(User user, String rawToken, Duration resetTtl) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }

        try {
            notificationSender.enviarResetPassword(user.getEmail(), user.getIdUser(), rawToken, resetTtl.toMinutes());
            return true;
        } catch (RuntimeException e) {
            log.error("No se pudo enviar la notification de reset de password para usuario={} empresa={}",
                    user.getIdUser(), user.getIdCompany(), e);
            return false;
        }
    }

    @Override
    public GenericMessageResponse reset(ResetPasswordRequest req) throws EntityNotFoundException {

        String hash = sha256(req.resetToken());
        Instant now = Instant.now();

        var token = tokenStore.findByHash(hash)
                .orElseThrow(() -> new BusinessRuleException(BusinessRuleCode.TOKEN_RESET_INVALIDO));

        if (!token.isValid(now)) {
            throw new BusinessRuleException(BusinessRuleCode.TOKEN_RESET_EXPIRADO);
        }

        if (!tokenStore.markUsed(token.id(), now)) {
            throw new BusinessRuleException(BusinessRuleCode.TOKEN_RESET_INVALIDO);
        }

        User user = userStore.findByIdUser(token.idUser());
        passwordPolicy.validateOrThrow(req.newPassword());
        var recent = passwordHistoryStore.findRecentHashes(
                user.getIdUser(), user.getIdCompany(), passwordPolicy.historyCount()
        );

        for (String oldHash : recent) {
            if (passwordHasher.matches(req.newPassword(), oldHash)) {
                throw new BusinessRuleException(BusinessRuleCode.PASSWORD_REUTILIZADA);
            }
        }

        passwordHistoryStore.save(user.getIdUser(), user.getIdCompany(), user.getPasswordHash());

        user.setPasswordHash(passwordHasher.hash(req.newPassword()));
        user.setPasswordChangedAt(now);
        user.setPasswordExpiresAt(now.plus(passwordPolicy.passwordExpiration()));
        user.setFailedAttempts(0);
        user.setLockUntil(null);

        userStore.save(user);

        refreshSessionManager.revokeAllSessions(user.getIdUser(), user.getIdCompany());

        return new GenericMessageResponse("Contraseña restablecida. Ahora puede iniciar sesión.");
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(value.getBytes()));
        } catch (Exception e) {
            throw new ServiceException("No se pudo procesar el token", e);
        }
    }
}
