package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.user.UserService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.UserMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService service;
    private final UserMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS:CREAR')")
    public ResponseEntity<Void> crearUsuario(
            @Validated({UserDTO.Crear.class}) @RequestBody UserDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) {
        User user = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.crearUsuario(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping()
    @PreAuthorize("hasAuthority('USUARIOS:EDITAR')")
    public ResponseEntity<Void> actualizarUsuario(
            @Validated({UserDTO.Actualizar.class})
            @RequestBody UserDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {
        User user = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.actualizarUsuario(user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS:CONSULTAR')")
    public ResponseEntity<UserDTO> obtenerUsuarioPorId(
            @NotBlank @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {
        return ResponseEntity.ok(mapper.domainToDto(service.obtenerUsuarioPorId(id, principal.getIdCompany())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS:ELIMINAR')")
    public ResponseEntity<Void> eliminar(
            @NotBlank @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {
        service.eliminarUsuario(id, principal.getIdCompany());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS:LISTAR')")
    public ResponseEntity<List<UserDTO>> listarUsuariosDeMiEmpresa(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<UserDTO> usuarios = service.listarUsuariosPorEmpresa(principal.getIdCompany())
                .stream()
                .map(mapper::domainToDto)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('USUARIOS:LISTAR')")
    public PagedResult<UserDTO> listarUsuariosPaginadosPorFiltros(
            PageRequestDto pageRequest,
            @RequestBody() SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PagedResult<User> result = service.buscarUsuariosPaginados(
                searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()), principal.getIdCompany());
        List<UserDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
