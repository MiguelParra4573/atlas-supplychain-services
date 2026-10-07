package com.atlas.supplychain.auth.infrastructure.config.security.tenant;

import org.springframework.security.access.AccessDeniedException;

/**
 * Marca el 403 de TenantBoundaryFilter para que restAccessDeniedHandler use su mensaje
 * especifico en vez del generico -- sin afectar el mensaje de otros AccessDeniedException
 * (ej. @PreAuthorize).
 */
public class TenantMismatchException extends AccessDeniedException {

    public TenantMismatchException(String message) {
        super(message);
    }
}
