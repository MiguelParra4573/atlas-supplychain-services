package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;


import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    void crearUsuario(User user);

    void actualizarUsuario(User user) throws EntityNotFoundException;

    Optional<User> obtenerUsuarioPorId(String idCompany, String idUsuario);

    Optional<User> findByIdUsuarioWithAuthorities(String idUsuario);

    /**
     * Login real: {@code username} es el campo que el usuario final escribe en el formulario
     * (independiente de {@code email}), a diferencia de {@code idUser} (PK interna, generada por
     * el backend, nunca visible como credencial). Sin filtro de empresa por el mismo motivo que
     * {@link #findByIdUsuarioWithAuthorities(String)}: no hay tenant disponible antes de
     * autenticar.
     */
    Optional<User> findByUsernameWithAuthorities(String username);

    boolean existsByUsername(String username);

    Optional<User> findByIdUsuarioWithAuthorities(String idUsuario, String idCompany);

    void eliminarUsuario(String idCompany, String idUsuario) throws EntityNotFoundException;

    List<User> listarUsuariosPorCompany(String idCompany);

    PagedResult<User> listarUsuariosPaginadosPorFiltros(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    boolean existsByRole(String idCompany, Long idRol);

}
