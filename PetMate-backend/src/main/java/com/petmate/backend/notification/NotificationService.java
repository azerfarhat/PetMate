package com.petmate.backend.notification;

import com.petmate.backend.entity.Notification;
import com.petmate.backend.exception.NotificationNotFoundException;
import com.petmate.backend.notification.dto.NotificationResponse;
import com.petmate.backend.notification.dto.NotificationsResponse;
import com.petmate.backend.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notifications de l'utilisateur connecté (MATCH, MESSAGE, SYSTEM). L'identifiant
 * est toujours tiré du principal authentifié : des notifications d'un autre
 * utilisateur sont inaccessibles (404 ambigu).
 */
@Service
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse listMine(Long userId, int limit, int offset) {
        List<NotificationResponse> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(pageOf(offset, limit), sizeOf(limit)))
                .stream()
                .map(this::toResponse)
                .toList();
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        return new NotificationsResponse(notifications, unreadCount);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification introuvable"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private static int sizeOf(int limit) {
        return Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
    }

    private static int pageOf(int offset, int limit) {
        return Math.max(0, offset) / Math.max(1, limit);
    }
}