package com.petmate.backend.notification;

import com.petmate.backend.notification.dto.NotificationsResponse;
import com.petmate.backend.security.AuthUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST des notifications de l'utilisateur connecté (protégée par JWT).
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Notifications de l'utilisateur, les plus récentes d'abord, avec le nombre
     * de non-lues pour le badge.
     */
    @GetMapping
    public ResponseEntity<NotificationsResponse> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(notificationService.listMine(userId(authentication), limit, offset));
    }

    /**
     * Marque toutes les notifications comme lues. Idempotent.
     */
    @PostMapping("/read")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(userId(authentication));
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque une notification comme lue. Uniquement la sienne.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Authentication authentication,
                                         @PathVariable("id") Long id) {
        notificationService.markRead(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}