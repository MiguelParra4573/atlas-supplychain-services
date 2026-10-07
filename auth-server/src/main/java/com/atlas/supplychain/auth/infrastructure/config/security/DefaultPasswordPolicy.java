package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.security.contracts.PasswordPolicy;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DefaultPasswordPolicy implements PasswordPolicy {

    private final PasswordPolicyProperties properties;

    @Override
    public void validateOrThrow(String newPassword) {
        if (newPassword == null || newPassword.length() < properties.getMinLength()) {
            throw new BusinessRuleException(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA,
                    "La contraseña debe tener al menos " + properties.getMinLength() + " caracteres.");
        }
        if (properties.isRequireUpper() && !newPassword.matches(".*[A-Z].*")) {
            throw new BusinessRuleException(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA,
                    "La contraseña debe incluir al menos una mayúscula.");
        }
        if (properties.isRequireLower() && !newPassword.matches(".*[a-z].*")) {
            throw new BusinessRuleException(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA,
                    "La contraseña debe incluir al menos una minúscula.");
        }
        if (properties.isRequireDigit() && !newPassword.matches(".*\\d.*")) {
            throw new BusinessRuleException(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA,
                    "La contraseña debe incluir al menos un número.");
        }
        if (properties.isRequireSpecial() && !newPassword.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessRuleException(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA,
                    "La contraseña debe incluir al menos un caracter especial.");
        }
    }

    @Override
    public int historyCount() {
        return properties.getHistoryCount();
    }

    @Override
    public Duration passwordExpiration() {
        return Duration.ofDays(properties.getExpirationDays());
    }

    @Override
    public Duration resetTokenTtl() {
        return Duration.ofMinutes(properties.getResetTokenTtlMinutes());
    }
}
