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

    /**
     * Retourne uniquement le compte actif portant cet email. Comme plusieurs
     * comptes supprimés (soft delete) peuvent partager le même email qu'un
     * compte actif, le login et la réinitialisation de mot de passe doivent
     * cibler le compte actif et ignorer les comptes archivés.
     */
    Optional<User> findByEmailAndActiveTrue(String email);

    /**
     * Compte actif par identifiant. Un compte supprimé (soft delete) ou
     * jamais vérifié ne doit jamais être exposé (404/inauthentifié).
     */
    Optional<User> findByIdAndActiveTrue(Long id);

    /**
     * Vérifie l'existence d'un compte {@code actif} avec cette adresse email.
     * La réinscription est autorisée si seuls des comptes supprimés (soft
     * delete, actifs à false) utilisent déjà cet email.
     */
    boolean existsByEmailAndActiveTrue(String email);

    /**
     * Charge le profil de l'utilisateur avec ses Pets (une seule collection
     * fetch-ée ici). Les Photos sont chargées séparément par le service via
     * {@code PetPhotoRepository#findByPetIds} : Hibernate interdit de fetch-er
     * deux collections List en même temps (MultipleBagFetchException).
     * Distinct car le join sur la collection duplique les lignes.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.pets p
            WHERE u.id = :id
            """)
    Optional<User> findProfileById(@Param("id") Long id);
}