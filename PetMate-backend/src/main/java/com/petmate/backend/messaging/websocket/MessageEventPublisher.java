package com.petmate.backend.messaging.websocket;

import com.petmate.backend.messaging.MessagePublishedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Diffusion temps réel des événements de messagerie, après commit de la
 * transaction. Les télégrammes vont dans les files personnelles des deux
 * participants ({@code /user/queue/...}), jamais sur un topic partagé : une
 * conversation n'est accessible qu'aux sessions de ses participants.
 */
@Component
public class MessageEventPublisher {

    private static final String CONVERSATION_QUEUE = "/queue/conversations.%d";
    private static final String NOTIFICATION_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public MessageEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagePublished(MessagePublishedEvent event) {
        String conversationQueue = CONVERSATION_QUEUE.formatted(event.message().conversationId());

        // Écho vers toutes les sessions de l'expéditeur (multiappareils).
        messagingTemplate.convertAndSendToUser(userKey(event.message().senderId()), conversationQueue, event.message());

        if (event.recipientUserId() != null) {
            messagingTemplate.convertAndSendToUser(userKey(event.recipientUserId()), conversationQueue, event.message());
            if (event.notification() != null) {
                messagingTemplate.convertAndSendToUser(
                        userKey(event.recipientUserId()), NOTIFICATION_QUEUE, event.notification());
            }
        }
    }

    private String userKey(Long userId) {
        return String.valueOf(userId);
    }
}