package com.auca.exceptions;

/** Specialised ValidationException: UnauthorizedApproval. */
public class UnauthorizedApprovalException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedApprovalException(String message) {
        super(message);
    }
}
