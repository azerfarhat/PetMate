package com.petmate.backend.exception;

/**
 * L'adresse email est déjà utilisée par un compte existant.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}