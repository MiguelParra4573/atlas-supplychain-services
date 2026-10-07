package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload de {@code POST /modules/self-register}: un micro de negocio nuevo, clonado de
 * {@code atlas-ms-archetype}, declara sus operaciones protegidas para que {@code auth-server} las
 * siembre en {@code auth.operations} de todas las empresas activas.
 *
 * <p>Desde la Spec 1.2 **no registra ningún módulo**. Antes traía {@code name}/{@code basePath}/
 * {@code icon}/{@code category} y {@code auth-server} creaba con eso un {@code Module} homónimo en
 * cada empresa, pisando en cada redeploy la personalización que la empresa hubiera hecho de su
 * menú. Ahora declara el {@code resource} -- el código estable de la pantalla, primera mitad de la
 * autoridad {@code RECURSO:OPERACION} -- y dónde se agrupan esas operaciones en el menú lo decide
 * un administrador de cada empresa.</p>
 *
 * <p>No lleva {@code idCompany}: es la plantilla que se replica en cada empresa activa -- el
 * llamante es siempre un JWT de servicio ({@code typ="service"}), nunca un usuario de una empresa
 * concreta.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSelfRegistrationDTO {

    @Size(max = 150)
    private String resource;

    /**
     * Campo del contrato viejo, aceptado como alias de {@link #resource} para que un servicio que
     * todavía no se actualizó al arquetipo nuevo siga registrándose durante la ventana de
     * despliegue. Es exactamente el valor con el que la migración inicializó {@code resource}
     * (changelog 025: {@code resource = modules.name}), así que un cliente viejo sigue apuntando a
     * la misma autoridad. No agregar lógica nueva sobre este campo.
     *
     * @deprecated usar {@link #resource}; se elimina cuando ningún {@code ms-*} mande {@code name}.
     */
    @Deprecated(since = "Spec 1.2")
    @Size(max = 150)
    private String name;

    @NotEmpty
    @Valid
    private List<OperationSelfRegistrationDTO> operations;

    /**
     * El {@code resource} efectivo: el campo nuevo si vino, si no el {@code name} del contrato
     * viejo. {@code null} si no vino ninguno de los dos.
     */
    public String resourceEfectivo() {
        String efectivo = resource != null && !resource.isBlank() ? resource : name;
        return efectivo == null || efectivo.isBlank() ? null : efectivo.trim();
    }

    /**
     * Validación declarativa de "vino {@code resource} o su alias {@code name}", en vez de un
     * {@code if} en el controller: así el 400 lo produce el mismo camino que cualquier otro campo
     * inválido ({@code MethodArgumentNotValidException} → {@code GlobalExceptionHandler}) y no una
     * excepción suelta que el handler genérico convertiría en 500.
     */
    @AssertTrue(message = "Falta 'resource' (o el alias 'name' del contrato viejo): sin él la autoridad quedaría como ':TAG'.")
    @JsonIgnore
    public boolean isResourcePresente() {
        return resourceEfectivo() != null;
    }
}
