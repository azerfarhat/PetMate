package com.petmate.backend.exception;

/**
 * Erreur liée au changement de mot de passe par code de vérification email.
 */
public class PasswordChangeException extends RuntimeException {

    public PasswordChangeException(String message) {
        super(message);
    }
}