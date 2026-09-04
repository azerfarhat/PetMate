package com.petmate.backend.exception;

/**
 * Signalement invalide (se signaler soi-même, doublon en cours de traitement,
 * raison manquante, etc.).
 */
public class ReportException extends RuntimeException {

    public ReportException(String message) {
        super(message);
    }
}