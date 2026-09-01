package com.petmate.backend.repository;

import com.petmate.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès aux messages d'une conversation.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Suppression en masse des messages d'une conversation, en une requête SQL.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Message m WHERE m.conversation.id = :conversationId")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}