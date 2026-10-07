package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHasher;
import com.atlas.supplychain.auth.application.security.contracts.PasswordHistoryStore;
import com.atlas.supplychain.auth.application.security.contracts.PasswordPolicy;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserStore;
import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserStore userStore;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHistoryStore passwordHistoryStore;
    private final RefreshSessionManager refreshSessionManager;
    private final AuditTrail auditTrail;

    @Override
    public void changeMyPassword(String idUser, String companyId, ChangePasswordRequest req)
            throws EntityNotFoundException {

        User user = userStore.findByIdUser(idUser);

        if (!passwordHasher.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException(BusinessRuleCode.PASSWORD_ACTUAL_INCORRECTA);
        }

        passwordPolicy.validateOrThrow(req.newPassword());
        var recent = passwordHistoryStore.findRecentHashes(idUser, companyId, passwordPolicy.historyCount());
        for (String oldHash : recent) {
            if (passwordHasher.matches(req.newPassword(), oldHash)) {
                throw new BusinessRuleException(BusinessRuleCode.PASSWORD_REUTILIZADA);
            }
        }

        passwordHistoryStore.save(idUser, companyId, user.getPasswordHash());

        user.setPasswordHash(passwordHasher.hash(req.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordExpiresAt(Instant.now().plus(passwordPolicy.passwordExpiration()));
        user.setFailedAttempts(0);
        user.setLockUntil(null);

        userStore.save(user);

        auditTrail.registrar(new AuditEventCommand(
                "UPDATE",
                "CAMBIO_PASSWORD",
                "USUARIO",
                idUser,
                "SEGURIDAD",
                "MEDIUM",
                companyId,
                idUser,
                idUser,
                null,
                "Cambio de contraseña propio"
        ));

        refreshSessionManager.revokeAllSessions(idUser, companyId);
    }
}
