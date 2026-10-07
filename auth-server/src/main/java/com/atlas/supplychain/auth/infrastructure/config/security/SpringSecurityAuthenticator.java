package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.security.contracts.Authenticator;
import com.atlas.supplychain.auth.application.security.contracts.UserIdentityProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringSecurityAuthenticator implements Authenticator, UserIdentityProvider {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public AuthenticatedUser authenticate(String username, String password) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails user)) {
            throw new RuntimeException("UserDetails inesperado: " + principal.getClass());
        }

        return new AuthenticatedUser(
                user.getUsername(),
                user.getRealUsername(),
                user.getName(),
                user.getLastName(),
                user.getIdCompany(),
                user.getRoleNames(),
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }

    @Override
    public UserIdentity load(String idUser) {
        // Llamado con el idUser estable (rotación de refresh token, ver AuthenticationService),
        // nunca con el username de login -- loadUserByIdUser, no loadUserByUsername.
        CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByIdUser(idUser);

        return new UserIdentity(
                user.getUsername(),
                user.getRealUsername(),
                user.getName(),
                user.getLastName(),
                user.getIdCompany(),
                user.getRoleNames(),
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }
}
