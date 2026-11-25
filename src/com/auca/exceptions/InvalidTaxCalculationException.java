package com.auca.exceptions;

/** Specialised ValidationException: InvalidTaxCalculation. */
public class InvalidTaxCalculationException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public InvalidTaxCalculationException(String message) {
        super(message);
    }
}
