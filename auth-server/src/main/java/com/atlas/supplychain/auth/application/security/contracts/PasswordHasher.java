package com.atlas.supplychain.auth.application.security.contracts;

public interface PasswordHasher {
    String hash(String raw);

    boolean matches(String raw, String hash);
}
