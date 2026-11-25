package com.auca.exceptions;

/** Specialised ValidationException: PrerequisiteMissing. */
public class PrerequisiteMissingException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public PrerequisiteMissingException(String message) {
        super(message);
    }
}
