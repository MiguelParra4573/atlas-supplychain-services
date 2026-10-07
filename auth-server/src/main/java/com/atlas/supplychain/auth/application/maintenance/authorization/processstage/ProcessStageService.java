package com.atlas.supplychain.auth.application.maintenance.authorization.processstage;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ProcessStage;

import java.util.List;

public interface ProcessStageService {

    void crear(ProcessStage stage) throws DuplicateRecordException;

    void actualizar(ProcessStage stage) throws EntityNotFoundException, DuplicateRecordException;

    ProcessStage obtenerPorId(Long idProcessStage) throws EntityNotFoundException;

    void eliminar(Long idProcessStage) throws EntityNotFoundException;

    List<ProcessStage> listarPorProceso(Long idBusinessProcess);
}
