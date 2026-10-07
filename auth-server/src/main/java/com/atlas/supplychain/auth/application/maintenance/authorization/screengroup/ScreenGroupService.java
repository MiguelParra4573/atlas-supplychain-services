package com.atlas.supplychain.auth.application.maintenance.authorization.screengroup;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;

import java.util.List;

public interface ScreenGroupService {

    void crear(ScreenGroup group) throws EntityNotFoundException;

    void actualizar(ScreenGroup group) throws EntityNotFoundException;

    void eliminar(String idCompany, Long idScreenGroup) throws EntityNotFoundException;

    List<ScreenGroup> listarPorModulo(String idCompany, Long idModule) throws EntityNotFoundException;
}
