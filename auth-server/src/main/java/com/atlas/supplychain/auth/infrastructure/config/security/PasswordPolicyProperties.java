package com.atlas.supplychain.auth.infrastructure.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "atlas.password-policy")
public class PasswordPolicyProperties {

    private int minLength = 10;
    private boolean requireUpper = true;
    private boolean requireLower = true;
    private boolean requireDigit = true;
    private boolean requireSpecial = true;
    private int historyCount = 5;
    private int expirationDays = 90;
    private int resetTokenTtlMinutes = 20;
}
