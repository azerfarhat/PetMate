package com.petmate.backend.exception;

/**
 * Détachement d'upload rejeté (type MIME interdit, taille hors limite,
 * cible d'upload interdite, etc.).
 */
public class UploadException extends RuntimeException {

    public UploadException(String message) {
        super(message);
    }
}