package com.auca.exceptions;

/** Specialised ValidationException: InvalidTaxPeriod. */
public class InvalidTaxPeriodException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public InvalidTaxPeriodException(String message) {
        super(message);
    }
}
