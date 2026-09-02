package com.petmate.backend.notification;

import com.petmate.backend.entity.Notification;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.NotificationType;
import com.petmate.backend.exception.NotificationNotFoundException;
import com.petmate.backend.notification.dto.NotificationsResponse;
import com.petmate.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les notifications sont strictement personnelles : la liste et le marquage ne
 * concernent que l'utilisateur authentifié, une notification d'un autre est
 * inaccessible (404).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long ME = 1L;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void listMine_returnsMineWithUnreadCount() {
        Notification unread = notification(1L, false);
        Notification read = notification(2L, true);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(ME), any(Pageable.class)))
                .thenReturn(List.of(unread, read));
        when(notificationRepository.countByUserIdAndReadFalse(ME)).thenReturn(1L);

        NotificationsResponse response = notificationService.listMine(ME, 20, 0);

        assertEquals(2, response.notifications().size());
        assertEquals(1L, response.unreadCount());
        assertEquals(1L, response.notifications().get(0).id());
        assertEquals(NotificationType.MESSAGE, response.notifications().get(0).type());
        assertEquals(false, response.notifications().get(0).read());
    }

    @Test
    void listMine_withoutNotification_returnsEmpty() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(ME), any(Pageable.class)))
                .thenReturn(List.of());
        when(notificationRepository.countByUserIdAndReadFalse(ME)).thenReturn(0L);

        NotificationsResponse response = notificationService.listMine(ME, 20, 0);

        assertTrue(response.notifications().isEmpty());
        assertEquals(0L, response.unreadCount());
    }

    @Test
    void markAllRead_updatesAllMineInOneQuery() {
        notificationService.markAllRead(ME);

        verify(notificationRepository).markAllReadForUser(ME);
    }

    @Test
    void markRead_marksNotificationAndSaves() {
        Notification notification = notification(1L, false);
        when(notificationRepository.findByIdAndUserId(1L, ME)).thenReturn(Optional.of(notification));

        notificationService.markRead(ME, 1L);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_isIdempotentWhenAlreadyRead() {
        Notification notification = notification(1L, true);
        when(notificationRepository.findByIdAndUserId(1L, ME)).thenReturn(Optional.of(notification));

        notificationService.markRead(ME, 1L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_unknownOrForeignNotification_throwsNotFound() {
        when(notificationRepository.findByIdAndUserId(99L, ME)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.markRead(ME, 99L));
    }

    private Notification notification(Long id, boolean read) {
        return Notification.builder()
                .id(id)
                .title("Titre")
                .content("Contenu")
                .type(NotificationType.MESSAGE)
                .read(read)
                .user(User.builder().id(ME).build())
                .build();
    }
}