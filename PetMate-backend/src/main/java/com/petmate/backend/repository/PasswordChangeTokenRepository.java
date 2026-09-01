package com.petmate.backend.repository;

import com.petmate.backend.entity.PasswordChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordChangeTokenRepository extends JpaRepository<PasswordChangeToken, Long> {

    Optional<PasswordChangeToken> findByCode(String code);

    /**
     * Invalide tous les codes encore actifs d'un utilisateur, garantissant au
     * plus un code valide à la fois.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordChangeToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    int invalidateActiveTokensForUser(@Param("userId") Long userId);
}