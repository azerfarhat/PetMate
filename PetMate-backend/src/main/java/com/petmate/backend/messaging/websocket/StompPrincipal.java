package com.petmate.backend.messaging.websocket;

import java.security.Principal;

/**
 * {@link Principal} porté par une session STOMP authentifiée via le JWT. Le
 * {@code name} est l'identifiant de l'utilisateur : c'est la clé utilisée par
 * {@code SimpMessagingTemplate.convertAndSendToUser} pour router les télegrams
 * vers les sessions de l'utilisateur.
 */
public record StompPrincipal(Long userId) implements Principal {

    @Override
    public String getName() {
        return userId == null ? "anonymous" : String.valueOf(userId);
    }
}