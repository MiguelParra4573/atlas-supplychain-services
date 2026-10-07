package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.businessprocess.BusinessProcessService;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.BusinessProcessMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.BusinessProcessDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catálogo de procesos de negocio (Spec 1.0). Es mantenimiento **del proveedor del ERP**, no del
 * cliente final: quién contrata qué se decide del lado de Promethea. Por eso las autoridades son
 * `PROCESOS:*` y no cuelgan de `MODULOS`.
 */
@RestController
@RequestMapping("/business-processes")
@RequiredArgsConstructor
@Validated
public class BusinessProcessController {

    private final BusinessProcessService service;
    private final BusinessProcessMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('PROCESOS:CREAR')")
    public ResponseEntity<Void> crear(
            @Validated({BusinessProcessDTO.Crear.class}) @RequestBody BusinessProcessDTO dto)
            throws DuplicateRecordException {

        service.crear(mapper.dtoToDomain(dto));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('PROCESOS:EDITAR')")
    public ResponseEntity<Void> actualizar(
            @Validated({BusinessProcessDTO.Actualizar.class}) @RequestBody BusinessProcessDTO dto)
            throws EntityNotFoundException {

        service.actualizar(mapper.dtoToDomain(dto));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCESOS:CONSULTAR')")
    public ResponseEntity<BusinessProcessDTO> obtenerPorId(@PathVariable Long id)
            throws EntityNotFoundException {

        return ResponseEntity.ok(mapper.domainToDto(service.obtenerPorId(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROCESOS:ELIMINAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) throws EntityNotFoundException {
        service.eliminar(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROCESOS:LISTAR')")
    public ResponseEntity<List<BusinessProcessDTO>> listar() {
        return ResponseEntity.ok(service.listar().stream().map(mapper::domainToDto).toList());
    }

    /**
     * El catálogo completo con el estado de activación de **mi** empresa en cada fila. Un proceso
     * sin fila de activación sale con {@code active = false}: no contratado y no activo son lo
     * mismo de cara al menú.
     */
    @GetMapping("/my-company")
    @PreAuthorize("hasAuthority('PROCESOS_EMPRESA:LISTAR')")
    public ResponseEntity<List<BusinessProcessDTO>> listarDeMiEmpresa(
            @AuthenticationPrincipal CustomUserDetails principal) {

        Map<Long, Boolean> activacion = service.listarPorEmpresa(principal.getIdCompany()).stream()
                .collect(Collectors.toMap(CompanyProcess::getIdBusinessProcess,
                        companyProcess -> Boolean.TRUE.equals(companyProcess.getActive()),
                        (uno, otro) -> uno));

        List<BusinessProcessDTO> catalogo = service.listar().stream()
                .map(proceso -> {
                    BusinessProcessDTO dto = mapper.domainToDto(proceso);
                    dto.setActive(activacion.getOrDefault(proceso.getIdBusinessProcess(), Boolean.FALSE));
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(catalogo);
    }

    /**
     * Activa o desactiva un proceso **para la empresa del token**. Desactivar corta el acceso a las
     * pantallas de ese proceso, no solo las esconde del menú (ver {@code User.esVigente}).
     *
     * <p>El {@code idCompany} sale del principal y nunca de la URL. Tomarlo del path dejaba que el
     * administrador de una empresa apagara un proceso contratado por otra -- y como desactivar
     * corta acceso, eso deja a los usuarios de la otra empresa en 403 sobre pantallas que sí
     * pagaron. Ningún otro endpoint de {@code auth-server} acepta {@code idCompany} por URL.</p>
     *
     * <p>Si el proveedor necesita algún día activar procesos de un cliente desde su propia sesión,
     * eso es un caso de uso distinto y va detrás de una autoridad propia, no de la misma
     * {@code PROCESOS:EDITAR} que puede tener cualquier administrador.</p>
     */
    @PutMapping("/{id}/activation")
    @PreAuthorize("hasAuthority('PROCESOS_EMPRESA:EDITAR')")
    public ResponseEntity<Void> activar(
            @PathVariable Long id,
            @RequestBody ActivacionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) throws EntityNotFoundException {

        service.activar(principal.getIdCompany(), id, request.active());
        return ResponseEntity.ok().build();
    }

    /** Body mínimo: solo el estado deseado. */
    public record ActivacionRequest(boolean active) {
    }
}
