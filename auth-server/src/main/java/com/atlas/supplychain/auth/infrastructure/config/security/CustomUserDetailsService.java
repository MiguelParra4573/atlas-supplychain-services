package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithAuthorities(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo: " + username));

        return new CustomUserDetails(user, user.permisosEfectivos());
    }

    /**
     * Distinto de {@link #loadUserByUsername}: ese lo llama Spring Security (vía
     * {@code AuthenticationManager}) con lo que el usuario tipeó en el login (campo
     * {@code username}). Este lo usan {@code JwtAuthenticationFilter} (el {@code sub} del JWT ya
     * validado) y la rotación de refresh token -- ambos casos donde el identificador disponible
     * es {@code idUser} (interno, estable), no el username humano.
     */
    public UserDetails loadUserByIdUser(String idUser) throws UsernameNotFoundException {
        User user = userRepository.findByIdUsuarioWithAuthorities(idUser)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo: " + idUser));

        return new CustomUserDetails(user, user.permisosEfectivos());
    }

}
