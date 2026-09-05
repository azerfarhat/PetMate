package com.petmate.backend.messaging.dto;

/**
 * Signal "en train d'écrire" reçu par WebSocket (destination {@code /app/typing}).
 * La conversation est portée par le message (le chemin REST porte déjà l'id).
 *
 * @param conversationId conversation concernée
 * @param typing vrai pendant la saisie, faux quand elle est stoppée
 */
public record TypingSignal(
        Long conversationId,
        boolean typing) {
}