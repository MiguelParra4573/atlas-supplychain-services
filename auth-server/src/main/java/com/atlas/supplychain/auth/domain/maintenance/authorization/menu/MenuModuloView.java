package com.atlas.supplychain.auth.domain.maintenance.authorization.menu;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Proyección de solo lectura de un {@code Module} con al menos una {@code Operation} concedida y
 * vigente, para el árbol de menú (Spec 1.18). No es la entidad {@code Module} completa: deja afuera
 * {@code state} e {@code idCompany}, que el frontend no necesita para pintar un menú. {@code
 * operations} ya viene ordenada por etapa/override de proceso (Spec 1.3).
 *
 * <p>{@code idBusinessProcess}/{@code businessProcessName} ordenan el menú (Spec 1.3) -- eje
 * distinto de {@code idMenuCategory}/{@code menuCategoryName} (Spec 1.4), que es lo que el shell
 * usa para agrupar el rail. Ambos pares {@code null} si el módulo es transversal/sin categoría.
 */
public record MenuModuloView(
        Long idModule,
        String name,
        @Nullable String icon,
        @Nullable Long idBusinessProcess,
        @Nullable String businessProcessName,
        @Nullable Long idMenuCategory,
        @Nullable String menuCategoryName,
        List<MenuOperacionView> operations) {}
