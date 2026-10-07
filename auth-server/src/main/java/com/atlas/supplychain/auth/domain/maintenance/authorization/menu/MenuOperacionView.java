package com.atlas.supplychain.auth.domain.maintenance.authorization.menu;

/**
 * Proyección de solo lectura de una {@code Operation} concedida y vigente, para el árbol de
 * menú (Spec 1.18). No es la entidad {@code Operation} completa: deja afuera {@code state},
 * {@code permitAll} e {@code idCompany}, que el frontend no necesita para pintar un menú.
 *
 * <p>{@code resource} viaja desde la Spec 1.2: es el código estable de la pantalla y lo que el
 * frontend usa para resolver a qué ruta navegar. Antes esa traducción se hacía con el nombre del
 * módulo, que dejó de servir cuando un módulo pasó a agrupar varias pantallas distintas.</p>
 *
 * <p>{@code name}/{@code icon} (Spec 1.4): nombre/ícono legibles de la pantalla, {@code null} si
 * no se cargaron -- el frontend cae al código de {@code resource}. {@code idScreenGroup}/
 * {@code screenGroupName}: la carpeta plegable de esta pantalla dentro del módulo, {@code null}
 * si no tiene.</p>
 */
public record MenuOperacionView(Long idOperation, String resource, String tag, String name, String icon,
                                Long idScreenGroup, String screenGroupName, String path, String httpMethod) {
}
