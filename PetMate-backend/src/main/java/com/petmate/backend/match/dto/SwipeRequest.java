package com.petmate.backend.match.dto;

import com.petmate.backend.enums.SwipeType;
import jakarta.validation.constraints.NotNull;

/**
 * Action de swipe : type (LIKE / PASS) sur la Pet cible d'un autre owner.
 * La Pet représentative de l'utilisateur est déterminée côté serveur (une seule
 * Pet par owner dans le matching).
 */
public record SwipeRequest(
        @NotNull
        SwipeType type,

        @NotNull
        Long targetPetId) {
}