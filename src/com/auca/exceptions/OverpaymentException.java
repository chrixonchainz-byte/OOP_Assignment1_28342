package com.auca.exceptions;

/** Specialised ValidationException: Overpayment. */
public class OverpaymentException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public OverpaymentException(String message) {
        super(message);
    }
}
