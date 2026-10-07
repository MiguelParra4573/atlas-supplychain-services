package com.atlas.supplychain.auth.domain.common.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Formato único de autoridad RBAC de Atlas: {@code RECURSO:OPERACION}. Sin prefijo alguno
 * (ni el histórico ni ningún otro); el separador ':' basta. Se llama en cada request
 * autenticado (User.permisosEfectivos()), de ahí los Pattern precompilados en vez de
 * recompilar la regex por permiso.
 *
 * <p>Ambas mitades salen de la propia {@link Operation} (Spec 1.2). Hasta esa spec la primera
 * mitad salía de {@code Module.name}, lo que ataba la seguridad a una etiqueta que cada empresa
 * puede editar: renombrar un módulo cambiaba sus permisos en silencio. {@code resource} es el
 * código del microservicio que sirve la pantalla y no se edita desde administración, así que la
 * cadena resultante es la misma de siempre ({@code USUARIOS:CREAR}) pero ya no depende del
 * menú.</p>
 */
public final class AuthorityName {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALLOWED = Pattern.compile("[^A-Z0-9]+");

    private AuthorityName() {
    }

    public static String of(Operation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation es obligatoria para construir una autoridad");
        }

        String resourcePart = normalize(operation.getResource());
        String operationPart = normalize(operation.getTag());

        if (resourcePart.isEmpty() || operationPart.isEmpty()) {
            throw new IllegalArgumentException(
                    "operation.resource y operation.tag no pueden normalizar a cadena vacía (resource=%s, tag=%s)"
                            .formatted(operation.getResource(), operation.getTag()));
        }

        return resourcePart + ":" + operationPart;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(upper, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALLOWED.matcher(withoutDiacritics).replaceAll("_");
    }
}
