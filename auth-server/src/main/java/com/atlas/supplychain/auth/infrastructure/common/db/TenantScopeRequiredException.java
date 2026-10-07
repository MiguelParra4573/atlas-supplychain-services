package com.atlas.supplychain.auth.infrastructure.common.db;

/**
 * Un idCompany ausente en el punto de paso obligado de {@link AbstractQueryDslRepository}
 * nunca es entrada de usuario: es un bug (endpoint sin autenticación, adaptador que no
 * propagó el principal). No es una regla de negocio (no es {@code BusinessRuleException}),
 * por eso extiende {@link IllegalStateException} y se mapea a 5xx, no a 4xx.
 */
public class TenantScopeRequiredException extends IllegalStateException {

    public TenantScopeRequiredException(String message) {
        super(message);
    }
}
