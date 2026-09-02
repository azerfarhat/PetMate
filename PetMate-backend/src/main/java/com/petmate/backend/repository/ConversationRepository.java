package com.petmate.backend.repository;

import com.petmate.backend.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux conversations, créées automatiquement lors d'un match.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Conversation c WHERE c.id = :id")
    int deleteByIdOptimistic(@Param("id") Long id);

    /**
     * Conversations d'un utilisateur avec leur match (participants et Pet)
     * chargés en une requête. Paginée classiquement : aucun join de collection,
     * donc aucune duplication de lignes.
     */
    @Query("""
            SELECT DISTINCT c FROM Conversation c
            JOIN FETCH c.match m
            JOIN FETCH m.user1
            JOIN FETCH m.user2
            JOIN FETCH m.pet1
            JOIN FETCH m.pet2
            WHERE m.user1.id = :userId OR m.user2.id = :userId
            """)
    List<Conversation> findAllForUserWithDetails(@Param("userId") Long userId, Pageable pageable);

    /**
     * Une conversation avec son match et ses deux participants, en une requête.
     * Sert aux opérations d'envoi/lecture : le statut du match et les
     * participants sont connus sans requête supplémentaire.
     */
    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.match m
            JOIN FETCH m.user1
            JOIN FETCH m.user2
            WHERE c.id = :conversationId
            """)
    Optional<Conversation> findByIdWithParticipants(@Param("conversationId") Long conversationId);
}