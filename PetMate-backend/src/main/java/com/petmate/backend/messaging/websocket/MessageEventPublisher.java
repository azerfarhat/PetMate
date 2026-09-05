package com.petmate.backend.messaging.websocket;

import com.petmate.backend.messaging.MessagePublishedEvent;
import com.petmate.backend.messaging.dto.TypingStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Diffusion temps réel des événements de messagerie. Les télégrammes vont dans
 * les files personnelles des participants ({@code /user/queue/...}), jamais sur
 * un topic partagé : une conversation n'est accessible qu'aux sessions de ses
 * participants. Les messages persistés sont diffusés après commit de la
 * transaction ; le statut "en train d'écrire", transitoire, est diffusé
 * immédiatement.
 */
@Component
public class MessageEventPublisher {

    private static final String CONVERSATION_QUEUE = "/queue/conversations.%d";
    private static final String CONVERSATION_TYPING_QUEUE = "/queue/conversations.%d.typing";
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

    /**
     * Diffuse un signal "en train d'écrire" à l'interlocuteur, sur sa file
     * personnelle de la conversation. Transitoire : aucune persistance.
     */
    public void publishTyping(Long conversationId, Long typingUserId, Long recipientUserId, boolean typing) {
        messagingTemplate.convertAndSendToUser(
                userKey(recipientUserId),
                CONVERSATION_TYPING_QUEUE.formatted(conversationId),
                new TypingStatus(conversationId, typingUserId, typing));
    }

    private String userKey(Long userId) {
        return String.valueOf(userId);
    }
}