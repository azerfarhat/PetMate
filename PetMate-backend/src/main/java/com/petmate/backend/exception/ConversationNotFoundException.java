package com.petmate.backend.exception;

/**
 * Une conversation demandée est introuvable, ne concerne pas l'utilisateur ou
 * n'est plus accessible (match supprimé, blocage). Message volontairement
 * générique pour ne pas fuiter l'existence d'une ressource.
 */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(String message) {
        super(message);
    }
}