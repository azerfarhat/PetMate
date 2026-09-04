package com.petmate.backend.exception;

/**
 * Action de blocage invalide (se bloquer soi-même, cible inexistante, etc.).
 */
public class BlockException extends RuntimeException {

    public BlockException(String message) {
        super(message);
    }
}