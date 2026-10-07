package com.atlas.supplychain.auth.domain.maintenance.authorization.screen;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;

import java.util.List;

/**
 * Una fila del buscador de pantallas (Spec 1.3): un {@code resource} distinto, con sus módulos
 * actuales ya resueltos. Unidad de trabajo de la pantalla de asignación en {@code client/} -- no
 * la operación individual, que es la granularidad de {@code Operation}.
 *
 * <p>{@code idProcessStage}/{@code name}/{@code icon} son los de la primera operación del recurso
 * que los declare (en la práctica las 5 operaciones CRUD de un mismo {@code resource} comparten
 * los mismos valores o ninguno -- lo mantiene consistente quien las auto-registra, no hay
 * constraint de base que lo fuerce). {@code name}/{@code icon} nullable: sin ellos, el frontend
 * cae al código de {@code resource} (Spec 1.4).</p>
 */
public record ScreenSummary(String resource, String name, String icon, Long idProcessStage, List<Module> modules) {
}


