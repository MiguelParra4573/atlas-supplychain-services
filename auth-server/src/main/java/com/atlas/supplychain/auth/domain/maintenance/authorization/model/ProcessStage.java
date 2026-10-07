package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Etapa ordenada de un {@link BusinessProcess} (Spec 1.3): Clientes -&gt; Cotización -&gt; Pedido
 * -&gt; Factura. Catálogo del proveedor, global como {@link BusinessProcess} -- no lleva
 * {@code idCompany}. {@code sequence} es el orden por defecto del menú para cualquier
 * {@code Operation} que declare esta etapa; la empresa puede sobreescribirlo con
 * {@code module_operation.display_order}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStage {

    private Long idProcessStage;

    private Long idBusinessProcess;

    /** Código estable dentro del proceso (ej. {@code COTIZACION}). Único por proceso, no global. */
    private String code;

    private String name;

    /** Orden dentro del proceso. Único por proceso -- dos etapas no pueden empatar. */
    private Integer sequence;

    /** Baja lógica del catálogo. */
    private Boolean state;
}
