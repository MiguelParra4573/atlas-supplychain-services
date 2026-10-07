package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.application.security.contracts.UserStore;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserStoreImpl implements UserStore {

    private final UserRepository userRepository;

    @Override
    public User findByIdUser(String idUser) throws EntityNotFoundException {
        return userRepository.findByIdUsuarioWithAuthorities(idUser)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUser));
    }

    @Override
    public Optional<User> findWithAuthorities(String idUser, String idCompany) {
        return userRepository.findByIdUsuarioWithAuthorities(idUser, idCompany);
    }

    @Override
    public Optional<User> findWithAuthoritiesByUsername(String username) {
        return userRepository.findByUsernameWithAuthorities(username);
    }

    @Override
    public void save(User user) {
        try {
            userRepository.actualizarUsuario(user);
        } catch (EntityNotFoundException e) {
            // El usuario ya estaba cargado momentos antes (login/reset/change-password);
            // que haya desaparecido entre la lectura y el guardado es un bug de carrera, no
            // algo que este contrato (UserStore.save, sin throws) esté preparado a exponer.
            throw new IllegalStateException("Usuario desapareció entre lectura y guardado: " + user.getIdUser(), e);
        }
    }
}
