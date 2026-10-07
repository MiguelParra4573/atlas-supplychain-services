package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.processstage.ProcessStageService;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ProcessStageMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ProcessStageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de etapas de proceso (Spec 1.3). Mantenimiento del proveedor del ERP, igual que
 * {@code BusinessProcessController}: autoridades {@code PROCESS_STAGES:*}, no cuelgan de
 * {@code MODULOS}.
 */
@RestController
@RequestMapping("/process-stages")
@RequiredArgsConstructor
@Validated
public class ProcessStageController {

    private final ProcessStageService service;
    private final ProcessStageMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('PROCESS_STAGES:CREAR')")
    public ResponseEntity<Void> crear(
            @Validated({ProcessStageDTO.Crear.class}) @RequestBody ProcessStageDTO dto)
            throws DuplicateRecordException {

        service.crear(mapper.dtoToDomain(dto));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('PROCESS_STAGES:EDITAR')")
    public ResponseEntity<Void> actualizar(
            @Validated({ProcessStageDTO.Actualizar.class}) @RequestBody ProcessStageDTO dto)
            throws EntityNotFoundException, DuplicateRecordException {

        service.actualizar(mapper.dtoToDomain(dto));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCESS_STAGES:CONSULTAR')")
    public ResponseEntity<ProcessStageDTO> obtenerPorId(@PathVariable Long id) throws EntityNotFoundException {
        return ResponseEntity.ok(mapper.domainToDto(service.obtenerPorId(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCESS_STAGES:ELIMINAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) throws EntityNotFoundException {
        service.eliminar(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/business-process/{idBusinessProcess}")
    @PreAuthorize("hasAuthority('PROCESS_STAGES:LISTAR')")
    public ResponseEntity<List<ProcessStageDTO>> listarPorProceso(@PathVariable Long idBusinessProcess) {
        return ResponseEntity.ok(service.listarPorProceso(idBusinessProcess).stream()
                .map(mapper::domainToDto)
                .toList());
    }
}
