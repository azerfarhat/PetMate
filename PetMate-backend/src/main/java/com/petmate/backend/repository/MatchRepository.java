package com.petmate.backend.repository;

import com.petmate.backend.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Accès aux Match pet-to-pet. Un match concerne une paire précise de Pet ;
 * la présence d'un match actif entre deux Pet n'empêche pas d'autres Pet des
 * mêmes owners de matcher entre elles.
 */
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Match existant pour une paire de Pet (dans un sens ou l'autre), tout
     * statut confondu, le plus récent d'abord. Sert à ne pas dupliquer un match
     * actif puis à réactiver un match UNMATCHED.
     */
    @Query("""
            SELECT m FROM Match m
            WHERE (m.pet1.id = :pet1Id AND m.pet2.id = :pet2Id)
               OR (m.pet1.id = :pet2Id AND m.pet2.id = :pet1Id)
            ORDER BY m.id DESC
            """)
    List<Match> findByPetPair(@Param("pet1Id") Long pet1Id, @Param("pet2Id") Long pet2Id);

    /**
     * Matchs d'un utilisateur avec leurs Pets (photos) chargés en une requête.
     */
    @Query("""
            SELECT DISTINCT m FROM Match m
            LEFT JOIN FETCH m.pet1 p1
            LEFT JOIN FETCH p1.photos
            LEFT JOIN FETCH m.pet2 p2
            LEFT JOIN FETCH p2.photos
            WHERE m.user1.id = :userId OR m.user2.id = :userId
            ORDER BY m.matchedAt DESC
            """)
    List<Match> findForUser(@Param("userId") Long userId);
}