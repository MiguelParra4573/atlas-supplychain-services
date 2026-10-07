package com.atlas.supplychain.auth.domain.common.exceptions;

public enum BusinessRuleCode {

    PASSWORD_ACTUAL_INCORRECTA(400, "La contraseña actual es incorrecta."),
    PASSWORD_REUTILIZADA(409, "No se permite reutilizar una contraseña reciente."),
    TOKEN_RESET_INVALIDO(400, "El enlace de restablecimiento no es válido o ha expirado."),
    TOKEN_RESET_EXPIRADO(400, "El enlace de restablecimiento no es válido o ha expirado."),
    REFRESH_TOKEN_INVALIDO(401, "La sesión expiró. Inicie sesión nuevamente."),
    PASSWORD_NO_CUMPLE_POLITICA(400, "La contraseña no cumple la política de authorization.");

    private final int httpStatus;
    private final String publicMessage;

    BusinessRuleCode(int httpStatus, String publicMessage) {
        this.httpStatus = httpStatus;
        this.publicMessage = publicMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getPublicMessage() {
        return publicMessage;
    }

    /**
     * TOKEN_RESET_EXPIRADO colapsa a TOKEN_RESET_INVALIDO ante el cliente: distinguirlos
     * permitiría enumerar tokens válidos. El valor del enum queda separado solo para el log.
     */
    public String getPublicCode() {
        return this == TOKEN_RESET_EXPIRADO ? TOKEN_RESET_INVALIDO.name() : name();
    }
}
