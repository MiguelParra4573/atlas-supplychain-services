package com.atlas.supplychain.auth.domain.common.exceptions;

public class RemoteExecutionException extends Exception {

    private final Integer statusCode;

    public RemoteExecutionException(Integer statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public RemoteExecutionException(Integer statusCode, String message) {
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
}
