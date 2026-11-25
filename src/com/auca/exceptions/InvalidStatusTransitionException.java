package com.auca.exceptions;

/** Specialised ValidationException: InvalidStatusTransition. */
public class InvalidStatusTransitionException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
