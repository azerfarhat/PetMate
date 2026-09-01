package com.petmate.backend.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mise à jour du profil d'un utilisateur connecté : champs personnels + liste
 * complète de ses Pet. La liste est remplacée atomiquement (ajout, modification
 * ou suppression de Pet) et doit toujours contenir au moins un Pet.
 */
public record UpdateProfileRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Size(max = 2048)
        String profilePicture,

        @Size(max = 1000)
        String bio,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @Positive
        Integer searchRadius,

        @NotEmpty
        @Valid
        List<PetUpdateRequest> pets) {

    public UpdateProfileRequest {
        pets = pets == null ? List.of() : List.copyOf(pets);
    }
}