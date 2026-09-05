package com.petmate.backend.messaging.dto;

/**
 * Statut "en train d'écrire" diffusé en temps réel à l'interlocuteur sur sa
 * file personnelle {@code /user/queue/conversations.{id}.typing}.
 *
 * @param conversationId conversation concernée
 * @param userId utilisateur qui tape (l'interlocuteur)
 * @param typing vrai pendant la saisie, faux quand elle est stoppée
 */
public record TypingStatus(
        Long conversationId,
        Long userId,
        boolean typing) {
}