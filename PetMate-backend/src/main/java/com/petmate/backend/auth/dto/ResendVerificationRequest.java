package com.petmate.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Demande de renvoi d'un email de vérification pour un compte non vérifié.
 */
public record ResendVerificationRequest(

        @NotBlank
        @Email
        String email) {
}