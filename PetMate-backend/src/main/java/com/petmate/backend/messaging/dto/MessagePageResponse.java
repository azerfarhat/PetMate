package com.petmate.backend.messaging.dto;

import java.util.List;

/**
 * Page d'historique d'une conversation, par curseur (keyset sur l'identifiant).
 *
 * <p>La sélection se fait sur les messages les plus récents (ou sur ceux
 * strictement antérieurs à {@code beforeId}) ; les messages retournés sont
 * ordonnés chronologiquement à l'intérieur de la page. {@code nextCursor}
 * désigne l'identifiant du message le plus ancien de la page : il sert de
 * {@code beforeId} pour charger la page précédente. {@code hasMore} indique
 * si des messages plus anciens existent.
 *
 * @param messages messages de la page, du plus ancien au plus récent
 * @param hasMore  vrai si des messages plus anciens restent à charger
 * @param nextCursor identifiant du message le plus ancien (curseur), ou null
 *                   si la page est vide
 */
public record MessagePageResponse(
        List<MessageResponse> messages,
        boolean hasMore,
        Long nextCursor) {
}