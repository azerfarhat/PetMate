package com.petmate.backend.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contenu d'un message envoyé dans une conversation.
 */
public record SendMessageRequest(
        @NotBlank(message = "Le message ne peut pas être vide")
        @Size(max = 5000, message = "Le message doit contenir au maximum 5000 caractères")
        String content) {
}