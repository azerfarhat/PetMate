package com.petmate.backend.exception;

/**
 * Erreur lors de la création d'un compte Owner (règle métier).
 */
public class RegistrationException extends RuntimeException {

    public RegistrationException(String message) {
        super(message);
    }
}