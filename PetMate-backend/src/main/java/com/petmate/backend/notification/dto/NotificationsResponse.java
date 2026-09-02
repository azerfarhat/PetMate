package com.petmate.backend.notification.dto;

import java.util.List;

/**
 * Liste paginée des notifications de l'utilisateur connecté, avec le nombre de
 * non-lues (pour le badge).
 */
public record NotificationsResponse(List<NotificationResponse> notifications, long unreadCount) {
}