package com.petmate.backend.repository;

import com.petmate.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès aux conversations, créées automatiquement lors d'un match.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Conversation c WHERE c.id = :id")
    int deleteByIdOptimistic(@Param("id") Long id);
}