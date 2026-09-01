package com.petmate.backend.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Profil complet d'un utilisateur (GET /users/me).
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String profilePicture,
        String bio,
        Double latitude,
        Double longitude,
        Integer searchRadius,
        boolean emailVerified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PetResponse> pets) {
}