package com.atlas.supplychain.auth.domain.common.exceptions;

import org.jspecify.annotations.Nullable;

public class RemoteExecutionException extends Exception {

    // null cuando no hubo respuesta HTTP que traducir (ej. timeout, conexión rechazada): los
    // dos constructores de abajo que no reciben código lo dejan así a propósito.
    private final @Nullable Integer statusCode;

    public RemoteExecutionException(@Nullable Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public RemoteExecutionException(@Nullable Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public RemoteExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public RemoteExecutionException(String message) {
        super(message);
        this.statusCode = null;
    }

    public @Nullable Integer getStatusCode() {
        return statusCode;
    }
}
