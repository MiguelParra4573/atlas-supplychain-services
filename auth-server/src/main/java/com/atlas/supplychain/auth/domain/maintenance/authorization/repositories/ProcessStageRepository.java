package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ProcessStage;

import java.util.List;

public interface ProcessStageRepository {

    void crear(ProcessStage stage);

    void actualizar(ProcessStage stage) throws EntityNotFoundException;

    ProcessStage obtenerPorId(Long idProcessStage) throws EntityNotFoundException;

    void eliminar(Long idProcessStage) throws EntityNotFoundException;

    /** Catálogo completo, para resolver la secuencia por defecto del menú (Spec 1.3). */
    List<ProcessStage> listar();

    List<ProcessStage> listarPorProceso(Long idBusinessProcess);

    boolean existsByCodeEnProceso(Long idBusinessProcess, String code);

    boolean existsBySequenceEnProceso(Long idBusinessProcess, Integer sequence);
}
