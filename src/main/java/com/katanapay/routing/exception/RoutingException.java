package com.katanapay.routing.exception;

/**
 * Exception thrown when there is an issue with payment routing logic.
 */
public class RoutingException extends RuntimeException {

    public RoutingException(String message) {
        super(message);
    }
}