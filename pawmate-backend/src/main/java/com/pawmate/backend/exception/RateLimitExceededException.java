package com.pawmate.backend.exception;

/**
 * Levée quand un client dépasse la limite autorisée (ex. trop de comptes
 * créés depuis la même adresse IP en peu de temps).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
