package com.petmate.backend.messaging.dto;

/**
 * Signal "en train d'écrire" envoyé par REST ou par WebSocket.
 *
 * @param typing vrai quand l'utilisateur est en train de saisir, faux quand il
 *               a interrompu la saisie
 */
public record TypingRequest(boolean typing) {
}