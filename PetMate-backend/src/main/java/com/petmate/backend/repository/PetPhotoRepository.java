package com.petmate.backend.repository;

import com.petmate.backend.entity.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {

    /**
     * Photos de plusieurs Pet en une seule requête (évite le N+1 sur le feed).
     * Le Pet est chargé (fetch join) : l'indexation côté service ne déclenche
     * aucun chargement paresseux.
     */
    @Query("SELECT pp FROM PetPhoto pp JOIN FETCH pp.pet WHERE pp.pet.id IN :petIds")
    List<PetPhoto> findByPetIds(@Param("petIds") Collection<Long> petIds);

    /**
     * Suppression en masse des photos des Pet supprimés, en une requête SQL.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PetPhoto pp WHERE pp.pet.id IN :petIds")
    int deleteByPetIds(@Param("petIds") Collection<Long> petIds);

    /**
     * Suppression en masse des photos retirées d'un Pet conservé.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PetPhoto pp WHERE pp.pet.id = :petId AND pp.id IN :photoIds")
    int deleteByPetIdAndPhotoIds(@Param("petId") Long petId, @Param("photoIds") Collection<Long> photoIds);
}