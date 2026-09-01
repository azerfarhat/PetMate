package com.petmate.backend.match.dto;

import com.petmate.backend.enums.SwipeType;

/**
 * Résultat d'un swipe. {@code matched} vaut {@code true} en cas de LIKE croisé :
 * un Match a été créé, une conversation ouverte et des notifications envoyées.
 */
public record SwipeResponse(
        boolean matched,
        Long matchId,
        Long conversationId,
        SwipeType type) {

    public static SwipeResponse notMatched(SwipeType type) {
        return new SwipeResponse(false, null, null, type);
    }
}