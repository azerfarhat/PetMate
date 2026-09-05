package com.petmate.backend.user.dto;

import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;

import java.util.List;

/**
 * Pet exposé dans le profil d'un utilisateur. Aucune entité JPA n'est exposée.
 */
public record PetResponse(
        Long id,
        Long ownerId,
        String name,
        PetType type,
        String breed,
        PetGender gender,
        Integer age,
        EnergyLevel energyLevel,
        String description,
        boolean vaccinated,
        boolean neutered,
        List<PetPhotoResponse> photos) {
}