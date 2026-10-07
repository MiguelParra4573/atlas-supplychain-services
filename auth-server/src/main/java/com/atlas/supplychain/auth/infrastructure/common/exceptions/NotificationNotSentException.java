package com.atlas.supplychain.auth.infrastructure.common.exceptions;

public class NotificationNotSentException extends RuntimeException {
    public NotificationNotSentException(String message) {
        super(message);
    }

    public NotificationNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
