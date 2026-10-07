package com.atlas.supplychain.auth.application.maintenance.authorization.businessprocess;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;

import java.util.List;

/** Catálogo de procesos de negocio y su activación por empresa (Spec 1.0). */
public interface BusinessProcessService {

    void crear(BusinessProcess process) throws DuplicateRecordException;

    void actualizar(BusinessProcess process) throws EntityNotFoundException;

    BusinessProcess obtenerPorId(Long idBusinessProcess) throws EntityNotFoundException;

    void eliminar(Long idBusinessProcess) throws EntityNotFoundException;

    List<BusinessProcess> listar();

    List<CompanyProcess> listarPorEmpresa(String idCompany);

    void activar(String idCompany, Long idBusinessProcess, boolean active) throws EntityNotFoundException;
}
