package com.katanapay.routing.exception;

/**
 * Exception thrown when there is an issue with a payment provider.
 */
public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}