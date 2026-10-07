package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.module.ModuleService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ModuleMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ModuleDTO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
@Validated
public class ModuleController {

    private final ModuleService service;
    private final ModuleMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('MODULOS:CREAR')")
    public ResponseEntity<Void> crearModulo(
            @Validated({ModuleDTO.Crear.class})
            @RequestBody ModuleDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Module module = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.crearModulo(module);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('MODULOS:EDITAR')")
    public ResponseEntity<Void> actualizarModulo(
            @Validated({ModuleDTO.Actualizar.class})
            @RequestBody ModuleDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        Module module = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.actualizarModulo(module);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULOS:CONSULTAR')")
    public ResponseEntity<ModuleDTO> obtenerModuloPorId(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        return ResponseEntity.ok(
                mapper.domainToDto(service.obtenerModuloPorId(principal.getIdCompany(), id))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULOS:ELIMINAR')")
    public ResponseEntity<Void> eliminarModulo(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.eliminarModulo(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/estado/{state}")
    @PreAuthorize("hasAuthority('MODULOS:LISTAR')")
    public ResponseEntity<List<ModuleDTO>> listarModulosPorState(
            @NotNull @PathVariable Boolean state,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<ModuleDTO> modulos = service.listarModulosPorState(principal.getIdCompany(), state)
                .stream().map(mapper::domainToDto).toList();

        return ResponseEntity.ok(modulos);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('MODULOS:LISTAR')")
    public PagedResult<ModuleDTO> listarModulosPaginadosPorFiltros(
            PageRequestDto pageRequest,
            @RequestBody SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<Module> result = service.listarModulosPaginados(
                principal.getIdCompany(), searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<ModuleDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
