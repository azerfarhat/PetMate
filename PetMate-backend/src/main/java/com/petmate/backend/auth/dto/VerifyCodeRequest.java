package com.petmate.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Vérification d'adresse email par code à 6 chiffres reçu par email.
 */
public record VerifyCodeRequest(

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Le code doit contenir exactement 6 chiffres")
        String code) {
}