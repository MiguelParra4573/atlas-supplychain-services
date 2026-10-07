package com.atlas.supplychain.auth.application.maintenance.authorization.processstage;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ProcessStage;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ProcessStageRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ProcessStageServiceImpl implements ProcessStageService {

    private final ProcessStageRepository repository;

    @Override
    public void crear(ProcessStage stage) throws DuplicateRecordException {
        validarCodigoUnico(stage);
        validarSecuenciaUnica(stage);
        repository.crear(stage);
    }

    @Override
    public void actualizar(ProcessStage stage) throws EntityNotFoundException, DuplicateRecordException {
        // Excluyendo el propio registro: si no cambió, el código/secuencia existente en la base es
        // el suyo, no una colisión con otra etapa.
        ProcessStage existente = repository.obtenerPorId(stage.getIdProcessStage());
        if (!existente.getCode().equals(stage.getCode())) {
            validarCodigoUnico(stage);
        }
        if (!existente.getSequence().equals(stage.getSequence())) {
            validarSecuenciaUnica(stage);
        }
        repository.actualizar(stage);
    }

    @Override
    public ProcessStage obtenerPorId(Long idProcessStage) throws EntityNotFoundException {
        return repository.obtenerPorId(idProcessStage);
    }

    @Override
    public void eliminar(Long idProcessStage) throws EntityNotFoundException {
        repository.eliminar(idProcessStage);
    }

    @Override
    public List<ProcessStage> listarPorProceso(Long idBusinessProcess) {
        return repository.listarPorProceso(idBusinessProcess);
    }

    // `code` y `sequence` son únicos dentro del proceso, no global -- dos procesos distintos
    // pueden tener los dos una etapa "01"/"INICIO" sin colisión.
    private void validarCodigoUnico(ProcessStage stage) throws DuplicateRecordException {
        if (repository.existsByCodeEnProceso(stage.getIdBusinessProcess(), stage.getCode())) {
            throw new DuplicateRecordException(
                    "Ya existe la etapa '" + stage.getCode() + "' en este proceso");
        }
    }

    private void validarSecuenciaUnica(ProcessStage stage) throws DuplicateRecordException {
        if (repository.existsBySequenceEnProceso(stage.getIdBusinessProcess(), stage.getSequence())) {
            throw new DuplicateRecordException(
                    "Ya existe una etapa con secuencia " + stage.getSequence() + " en este proceso");
        }
    }
}
