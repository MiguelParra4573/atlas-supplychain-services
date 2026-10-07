package com.atlas.supplychain.auth.infrastructure.config.security.listener;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.infrastructure.config.security.AccountLockoutProperties;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventsListener {

    private static final String COMPANY_ID_DESCONOCIDA = "UNKNOWN";

    private final UserRepository userRepository;
    private final AuditTrail auditTrail;
    private final AccountLockoutProperties accountLockoutProperties;

    @EventListener
    @Transactional
    public void onBadCredentials(org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent e) {
        String username = (String) e.getAuthentication().getPrincipal();

        Optional<User> maybeUser = userRepository.findByUsernameWithAuthorities(username);
        if (maybeUser.isEmpty()) {
            auditarLoginFallido(username, COMPANY_ID_DESCONOCIDA);
            return;
        }

        User user = maybeUser.get();
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        int maxAttempts = accountLockoutProperties.getMaxAttempts();
        if (attempts >= maxAttempts) {
            user.setLockUntil(Instant.now().plus(Duration.ofMinutes(accountLockoutProperties.getLockMinutes())));
        }
        actualizarSilenciando(user);

        auditarLoginFallido(username, user.getIdCompany());
        if (attempts >= maxAttempts) {
            auditTrail.registrar(new AuditEventCommand(
                    "LOGIN",
                    "USUARIO_BLOQUEADO",
                    "USUARIO",
                    user.getIdUser(),
                    "SEGURIDAD",
                    "HIGH",
                    user.getIdCompany(),
                    user.getIdUser(),
                    user.getIdUser(),
                    null,
                    "Usuario bloqueado por exceso de intentos fallidos"));
        }
    }

    private void auditarLoginFallido(String username, String companyId) {
        auditTrail.registrar(new AuditEventCommand(
                "LOGIN",
                "LOGIN_FALLIDO",
                "USUARIO",
                username,
                "SEGURIDAD",
                "MEDIUM",
                companyId,
                username,
                username,
                null,
                "Inicio de sesión fallido"));
    }

    @EventListener
    @Transactional
    public void onSuccess(org.springframework.security.authentication.event.AuthenticationSuccessEvent e) {
        Object principal = e.getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails cud)) {
            return;
        }

        Optional<User> maybeUser = userRepository.findByIdUsuarioWithAuthorities(cud.getUsername());
        if (maybeUser.isEmpty()) {
            return;
        }

        User user = maybeUser.get();
        user.setFailedAttempts(0);
        user.setLockUntil(null);
        actualizarSilenciando(user);
    }

    // El usuario ya estaba cargado momentos antes en el mismo flujo de login; que haya
    // desaparecido entre la lectura y el guardado es una carrera improbable, no algo que
    // este listener de eventos de Spring Security esté en posición de propagar.
    private void actualizarSilenciando(User user) {
        try {
            userRepository.actualizarUsuario(user);
        } catch (EntityNotFoundException ex) {
            log.warn("No se pudo actualizar el estado de login de {}: usuario ya no existe", user.getIdUser());
        }
    }
}
