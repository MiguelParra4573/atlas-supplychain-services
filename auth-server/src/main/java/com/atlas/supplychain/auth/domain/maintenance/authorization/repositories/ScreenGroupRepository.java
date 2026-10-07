package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;

import java.util.List;

public interface ScreenGroupRepository {

    void crear(ScreenGroup group);

    void actualizar(ScreenGroup group) throws EntityNotFoundException;

    void eliminar(String idCompany, Long idScreenGroup) throws EntityNotFoundException;

    /** Siempre se opera en el contexto de un módulo -- no hace falta buscar/paginar global. */
    List<ScreenGroup> listarPorModulo(String idCompany, Long idModule);
}
