package com.atlas.supplychain.auth.infrastructure.maintenance.organization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.organization.agency.AgencyService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Agency;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.mappers.AgencyMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization.AgencyDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Vocabulario {@code AGENCIAS:*} sembrado por la migration 022 (Spec 4.2 del frontend).
 * {@code idCompany} sale siempre del principal autenticado, nunca del cuerpo de la petición
 * (mismo criterio que {@code RoleController}).
 */
@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService service;
    private final AgencyMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('AGENCIAS:CREAR')")
    public ResponseEntity<Void> crearAgency(
            @Validated({AgencyDTO.Crear.class}) @RequestBody AgencyDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.crearAgency(mapper.dtoToDomain(dto, principal.getIdCompany()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('AGENCIAS:EDITAR')")
    public ResponseEntity<Void> actualizarAgency(
            @Validated({AgencyDTO.Actualizar.class}) @RequestBody AgencyDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.actualizarAgency(mapper.dtoToDomain(dto, principal.getIdCompany()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AGENCIAS:CONSULTAR')")
    public ResponseEntity<AgencyDTO> obtenerAgencyPorId(
            @NotBlank @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        Agency agency = service.obtenerAgencyPorId(principal.getIdCompany(), id);
        return ResponseEntity.ok(mapper.domainToDto(agency));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AGENCIAS:ELIMINAR')")
    public ResponseEntity<Void> eliminarAgency(
            @NotBlank @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.eliminarAgency(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AGENCIAS:LISTAR')")
    public ResponseEntity<List<AgencyDTO>> listarAgenciesDeMiEmpresa(
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<AgencyDTO> agencies = service.listarAgenciesPorCompany(principal.getIdCompany())
                .stream().map(mapper::domainToDto).toList();
        return ResponseEntity.ok(agencies);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('AGENCIAS:LISTAR')")
    public PagedResult<AgencyDTO> listarAgenciesPaginadas(
            PageRequestDto pageRequest,
            @RequestBody SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<Agency> result = service.listarAgenciesPaginadas(
                principal.getIdCompany(), searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<AgencyDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
