package com.atlas.supplychain.auth.domain.maintenance.authorization.menu;

import java.util.List;

/**
 * Proyección de solo lectura de un {@code Module} con al menos una {@code Operation} concedida
 * y vigente, para el árbol de menú (Spec 1.18). No es la entidad {@code Module} completa: deja
 * afuera {@code state} e {@code idCompany}, que el frontend no necesita para pintar un menú.
 * {@code operations} ya viene ordenada por etapa/override de proceso (Spec 1.3).
 *
 * <p>{@code idBusinessProcess}/{@code businessProcessName} ordenan el menú (Spec 1.3) -- eje
 * distinto de {@code idMenuCategory}/{@code menuCategoryName} (Spec 1.4), que es lo que el shell
 * usa para agrupar el rail. Ambos pares {@code null} si el módulo es transversal/sin categoría.</p>
 */
public record MenuModuloView(Long idModule, String name, String icon,
                              Long idBusinessProcess, String businessProcessName,
                              Long idMenuCategory, String menuCategoryName,
                              List<MenuOperacionView> operations) {
}
