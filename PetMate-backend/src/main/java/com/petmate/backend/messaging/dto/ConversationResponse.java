package com.petmate.backend.messaging.dto;

import java.time.LocalDateTime;

/**
 * Une conversation du point de vue de l'utilisateur connecté : l'interlocuteur
 * et son Pet (ceux du match), le dernier message et le nombre de non-lus.
 */
public record ConversationResponse(
        Long conversationId,
        Long matchId,
        Long otherUserId,
        String otherFirstName,
        String otherProfilePicture,
        Long otherPetId,
        String otherPetName,
        String otherPetPrimaryPhotoUrl,
        MessagePreview lastMessage,
        long unreadCount) {

    public record MessagePreview(Long id, String content, Long senderId,
                                 LocalDateTime sentAt, boolean read) {
    }
}