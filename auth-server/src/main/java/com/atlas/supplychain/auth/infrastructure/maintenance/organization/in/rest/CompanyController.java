package com.atlas.supplychain.auth.infrastructure.maintenance.organization.in.rest;

import com.atlas.supplychain.auth.application.maintenance.organization.company.CompanyService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.mappers.CompanyMapper;
import com.atlas.supplychain.auth.shared.infrastructure.dto.PageRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.SearchRequestDto;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization.CompanyDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code Company} es la raíz del árbol de tenant (Spec 1.17): sin filtro de {@code idCompany}
 * en las lecturas -- a diferencia de {@code AgencyController}, ningún
 * método acá depende del {@code principal}. Vocabulario {@code EMPRESAS:*} sembrado por la
 * migration 022 (Spec 4.2 del frontend, hallazgo real: la matriz de permisos no podía listar
 * Organización porque nunca existió en el catálogo RBAC).
 */
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;
    private final CompanyMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('EMPRESAS:CREAR')")
    public ResponseEntity<Void> crearCompany(
            @Validated({CompanyDTO.Crear.class}) @RequestBody CompanyDTO dto) {

        service.crearCompany(mapper.dtoToDomain(dto));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('EMPRESAS:EDITAR')")
    public ResponseEntity<Void> actualizarCompany(
            @Validated({CompanyDTO.Actualizar.class}) @RequestBody CompanyDTO dto) throws EntityNotFoundException {

        service.actualizarCompany(mapper.dtoToDomain(dto));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPRESAS:CONSULTAR')")
    public ResponseEntity<CompanyDTO> obtenerCompanyPorId(
            @NotBlank @PathVariable String id) throws EntityNotFoundException {

        return ResponseEntity.ok(mapper.domainToDto(service.obtenerCompanyPorId(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPRESAS:ELIMINAR')")
    public ResponseEntity<Void> eliminarCompany(
            @NotBlank @PathVariable String id) throws EntityNotFoundException {

        service.eliminarCompany(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/state/{state}")
    @PreAuthorize("hasAuthority('EMPRESAS:LISTAR')")
    public ResponseEntity<List<CompanyDTO>> listarCompaniesPorState(@PathVariable boolean state) {

        List<CompanyDTO> companies = service.listarCompaniesPorState(state)
                .stream().map(mapper::domainToDto).toList();
        return ResponseEntity.ok(companies);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('EMPRESAS:LISTAR')")
    public PagedResult<CompanyDTO> listarCompaniesPaginadasPorFiltros(
            PageRequestDto pageRequest,
            @RequestBody SearchRequestDto searchRequest) {

        PagedResult<Company> result = service.listarCompaniesPaginadas(
                searchRequest.getFilters(), pageRequest.toPageQuery(searchRequest.getSorts()));
        List<CompanyDTO> content = result.content().stream().map(mapper::domainToDto).toList();
        return new PagedResult<>(content, result.page(), result.size(), result.totalElements());
    }
}
