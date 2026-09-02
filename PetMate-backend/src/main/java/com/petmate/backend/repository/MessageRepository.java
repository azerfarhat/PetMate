package com.petmate.backend.repository;

import com.petmate.backend.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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

    /**
     * Historique d'une conversation, paginé et trié par date d'envoi. L'expéditeur
     * est chargé (fetch join) pour éviter le N+1 sur une page de messages.
     */
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender s
            WHERE m.conversation.id = :conversationId
            ORDER BY m.sentAt ASC
            """)
    List<Message> findPageByConversationIdWithSender(@Param("conversationId") Long conversationId,
                                                     Pageable pageable);

    /**
     * Tous les messages d'un jeu de conversations (une page), en une seule
     * requête : sert à dériver le dernier message et le compteur de non-lus sans
     * requête par conversation.
     */
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender s
            WHERE m.conversation.id IN :conversationIds
            ORDER BY m.sentAt ASC
            """)
    List<Message> findByConversationIds(@Param("conversationIds") Collection<Long> conversationIds);

    /**
     * Messages non lus envoyés par l'autre participant (jamais les miens). Utilisé
     * par le marquage "lu" : on ne touche que ce qui vient de l'interlocuteur.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :conversationId AND m.read = false AND m.sender.id <> :myUserId
            """)
    List<Message> findUnreadFromOther(@Param("conversationId") Long conversationId,
                                      @Param("myUserId") Long myUserId);
}