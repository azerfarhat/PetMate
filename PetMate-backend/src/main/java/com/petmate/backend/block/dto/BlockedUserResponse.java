package com.petmate.backend.block.dto;

import java.time.LocalDateTime;

/**
 * Utilisateur bloqué par le compte authentifié : informations publiques
 * minimales + date du blocage (pour l'écran "gestion des blocages").
 */
public record BlockedUserResponse(
        Long userId,
        String firstName,
        String lastName,
        String profilePicture,
        LocalDateTime blockedAt) {
}