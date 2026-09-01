package com.petmate.backend.repository;

import com.petmate.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository de la persistance des {@link User}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Charge un profil complet (User + Pets + Photos) en une seule requête
     * (fetch joins), évitant le problème N+1. Distinct car les joins sur les
     * collections dupliquent les lignes.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.pets p
            LEFT JOIN FETCH p.photos
            WHERE u.id = :id
            """)
    Optional<User> findProfileById(@Param("id") Long id);
}