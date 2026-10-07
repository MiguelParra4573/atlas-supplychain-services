package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.security.contracts.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String raw) {
        return passwordEncoder.encode(raw);
    }

    @Override
    public boolean matches(String raw, String hash) {
        return passwordEncoder.matches(raw, hash);
    }
}