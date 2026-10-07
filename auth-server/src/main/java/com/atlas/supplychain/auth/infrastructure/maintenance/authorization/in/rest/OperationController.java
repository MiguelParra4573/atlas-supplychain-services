package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.operation.OperationService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ModuleScreenOrder;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ScreenSummary;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.ModuleMapper;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.OperationMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ModuleDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ModuleScreenDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.OperationDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ScreenDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
@Validated
public class OperationController {

    private final OperationService service;
    private final OperationMapper mapper;
    private final ModuleMapper moduleMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('OPERACIONES:CREAR')")
    public ResponseEntity<Void> crearOperacion(
            @Validated({OperationDTO.Crear.class})
            @RequestBody OperationDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Operation operation = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.crearOperacion(operation);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> actualizarOperacion(
            @Validated({OperationDTO.Actualizar.class})
            @RequestBody OperationDTO dto,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        Operation operation = mapper.dtoToDomain(dto, principal.getIdCompany());
        service.actualizarOperacion(operation);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACIONES:CONSULTAR')")
    public ResponseEntity<OperationDTO> obtenerOperacionPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        return ResponseEntity.ok(mapper.domainToDto(service.obtenerOperacionPorId(principal.getIdCompany(), id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACIONES:ELIMINAR')")
    public ResponseEntity<Void> eliminarOperacion(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.eliminarOperacion(principal.getIdCompany(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/module/{moduleId}")
    @PreAuthorize("hasAuthority('OPERACIONES:LISTAR')")
    public ResponseEntity<?> listarOperacionesPorModulo(
            @PathVariable Long moduleId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<OperationDTO> operaciones = service.listarOperacionesPorModulo(principal.getIdCompany(), moduleId)
                .stream()
                .map(mapper::domainToDto)
                .toList();
        return ResponseEntity.ok(operaciones);
    }

    /**
     * Bandeja de entrada del administrador (Spec 1.2): todo lo que el auto-registro dio de alta y
     * todavía no está en ningún menú.
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasAuthority('OPERACIONES:LISTAR')")
    public ResponseEntity<List<OperationDTO>> listarOperacionesSinModulo(
            @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(service.listarOperacionesSinModulo(principal.getIdCompany())
                .stream()
                .map(mapper::domainToDto)
                .toList());
    }

    @GetMapping("/{id}/modules")
    @PreAuthorize("hasAuthority('OPERACIONES:CONSULTAR')")
    public ResponseEntity<List<ModuleDTO>> listarModulosDeOperacion(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        return ResponseEntity.ok(service.listarModulosDeOperacion(principal.getIdCompany(), id)
                .stream()
                .map(moduleMapper::domainToDto)
                .toList());
    }

    /**
     * Colgar una operación de un módulo del menú. {@code OPERACIONES:EDITAR} y no
     * {@code MODULOS:EDITAR}: lo que cambia es dónde aparece esta operación, y es la pantalla de
     * asignación de operaciones la que lo dispara.
     */
    @PostMapping("/{id}/modules/{moduleId}")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> asignarAModulo(
            @PathVariable Long id,
            @PathVariable Long moduleId,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.asignarAModulo(principal.getIdCompany(), id, moduleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/modules/{moduleId}")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> desasignarDeModulo(
            @PathVariable Long id,
            @PathVariable Long moduleId,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.desasignarDeModulo(principal.getIdCompany(), id, moduleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cuelga las 5 operaciones de una pantalla de un módulo en una sola llamada (Spec 1.3) --
     * reemplaza para el uso normal el alta operación-por-operación de {@link #asignarAModulo}, que
     * sigue viva para el caso borde de asignar una sola operación suelta.
     */
    @PostMapping("/resource/{resource}/modules/{moduleId}")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> asignarRecursoAModulo(
            @PathVariable String resource,
            @PathVariable Long moduleId,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.asignarRecursoAModulo(principal.getIdCompany(), resource, moduleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/resource/{resource}/modules/{moduleId}")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> desasignarRecursoDeModulo(
            @PathVariable String resource,
            @PathVariable Long moduleId,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.desasignarRecursoDeModulo(principal.getIdCompany(), resource, moduleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fija el orden completo de las pantallas de un módulo en una sola llamada (Spec 1.3), en vez
     * de un `PATCH` por ítem. Body: {@code resource -> displayOrder}.
     */
    @PutMapping("/modules/{moduleId}/order")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> reordenarPantallasDeModulo(
            @PathVariable Long moduleId,
            @RequestBody Map<String, Integer> displayOrderPorRecurso,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.reordenarPantallasDeModulo(principal.getIdCompany(), moduleId, displayOrderPorRecurso);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fija (o quita, con {@code idScreenGroup: null}) la carpeta de una pantalla dentro de este
     * módulo (Spec 1.4). No falla si la pantalla no está asignada al módulo -- no hace nada.
     */
    @PutMapping("/resource/{resource}/modules/{moduleId}/group")
    @PreAuthorize("hasAuthority('OPERACIONES:EDITAR')")
    public ResponseEntity<Void> asignarCarpetaARecurso(
            @PathVariable String resource,
            @PathVariable Long moduleId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.asignarCarpetaARecurso(principal.getIdCompany(), resource, moduleId, body.get("idScreenGroup"));
        return ResponseEntity.noContent().build();
    }

    /**
     * Buscador de pantallas (Spec 1.3): una fila por {@code resource}, con sus módulos actuales ya
     * resueltos -- la unidad de trabajo real de la pantalla de asignación en {@code client/}, que
     * antes de esta spec cargaba todo sin paginar y sin buscador (grilla de checkboxes por
     * operación individual, Spec 1.2). Query params simples (no el contrato genérico de
     * {@code FilterCriterion}) porque acá solo hace falta un filtro de texto sobre {@code resource}.
     */
    @GetMapping("/screens")
    @PreAuthorize("hasAuthority('OPERACIONES:LISTAR')")
    public PagedResult<ScreenDTO> buscarPantallas(
            @RequestParam(required = false) String resource,
            @RequestParam(defaultValue = "false") boolean unassigned,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<ScreenSummary> result = service.buscarPantallas(
                principal.getIdCompany(), resource, unassigned, new PageQuery(page, size, List.of()));

        List<ScreenDTO> content = result.content().stream()
                .map(screen -> new ScreenDTO(screen.resource(), screen.name(), screen.icon(), screen.idProcessStage(),
                        screen.modules().stream().map(moduleMapper::domainToDto).toList()))
                .toList();

        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }

    /**
     * Pantallas asignadas a un módulo con su override de orden actual (Spec 1.3) -- lo que la
     * pantalla de reordenamiento en {@code client/} necesita para pintar la lista editable.
     */
    @GetMapping("/modules/{moduleId}/screens")
    @PreAuthorize("hasAuthority('OPERACIONES:LISTAR')")
    public ResponseEntity<List<ModuleScreenDTO>> pantallasDeModulo(
            @PathVariable Long moduleId,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        List<ModuleScreenDTO> pantallas = service.pantallasDeModulo(principal.getIdCompany(), moduleId).stream()
                .map(p -> new ModuleScreenDTO(p.resource(), p.displayOrder(), p.idScreenGroup()))
                .toList();

        return ResponseEntity.ok(pantallas);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('OPERACIONES:LISTAR')")
    public PagedResult<OperationDTO> listarOperacionesPaginadas(
            PageRequestDto pageRequest,
            @RequestBody() SearchRequestDto searchRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<Operation> result = service.listarOperacionesPaginadas(
                principal.getIdCompany(), searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<OperationDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
