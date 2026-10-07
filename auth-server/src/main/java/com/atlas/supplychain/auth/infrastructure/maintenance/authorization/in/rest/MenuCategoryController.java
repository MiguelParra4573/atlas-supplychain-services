package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.menucategory.MenuCategoryService;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.MenuCategoryMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.MenuCategoryDTO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de botones del rail (Spec 1.4). Dueño la empresa -- a diferencia de
 * {@code ProcessStageController}/{@code BusinessProcessController} (proveedor). */
@RestController
@RequestMapping("/menu-categories")
@RequiredArgsConstructor
@Validated
public class MenuCategoryController {

    private final MenuCategoryService service;
    private final MenuCategoryMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('MENU_CATEGORIES:CREAR')")
    public ResponseEntity<Void> crear(
            @Validated({MenuCategoryDTO.Crear.class}) @RequestBody MenuCategoryDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws DuplicateRecordException {

        MenuCategory category = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.crear(category);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('MENU_CATEGORIES:EDITAR')")
    public ResponseEntity<Void> actualizar(
            @Validated({MenuCategoryDTO.Actualizar.class}) @RequestBody MenuCategoryDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException, DuplicateRecordException {

        MenuCategory category = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.actualizar(category);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_CATEGORIES:CONSULTAR')")
    public ResponseEntity<MenuCategoryDTO> obtenerPorId(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        return ResponseEntity.ok(mapper.domainToDto(service.obtenerPorId(principal.getIdCompany(), id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_CATEGORIES:ELIMINAR')")
    public ResponseEntity<Void> eliminar(
            @NotNull @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.eliminar(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/estado/{state}")
    @PreAuthorize("hasAuthority('MENU_CATEGORIES:LISTAR')")
    public ResponseEntity<List<MenuCategoryDTO>> listarPorState(
            @NotNull @PathVariable Boolean state,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<MenuCategoryDTO> categorias = service.listarPorState(principal.getIdCompany(), state)
                .stream().map(mapper::domainToDto).toList();

        return ResponseEntity.ok(categorias);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('MENU_CATEGORIES:LISTAR')")
    public PagedResult<MenuCategoryDTO> listarPaginado(
            PageRequestDto pageRequest,
            @RequestBody SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<MenuCategory> result = service.listarPaginado(
                principal.getIdCompany(), searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<MenuCategoryDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
