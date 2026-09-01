package com.pawmate.backend.exception;

/**
 * Jeton de vérification email invalide, expiré ou déjà utilisé.
 */
public class VerificationTokenException extends RuntimeException {

    public VerificationTokenException(String message) {
        super(message);
    }
}