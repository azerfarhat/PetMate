package com.petmate.backend.exception;

/**
 * Action refusée car le compte n'a pas encore vérifié son adresse email.
 */
public class VerificationRequiredException extends RuntimeException {

    public VerificationRequiredException(String message) {
        super(message);
    }
}
