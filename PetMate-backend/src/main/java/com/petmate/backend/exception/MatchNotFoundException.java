package com.petmate.backend.exception;

/**
 * Un Match demandé est introuvable ou ne concerne pas l'utilisateur.
 */
public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(String message) {
        super(message);
    }
}