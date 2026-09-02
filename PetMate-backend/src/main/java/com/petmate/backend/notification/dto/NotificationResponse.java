package com.petmate.backend.notification.dto;

import com.petmate.backend.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Une notification de l'utilisateur connecté.
 */
public record NotificationResponse(
        Long id,
        String title,
        String content,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt) {
}