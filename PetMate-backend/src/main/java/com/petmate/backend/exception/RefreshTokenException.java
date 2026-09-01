package com.petmate.backend.exception;

/**
 * Erreur métier liée au Refresh Token (invalide, expiré, révoqué).
 */
public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException(String message) {
        super(message);
    }
}