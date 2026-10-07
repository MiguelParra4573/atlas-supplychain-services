package com.atlas.supplychain.auth.application.security.contracts;

public interface AccessTokenGenerator {
    String generate(AccessTokenClaims claims);

    long ttlSeconds();
}
