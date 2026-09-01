package com.petmate.backend.exception;

/**
 * Jeton de réinitialisation de mot de passe invalide, expiré ou déjà utilisé.
 */
public class PasswordResetTokenException extends RuntimeException {

    public PasswordResetTokenException(String message) {
        super(message);
    }
}
