package com.atlas.supplychain.auth.infrastructure.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "atlas.account-lockout")
public class AccountLockoutProperties {

    private int maxAttempts = 5;
    private int lockMinutes = 15;
}
