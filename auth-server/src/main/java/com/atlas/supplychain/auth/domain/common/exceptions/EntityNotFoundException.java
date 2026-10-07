package com.atlas.supplychain.auth.domain.common.exceptions;

import java.io.Serial;

public class EntityNotFoundException extends Exception {

  @Serial
  private static final long serialVersionUID = 1L;

  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public EntityNotFoundException(Throwable cause) {
    super(cause);
  }

}