package com.atlas.supplychain.auth.application.maintenance.authorization.user;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface UserService {

    void crearUsuario(User user);

    void actualizarUsuario(User user) throws EntityNotFoundException;

    void eliminarUsuario(String idUsuario, String idCompany) throws EntityNotFoundException;

    User obtenerUsuarioPorId(String idUsuario, String idCompany) throws EntityNotFoundException;

    List<User> listarUsuariosPorEmpresa(String idCompany);

    PagedResult<User> buscarUsuariosPaginados(List<FilterCriterion> filters, PageQuery pageQuery, String idCompany);
}
