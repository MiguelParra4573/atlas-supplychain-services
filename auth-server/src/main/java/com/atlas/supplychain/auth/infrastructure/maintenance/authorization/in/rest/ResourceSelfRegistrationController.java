package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.authorization.module.ResourceSelfRegistrationService;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.OperationSelfRegistrationDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ResourceSelfRegistrationDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Auto-registro RBAC de un micro de negocio nuevo: este controller lo llama exclusivamente un JWT
 * de servicio ({@code typ="service"}, ej. {@code sub="svc-ms-inventario"}) al arrancar -- nunca un
 * usuario, ni siquiera admin. A diferencia de {@link ModuleController} (CRUD de módulos de una
 * empresa, usuario humano), acá no hay tenant en el principal.
 *
 * <p>{@code hasAuthority('ROLE_SERVICE')} en vez de un tag {@code MODULOS:*} del vocabulario
 * cerrado (004-normalizar-catalogo-rbac.sql): {@code ROLE_SERVICE} ya es la autoridad sintética
 * que {@link com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails#forService}
 * concede a todo principal de servicio (Spec 4.2 §5) -- reusarla acá evita inventar un segundo
 * mecanismo de "es un servicio" en paralelo al que ya reconoce {@code JwtAuthenticationFilter}.
 * Deliberadamente NO {@code hasAuthority('MODULOS:CREAR')}: esa autoridad nunca la tiene un
 * principal de servicio (Spec 4.2 §5, {@code SERVICE_AUTHORITIES} es una lista fija que no
 * incluye ninguna autoridad de escritura), y un usuario humano -- admin incluido -- no debe poder
 * registrar operaciones por esta vía de todas formas.</p>
 *
 * <p>La ruta sigue siendo {@code /modules/self-register} aunque desde la Spec 1.2 no registre
 * ningún módulo: es el endpoint que ya conocen los servicios generados con el arquetipo, y
 * moverlo rompería a cualquiera que todavía no se haya actualizado sin ganar nada. El payload sí
 * acepta las dos formas -- ver {@link ResourceSelfRegistrationDTO#resourceEfectivo()}.</p>
 */
@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
@Validated
public class ResourceSelfRegistrationController {

    private final ResourceSelfRegistrationService service;

    @PostMapping("/self-register")
    @PreAuthorize("hasAuthority('ROLE_SERVICE')")
    public ResponseEntity<Void> autoRegistrarOperaciones(@Valid @RequestBody ResourceSelfRegistrationDTO dto) {
        // Que venga 'resource' (o su alias 'name') lo valida el propio DTO con @AssertTrue, no un
        // if acá: así el 400 sale por el mismo camino que cualquier otro campo inválido.
        service.registrarOperacionesEnTodasLasEmpresas(dto.resourceEfectivo(), aOperationTemplates(dto));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * {@code idCompany}/{@code idOperation} quedan sin asignar a propósito: esta plantilla no
     * pertenece a ninguna empresa concreta todavía, {@code ResourceSelfRegistrationServiceImpl} la
     * replica por cada empresa activa.
     */
    private List<Operation> aOperationTemplates(ResourceSelfRegistrationDTO dto) {
        return dto.getOperations().stream()
                .map(ResourceSelfRegistrationController::anOperationTemplate)
                .toList();
    }

    private static Operation anOperationTemplate(OperationSelfRegistrationDTO dto) {
        return Operation.builder()
                .tag(dto.getTag())
                .path(dto.getPath())
                .httpMethod(dto.getHttpMethod())
                .permitAll(dto.isPermitAll())
                .state(true)
                .build();
    }
}
