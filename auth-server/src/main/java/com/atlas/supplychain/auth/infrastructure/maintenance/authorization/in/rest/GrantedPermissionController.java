package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission.GrantedPermissionService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.GrantedPermissionMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.GrantedPermissionDTO;
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
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Validated
public class GrantedPermissionController {

    private final GrantedPermissionService service;
    private final GrantedPermissionMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISOS:CREAR')")
    public ResponseEntity<Void> crearPermiso(
            @Validated({GrantedPermissionDTO.Crear.class})
            @RequestBody GrantedPermissionDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.asignarPermisoARol(principal.getIdCompany(), dto.getIdRole(), dto.getIdOperation());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('PERMISOS:EDITAR')")
    public ResponseEntity<Void> actualizarPermiso(
            @Validated({GrantedPermissionDTO.Actualizar.class})
            @RequestBody GrantedPermissionDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {
        // "Actualizar" un GrantedPermission es revocar el par anterior y conceder el nuevo:
        // el recurso es solo el par (Role, Operation), no tiene estado propio que parchear (Spec 1.2.2).
        service.revocarPermiso(principal.getIdCompany(), dto.getIdGrantedPermission());
        service.asignarPermisoARol(principal.getIdCompany(), dto.getIdRole(), dto.getIdOperation());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISOS:CONSULTAR')")
    public ResponseEntity<GrantedPermissionDTO> obtenerPermisoPorId(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        GrantedPermission permiso = service.obtenerPermisoPorId(principal.getIdCompany(), id);
        return ResponseEntity.ok(mapper.domainToDto(permiso));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISOS:ELIMINAR')")
    public ResponseEntity<Void> eliminarPermiso(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.revocarPermiso(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/role/{idRol}")
    @PreAuthorize("hasAuthority('PERMISOS:LISTAR')")
    public ResponseEntity<List<GrantedPermissionDTO>> listarPermisosPorRol(
            @NotNull @PathVariable Long idRol,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<GrantedPermissionDTO> permissions = service.listarPermisosPorRol(principal.getIdCompany(), idRol)
                .stream().map(mapper::domainToDto).toList();
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('PERMISOS:LISTAR')")
    public PagedResult<GrantedPermissionDTO> listarPermisosPaginadosPorFiltros(
            PageRequestDto pageRequest,
            @RequestBody SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<GrantedPermission> result = service.listarPermisosPaginados(
                principal.getIdCompany(), searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<GrantedPermissionDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
