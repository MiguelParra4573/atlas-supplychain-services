package com.atlas.supplychain.auth.domain.maintenance.authorization.screen;

/**
 * Una pantalla asignada a un módulo, con su override de orden actual (Spec 1.3) y su carpeta
 * actual dentro de ese módulo (Spec 1.4) -- lo que "Módulos y operaciones" en {@code client/}
 * necesita para reordenar y para preseleccionar la carpeta de cada pantalla. {@code displayOrder}/
 * {@code idScreenGroup} {@code null} significa que todavía no se tocó, no que valga 0/ninguna.
 */
public record ModuleScreenOrder(String resource, Integer displayOrder, Long idScreenGroup) {
}
