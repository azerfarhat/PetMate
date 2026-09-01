package com.petmate.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Demande d'envoi d'un email de réinitialisation de mot de passe.
 */
public record ForgotPasswordRequest(

        @NotBlank
        @Email
        String email) {
}
