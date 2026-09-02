package com.petmate.backend.exception;

/**
 * Une notification demandée est introuvable ou ne concerne pas l'utilisateur.
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }
}