package com.pawmate.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Réinitialisation du mot de passe à l'aide du jeton reçu par email.
 */
public record ResetPasswordRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Le mot de passe doit contenir au moins une lettre, un chiffre et un caractère spécial")
        String newPassword) {
}
