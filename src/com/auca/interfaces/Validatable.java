package com.auca.interfaces;

import com.auca.exceptions.ValidationException;

/** Anything that can re-check its own rules and describe itself. */
public interface Validatable {
    void validate() throws ValidationException;

    void printDetails();

    String getSummary();
}
