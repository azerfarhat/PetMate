package com.petmate.backend.exception;

/**
 * L'envoi de l'email transactionnel a échoué.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }
}