package com.atlas.supplychain.auth.application.maintenance.authorization.screengroup;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ScreenGroupServiceImpl implements ScreenGroupService {

    private final ScreenGroupRepository repository;
    private final ModuleRepository moduleRepository;

    @Override
    public void crear(ScreenGroup group) throws EntityNotFoundException {
        // El módulo tiene que existir y ser de la empresa antes de colgarle una carpeta.
        moduleRepository.obtenerModuloPorId(group.getIdCompany(), group.getIdModule());
        if (group.getDisplayOrder() == null) {
            group.setDisplayOrder(0);
        }
        repository.crear(group);
    }

    @Override
    public void actualizar(ScreenGroup group) throws EntityNotFoundException {
        repository.actualizar(group);
    }

    @Override
    public void eliminar(String idCompany, Long idScreenGroup) throws EntityNotFoundException {
        repository.eliminar(idCompany, idScreenGroup);
    }

    @Override
    public List<ScreenGroup> listarPorModulo(String idCompany, Long idModule) throws EntityNotFoundException {
        moduleRepository.obtenerModuloPorId(idCompany, idModule);
        return repository.listarPorModulo(idCompany, idModule);
    }
}
