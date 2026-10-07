package com.atlas.supplychain.auth.domain.common.exceptions;

public class ExistingDependenciesException extends RuntimeException {
    public ExistingDependenciesException(String message) {
        super(message);
    }
}
