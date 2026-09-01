package com.petmate.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Confirmation du changement de mot de passe : code à 6 chiffres reçu par
 * email (usage unique) + nouveau mot de passe.
 */
public record PasswordChangeRequest(

        @NotBlank
        @Size(min = 6, max = 6)
        String code,

        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Le mot de passe doit contenir au moins une lettre, un chiffre et un caractère spécial")
        String newPassword) {
}