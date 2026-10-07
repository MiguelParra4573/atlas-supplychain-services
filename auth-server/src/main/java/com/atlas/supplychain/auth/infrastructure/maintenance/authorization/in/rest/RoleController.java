package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.role.RoleService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.RoleMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.RoleDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.RoleMenuSummaryDTO;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {

    private final RoleService service;
    private final RoleMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLES:CREAR')")
    public ResponseEntity<Void> crearRol(
            @Validated({RoleDTO.Crear.class})
            @RequestBody RoleDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Role role = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.crearRol(role);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('ROLES:EDITAR')")
    public ResponseEntity<Void> actualizarRol(
            @Validated({RoleDTO.Actualizar.class})
            @RequestBody RoleDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        Role role = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.actualizarRol(role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES:CONSULTAR')")
    public ResponseEntity<RoleDTO> obtenerRolPorId(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        Role role = service.obtenerRolPorId(principal.getIdCompany(), id);
        return ResponseEntity.ok(mapper.domainToDto(role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES:ELIMINAR')")
    public ResponseEntity<Void> eliminarRol(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.eliminarRol(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES:LISTAR')")
    public ResponseEntity<List<RoleDTO>> listarRolesDeMiEmpresa(
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<RoleDTO> roles = service.listarRolesPorCompany(principal.getIdCompany())
                .stream()
                .map(mapper::domainToDto)
                .toList();

        return ResponseEntity.ok(roles);
    }

    /**
     * Spec 1.2: qué módulos del menú termina viendo cada rol. Devuelve **todos** los roles de la
     * empresa en una sola respuesta a propósito -- la lista de roles necesita una fila por rol, y
     * un endpoint por rol sería una llamada por fila.
     */
    @GetMapping("/menu-summary")
    @PreAuthorize("hasAuthority('ROLES:LISTAR')")
    public ResponseEntity<List<RoleMenuSummaryDTO>> resumenDeMenuPorRol(
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<RoleMenuSummaryDTO> resumen = service.resumenDeMenuPorRol(principal.getIdCompany())
                .stream()
                .map(vista -> RoleMenuSummaryDTO.builder()
                        .idRole(vista.idRole())
                        .modules(vista.modules().stream()
                                .map(modulo -> RoleMenuSummaryDTO.RoleMenuModuleDTO.builder()
                                        .idModule(modulo.idModule())
                                        .name(modulo.name())
                                        .screenCount(modulo.screenCount())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return ResponseEntity.ok(resumen);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('ROLES:LISTAR')")
    public PagedResult<RoleDTO> listarRolesPaginados(
            PageRequestDto pageRequest,
            @RequestBody() SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<Role> result = service.listarRolesPaginados(
                principal.getIdCompany(), searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<RoleDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
