package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.screengroup.ScreenGroupService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ScreenGroupMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ScreenGroupDTO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Carpetas dentro de un módulo (Spec 1.4). Protegido con {@code OPERACIONES:*} -- mismo criterio
 * que {@code OperationController#asignarRecursoAModulo}: conceptualmente es parte del flujo de
 * "Módulos y operaciones", no un catálogo propio con su propia pantalla de mantenimiento.
 */
@RestController
@RequestMapping("/modules/{idModule}/screen-groups")
@RequiredArgsConstructor
@Validated
public class ScreenGroupController {

    private final ScreenGroupService service;
    private final ScreenGroupMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> crear(
            @PathVariable Long idModule,
            @Validated({ScreenGroupDTO.Crear.class}) @RequestBody ScreenGroupDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        ScreenGroup group = mapper.dtoToDomain(dto, principal.getIdCompany(), idModule);
        service.crear(group);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> actualizar(
            @PathVariable Long idModule,
            @Validated({ScreenGroupDTO.Actualizar.class}) @RequestBody ScreenGroupDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        ScreenGroup group = mapper.dtoToDomain(dto, principal.getIdCompany(), idModule);
        service.actualizar(group);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idModule,
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.eliminar(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERACIONES:LISTAR')")
    public ResponseEntity<List<ScreenGroupDTO>> listarPorModulo(
            @PathVariable Long idModule,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        return ResponseEntity.ok(service.listarPorModulo(principal.getIdCompany(), idModule)
                .stream().map(mapper::domainToDto).toList());
    }
}
