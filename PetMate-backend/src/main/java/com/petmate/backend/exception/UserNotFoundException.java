package com.petmate.backend.exception;

/**
 * Utilisateur introuvable en base.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}