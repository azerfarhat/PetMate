package com.petmate.backend.repository;

import com.petmate.backend.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * Charge un Pet avec toutes ses photos en une seule requête (fetch join),
     * évitant le problème N+1.
     */
    @Query("SELECT DISTINCT p FROM Pet p LEFT JOIN FETCH p.photos WHERE p.id = :id")
    Optional<Pet> findByIdWithPhotos(@Param("id") Long id);

    /**
     * Tous les Pet actifs d'un utilisateur avec leurs photos, en une seule
     * requête (fetch join). Un Pet supprimé (inactif) n'apparaît plus ici.
     */
    @Query("""
            SELECT DISTINCT p FROM Pet p
            LEFT JOIN FETCH p.photos
            WHERE p.owner.id = :ownerId AND p.active = true
            ORDER BY p.id
            """)
    List<Pet> findAllActiveByOwnerIdWithPhotos(@Param("ownerId") Long ownerId);

    /**
     * Pet actif possédé par {@code ownerId}, charges ses photos en une seule
     * requête. L'appartenance et l'activité sont filtrées en SQL (une seule
     * requête, aucune vérification en mémoire côté service).
     */
    @Query("""
            SELECT DISTINCT p FROM Pet p
            LEFT JOIN FETCH p.photos
            WHERE p.id = :petId AND p.owner.id = :ownerId AND p.active = true
            """)
    Optional<Pet> findOwnedActiveById(@Param("ownerId") Long ownerId, @Param("petId") Long petId);

    /**
     * Nombre de Pet actifs d'un utilisateur, hors un Pet donné. Utilisé pour
     * garantir qu'il reste toujours au moins un Pet actif (invariant de swipe).
     */
    @Query("SELECT COUNT(p) FROM Pet p WHERE p.owner.id = :ownerId AND p.active = true AND p.id <> :petId")
    long countActiveByOwnerIdAndIdNot(@Param("ownerId") Long ownerId, @Param("petId") Long petId);

    boolean existsByOwnerIdAndActiveTrue(@Param("ownerId") Long ownerId);

    /**
     * Feed de découverte : chaque Pet active d'un owner actif (non bloqué, hors
     * les miennes) est un candidat individuel. Aucune exclusion liée aux swipes
     * ou aux matchs (une Pet réapparaît toujours), sauf pendant le cooldown de
     * re-match : une Pet dont le match avec l'utilisateur vient d'être supprimé
     * (UNMATCHED) est masquée jusqu'à la fin du délai.
     * L'owner est joint (ManyToOne : ne multiplie pas les lignes, pagination sûre).
     */
    @Query("""
            SELECT p FROM Pet p
            JOIN p.owner u
            WHERE p.active = true AND u.active = true AND u.id <> :userId
              AND NOT EXISTS (SELECT b FROM Block b
                              WHERE (b.blocker.id = :userId AND b.blockedUser.id = u.id)
                                 OR (b.blocker.id = u.id AND b.blockedUser.id = :userId))
              AND NOT EXISTS (SELECT m FROM Match m
                              WHERE m.status = com.petmate.backend.enums.MatchStatus.UNMATCHED
                                AND m.unmatchedAt >= :cooldownCutoff
                                AND ((m.pet1.id = p.id AND m.user2.id = :userId)
                                  OR (m.pet2.id = p.id AND m.user1.id = :userId)))
            ORDER BY p.id DESC
            """)
    Page<Pet> findCandidates(@Param("userId") Long userId,
                             @Param("cooldownCutoff") java.time.LocalDateTime cooldownCutoff,
                             Pageable pageable);

    /**
     * Suppression en masse des Pet, en une seule requête SQL.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Pet p WHERE p.id IN :ids")
    int deleteByIds(@Param("ids") Collection<Long> ids);
}