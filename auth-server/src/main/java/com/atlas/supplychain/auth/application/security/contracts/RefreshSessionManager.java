package com.atlas.supplychain.auth.application.security.contracts;

public interface RefreshSessionManager {
    IssueResult issueForLogin(String idUser, String idCompany);

    RotateResult rotate(String refreshTokenRaw);

    void revoke(String refreshTokenRaw);

    void revokeAllSessions(String idUser, String idCompany);

    long refreshTtlSeconds();

    record IssueResult(String rawToken) {
    }

    record RotateResult(String rawToken, String idUser, String idCompany) {
    }
}