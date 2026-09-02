package com.petmate.backend.messaging.websocket;

import com.petmate.backend.enums.NotificationType;
import com.petmate.backend.messaging.MessagePublishedEvent;
import com.petmate.backend.messaging.dto.MessageResponse;
import com.petmate.backend.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Diffusion après-commit : le message part dans la file de la conversation des
 * deux participants, et la notification dans la file personnelle du
 * destinataire. Tout passe par des files par utilisateur, jamais par un topic
 * partagé.
 */
@ExtendWith(MockitoExtension.class)
class MessageEventPublisherTest {

    private static final String CONVERSATION_QUEUE = "/queue/conversations.100";
    private static final String NOTIFICATION_QUEUE = "/queue/notifications";

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MessageEventPublisher publisher;

    private final MessageResponse message = new MessageResponse(
            300L, 100L, "Bonjour", 1L, LocalDateTime.now(), false);

    private final NotificationResponse notification = new NotificationResponse(
            7L, "Nouveau message", "Bonjour", NotificationType.MESSAGE, false, LocalDateTime.now());

    @BeforeEach
    void setUp() {
        publisher = new MessageEventPublisher(messagingTemplate);
    }

    @Test
    void publishesToSenderAndRecipientConversationQueueAndNotification() {
        publisher.onMessagePublished(new MessagePublishedEvent(message, 2L, notification));

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq(CONVERSATION_QUEUE), eq(message));
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq(CONVERSATION_QUEUE), eq(message));
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq(NOTIFICATION_QUEUE), eq(notification));
    }

    @Test
    void withoutNotification_pushesOnlyTheMessage() {
        publisher.onMessagePublished(new MessagePublishedEvent(message, 2L, null));

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), eq(NOTIFICATION_QUEUE), any());
        verify(messagingTemplate, never()).convertAndSendToUser(eq("1"), eq(NOTIFICATION_QUEUE), any());
    }

    @Test
    void withoutRecipient_pushesOnlySenderEcho() {
        publisher.onMessagePublished(new MessagePublishedEvent(message, null, null));

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq(CONVERSATION_QUEUE), eq(message));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("2"), anyString(), any());
    }
}