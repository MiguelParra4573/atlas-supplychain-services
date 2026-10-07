package com.atlas.supplychain.auth.application.security.contracts;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;

import java.util.Optional;

public interface UserStore {
    User findByIdUser(String idUser) throws EntityNotFoundException;

    Optional<User> findWithAuthorities(String idUser, String idCompany);

    /**
     * Resuelve por {@code username} (credencial que el usuario final conoce y tipea), no por
     * {@code idUser} (PK interna, nunca visible como credencial). Sin filtro de empresa porque
     * {@code username} es único global -- el llamador que sí conoce el {@code idCompany} del
     * solicitante debe comparar {@link User#getIdCompany()} del resultado contra el propio y
     * tratar cualquier mismatch como "no encontrado" (no revelar que la cuenta existe en otro
     * tenant).
     */
    Optional<User> findWithAuthoritiesByUsername(String username);

    void save(User user);
}
