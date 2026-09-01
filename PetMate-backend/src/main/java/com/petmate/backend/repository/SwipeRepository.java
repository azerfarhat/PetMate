package com.petmate.backend.repository;

import com.petmate.backend.entity.Swipe;
import com.petmate.backend.enums.SwipeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux Swipe (LIKE / PASS). Un swipe n'épuise jamais définitivement une
 * Pet : un re-swipe met à jour le type (process reparti de zéro), et un LIKE
 * reste exploitable pour les matchs suivants tant qu'il n'est pas re-swipé PASS.
 */
public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    Optional<Swipe> findByUserIdAndPetId(Long userId, Long petId);

    /**
     * LIKE posés par {@code targetOwnerId} sur les Pet de {@code myUserId},
     * les plus anciens d'abord (permet de former un match par paire de Pet).
     */
    @Query("""
            SELECT s FROM Swipe s
            WHERE s.user.id = :targetOwnerId
              AND s.pet.owner.id = :myUserId
              AND s.type = :type
            ORDER BY s.id ASC
            """)
    List<Swipe> findLikesOnPetsOfUser(@Param("targetOwnerId") Long targetOwnerId,
                                      @Param("myUserId") Long myUserId,
                                      @Param("type") SwipeType type);
}