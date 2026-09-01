package com.petmate.backend.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Données d'inscription d'un Owner : informations du User + mot de passe +
 * au moins un Pet. Aucune entité JPA n'est exposée.
 */
public record RegisterRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Le mot de passe doit contenir au moins une lettre, un chiffre et un caractère spécial")
        String password,

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
        List<PetRequest> pets) {

    public RegisterRequest {
        pets = pets == null ? List.of() : List.copyOf(pets);
    }
}