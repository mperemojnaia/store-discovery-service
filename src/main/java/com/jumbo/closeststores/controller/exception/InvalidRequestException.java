package com.jumbo.closeststores.controller.exception;

/**
 * Thrown when a client request contains invalid parameters.
 * Mapped to HTTP 400 by {@link com.jumbo.closeststores.controller.GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
