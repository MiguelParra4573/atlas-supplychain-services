package com.atlas.supplychain.auth.application.security.contracts;

import java.time.Duration;

public interface PasswordPolicy {
    void validateOrThrow(String newPassword);

    /** Cuántas contraseñas recientes se revisan para bloquear reutilización. */
    int historyCount();

    /** Vigencia de una contraseña desde que se establece hasta que expira. */
    Duration passwordExpiration();

    /** Vigencia del token de un solo uso emitido en el flujo de "olvidé mi contraseña". */
    Duration resetTokenTtl();
}
