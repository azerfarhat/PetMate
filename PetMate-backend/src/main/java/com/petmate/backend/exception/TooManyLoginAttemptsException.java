package com.petmate.backend.exception;

/**
 * Trop de tentatives de connexion échouées : le compte est temporairement bloqué.
 */
public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}
