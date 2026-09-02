package com.petmate.backend.messaging.dto;

import java.time.LocalDateTime;

/**
 * Un message. Le client déduit "mien/sien" en comparant {@code senderId} à son
 * propre identifiant, ce qui évite d'exposer les deux côtés du fil de discussion.
 */
public record MessageResponse(
        Long id,
        Long conversationId,
        String content,
        Long senderId,
        LocalDateTime sentAt,
        boolean read) {
}