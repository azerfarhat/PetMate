package com.petmate.backend.exception;

/**
 * Un Pet demandé (mise à jour / suppression) est introuvable ou n'appartient
 * pas à l'utilisateur connecté.
 */
public class PetNotFoundException extends RuntimeException {

    public PetNotFoundException(String message) {
        super(message);
    }
}