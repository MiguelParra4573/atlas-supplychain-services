package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.menu.MenuService;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.MenuMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.MenuModuloDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vista compuesta sobre {@code User}+{@code Role}+{@code GrantedPermission}+{@code Operation}
 * +{@code Module} (Spec 1.18) -- no CRUD de ninguno de esos agregados, por eso no cuelga de
 * {@code UserController} ni de {@code ModuleController}. Sin {@code @PreAuthorize}: queda
 * {@code authenticated()} como el resto (RBAC granular es Spec 1.7, fuera de alcance acá).
 */
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService service;
    private final MenuMapper mapper;

    @GetMapping
    public ResponseEntity<List<MenuModuloDTO>> obtenerMenu(@AuthenticationPrincipal CustomUserDetails principal) {
        List<MenuModuloDTO> menu = mapper.domainToDto(
                service.obtenerMenu(principal.getUsername(), principal.getIdCompany()));
        return ResponseEntity.ok(menu);
    }
}
