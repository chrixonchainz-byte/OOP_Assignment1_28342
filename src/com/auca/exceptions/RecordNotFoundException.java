package com.auca.exceptions;

/** Specialised ValidationException: RecordNotFound. */
public class RecordNotFoundException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String message) {
        super(message);
    }
}
