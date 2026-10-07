package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;
import com.atlas.supplychain.auth.application.security.contracts.AccessTokenClaims;
import com.atlas.supplychain.auth.application.security.contracts.AccessTokenGenerator;
import com.atlas.supplychain.auth.application.security.contracts.Authenticator;
import com.atlas.supplychain.auth.application.security.contracts.RefreshSessionManager;
import com.atlas.supplychain.auth.application.security.contracts.UserIdentityProvider;
import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.LogoutRequest;
import com.atlas.supplychain.auth.application.security.dto.RefreshRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthenticationService {

    private final Authenticator authenticator;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshSessionManager refreshSessionManager;
    private final UserIdentityProvider userIdentityProvider;
    private final AuditTrail auditTrail;

    public TokenResponse login(LoginRequest request) {
        var user = authenticator.authenticate(request.getUsuario(), request.getContrasena());

        String access = accessTokenGenerator.generate(
                new AccessTokenClaims(user.idUser(), user.idCompany(), user.roles(), user.authorities()));
        var refresh = refreshSessionManager.issueForLogin(user.idUser(), user.idCompany());

        TokenResponse response = TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(access)
                .expiresIn(accessTokenGenerator.ttlSeconds())
                .refreshToken(refresh.rawToken())
                .refreshExpiresIn(refreshSessionManager.refreshTtlSeconds())
                .user(TokenResponse.UserInfo.builder()
                        .usuario(user.username())
                        .name(user.name())
                        .lastName(user.lastName())
                        .idCompany(user.idCompany())
                        .roles(user.roles())
                        .authorities(user.authorities())
                        .build())
                .build();

        String rolPrincipal = user.roles() != null && !user.roles().isEmpty() ? user.roles().get(0) : null;
        auditTrail.registrar(new AuditEventCommand(
                "LOGIN",
                "LOGIN_EXITOSO",
                "USUARIO",
                user.idUser(),
                "SEGURIDAD",
                "LOW",
                user.idCompany(),
                user.idUser(),
                user.idUser(),
                rolPrincipal,
                "Inicio de sesión exitoso"));

        return response;
    }

    public TokenResponse refresh(RefreshRequest request) {
        var rotated = refreshSessionManager.rotate(request.getRefreshToken());
        var user = userIdentityProvider.load(rotated.idUser());

        if (!rotated.idCompany().equals(user.idCompany())) {
            throw new IllegalArgumentException("Refresh token inválido para el tenant");
        }

        String access = accessTokenGenerator.generate(
                new AccessTokenClaims(user.idUser(), user.idCompany(), user.roles(), user.authorities()));

        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(access)
                .expiresIn(accessTokenGenerator.ttlSeconds())
                .refreshToken(rotated.rawToken())
                .refreshExpiresIn(refreshSessionManager.refreshTtlSeconds())
                .user(TokenResponse.UserInfo.builder()
                        .usuario(user.username())
                        .name(user.name())
                        .lastName(user.lastName())
                        .idCompany(user.idCompany())
                        .roles(user.roles())
                        .authorities(user.authorities())
                        .build())
                .build();
    }

    public void logout(LogoutRequest request) {
        refreshSessionManager.revoke(request.getRefreshToken());
    }
}
