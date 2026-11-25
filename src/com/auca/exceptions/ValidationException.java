package com.auca.exceptions;

/** Base checked exception for every input or business-rule failure in TAMS. */
public class ValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
