package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CustomUserDetails implements UserDetails {

    /**
     * Autoridades fijas de un principal de servicio (Spec 4.2 §5): ROLE_SERVICE marca el
     * principal como no-humano, y USUARIOS:LISTAR/ROLES:LISTAR son el mínimo indispensable
     * para que ms-reporte consuma POST /usuarios/search y GET /roles -- los dos endpoints
     * que Spec 4.2 integra, y que en el código real (no en la lectura rápida de la spec) están
     * protegidos con @PreAuthorize sobre autoridad efectiva (Spec 1.7), no con un simple
     * authenticated(). Este conjunto está hardcodeado acá, no sale de ningún Role de la BD:
     * un token de servicio nunca hereda el permisosEfectivos() de un usuario real.
     */
    private static final Set<String> SERVICE_AUTHORITIES = Set.of("ROLE_SERVICE", "USUARIOS:LISTAR", "ROLES:LISTAR");

    private final User user;
    private final Set<String> authorities;
    private final boolean serviceAccount;
    private final String serviceSubject;
    private final String serviceIdCompany;

    public CustomUserDetails(User user, Set<String> authorities) {
        this.user = user;
        this.authorities = authorities;
        this.serviceAccount = false;
        this.serviceSubject = null;
        this.serviceIdCompany = null;
    }

    private CustomUserDetails(String serviceSubject, String serviceIdCompany, boolean serviceAccount) {
        this.user = null;
        this.authorities = SERVICE_AUTHORITIES;
        this.serviceAccount = serviceAccount;
        this.serviceSubject = serviceSubject;
        this.serviceIdCompany = serviceIdCompany;
    }

    /**
     * Principal sintético para un JWT de servicio (claim {@code typ="service"}, Spec 4.2 §5):
     * se arma directo desde las claims del token verificado -- sin tocar userDetailsService ni
     * la tabla de usuarios, porque detrás no hay ningún {@link User} real.
     */
    public static CustomUserDetails forService(String subject, String idCompany) {
        return new CustomUserDetails(subject, idCompany, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    /** Nombres de rol reales (no autoridades) -- claim "roles" del JWT, Spec 1.7. */
    public List<String> getRoleNames() {
        if (serviceAccount) {
            return List.of();
        }
        return user.getRole() == null ? List.of() : List.of(user.getRole().getName());
    }

    @Override
    public String getPassword() {
        return serviceAccount ? null : user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return serviceAccount ? serviceSubject : user.getIdUser();
    }

    /**
     * Username real visible al usuario (ej. "miguel.parra") -- distinto de {@link #getUsername()},
     * que por convención de Spring Security devuelve el {@code idUser} estable (usado como
     * {@code sub} del JWT y para la rotación de refresh token, ver {@code CustomUserDetailsService}).
     * Se necesita un getter aparte porque {@code UserDetails.getUsername()} ya está comprometido
     * con ese otro significado.
     */
    public String getRealUsername() {
        return serviceAccount ? serviceSubject : user.getUsername();
    }

    public String getName() {
        return serviceAccount ? null : user.getName();
    }

    public String getLastName() {
        return serviceAccount ? null : user.getLastName();
    }

    public String getIdCompany() {
        return serviceAccount ? serviceIdCompany : user.getIdCompany();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (serviceAccount) {
            return true;
        }
        Instant lockUntil = user.getLockUntil();
        return lockUntil == null || lockUntil.isBefore(Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (serviceAccount) {
            return true;
        }
        Instant expiresAt = user.getPasswordExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    @Override
    public boolean isEnabled() {
        return serviceAccount || user.isState();
    }
}
