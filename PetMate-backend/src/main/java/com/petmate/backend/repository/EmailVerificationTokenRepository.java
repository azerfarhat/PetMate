package com.petmate.backend.repository;

import com.petmate.backend.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByCode(String code);

    /**
     * Invalide tous les jetons de vérification encore actifs d'un utilisateur,
     * garantissant au plus un jeton actif par utilisateur.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    int invalidateActiveTokensForUser(@Param("userId") Long userId);
}