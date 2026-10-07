package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.db.security;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStoreImplTest {

    @Mock
    private UserRepository userRepository;

    private UserStoreImpl store;

    @Test
    void findByIdUserDevuelveUsuarioSiExisteYLanzaSiNoExiste() throws EntityNotFoundException {
        store = new UserStoreImpl(userRepository);
        User user = User.builder().idUser("u1").idCompany("c1").build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        assertThat(store.findByIdUser("u1")).isEqualTo(user);

        when(userRepository.findByIdUsuarioWithAuthorities("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> store.findByIdUser("desconocido"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findWithAuthoritiesDelegaEnElRepositorioConIdCompany() {
        store = new UserStoreImpl(userRepository);
        User user = User.builder().idUser("u1").idCompany("c1").build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1", "c1")).thenReturn(Optional.of(user));

        assertThat(store.findWithAuthorities("u1", "c1")).contains(user);
    }

    @Test
    void findWithAuthoritiesByUsernameDelegaEnElRepositorioSinIdCompany() {
        store = new UserStoreImpl(userRepository);
        User user = User.builder().idUser("u1").idCompany("c1").build();
        when(userRepository.findByUsernameWithAuthorities("juan.perez")).thenReturn(Optional.of(user));

        assertThat(store.findWithAuthoritiesByUsername("juan.perez")).contains(user);
    }

    @Test
    void saveDelegaEnActualizarUsuario() throws com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException {
        store = new UserStoreImpl(userRepository);
        User user = User.builder().idUser("u1").idCompany("c1").build();

        store.save(user);

        verify(userRepository).actualizarUsuario(user);
        // (ya no recibe idCompany aparte -- el objeto lo trae)
    }
}
