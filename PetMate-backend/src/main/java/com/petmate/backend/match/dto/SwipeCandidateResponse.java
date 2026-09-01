package com.petmate.backend.match.dto;

import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;

/**
 * Carte de découverte : une Pet d'un autre owner (sa photo principale en
 * premier plan) + les informations publiques de l'owner uniquement (jamais ses
 * autres Pets).
 */
public record SwipeCandidateResponse(
        Long petId,
        String petName,
        PetType petType,
        String breed,
        Integer age,
        PetGender gender,
        EnergyLevel energyLevel,
        String primaryPhotoUrl,
        OwnerInfo owner) {

    public record OwnerInfo(Long id, String firstName, String bio, String profilePicture) {
    }
}