package com.petmate.backend.user.dto;

import java.time.LocalDateTime;

/**
 * Profil public d'un membre (GET /users/{id}).
 *
 * <p>Ne contient que les informations destinées aux autres membres : aucune
 * donnée privée (email, localisation, recherche, statut de vérification…)
 * n'est exposée.</p>
 */
public record PublicUserResponse(
        Long id,
        String firstName,
        String lastName,
        String bio,
        String profilePicture,
        LocalDateTime createdAt) {
}