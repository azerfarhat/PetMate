package com.petmate.backend.repository;

import com.petmate.backend.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Vrai si un blocage existe entre deux owners, dans un sens ou l'autre.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Block b
            WHERE (b.blocker.id = :firstId AND b.blockedUser.id = :secondId)
               OR (b.blocker.id = :secondId AND b.blockedUser.id = :firstId)
            """)
    boolean existBetweenOwners(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

    /**
     * Blocage émis UNIQUEMENT de {@code blockerId} vers {@code blockedUserId}.
     * Servi pour un débocage idempotent (chaque utilisateur lève son propre blocage).
     */
    Optional<Block> findByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);

    /**
     * Lève un blocage directionnel sans le recharger préalablement en mémoire.
     * Idempotent : ne rien supprimer n'est pas une erreur.
     */
    void deleteByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);

    /**
     * Utilisateurs bloqués par {@code blockerId}, les plus récents d'abord.
     * Le user bloqué est fetch-é avec le blocage (une seule collection fetch-ée).
     */
    @Query("""
            SELECT b FROM Block b
            JOIN FETCH b.blockedUser
            WHERE b.blocker.id = :blockerId
            ORDER BY b.createdAt DESC
            """)
    List<Block> findBlockedUsers(@Param("blockerId") Long blockerId);
}