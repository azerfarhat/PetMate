package com.petmate.backend.messaging;

import com.petmate.backend.messaging.dto.MessageResponse;
import com.petmate.backend.notification.dto.NotificationResponse;

/**
 * Événement émis après un envoi de message (dans la transaction). Le listener
 * réel est déclenché APRES le commit : un message jamais engagé ne part jamais
 * en temps réel, garantissant la cohérence REST/WebSocket.
 *
 * @param recipientUserId destinataire du message
 * @param notification    notification MESSAGE concurrente (peut être null)
 */
public record MessagePublishedEvent(MessageResponse message,
                                    Long recipientUserId,
                                    NotificationResponse notification) {
}