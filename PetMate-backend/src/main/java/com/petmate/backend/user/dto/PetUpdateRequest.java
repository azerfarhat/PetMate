package com.petmate.backend.user.dto;

import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Données d'un Pet dans une mise àjour de profil. {@code id} est {@code null}
 * pour un nouveau Pet, sinon il doit référencer un Pet déjà possédé par
 * l'utilisateur.
 */
public record PetUpdateRequest(

        Long id,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        PetType type,

        @Size(max = 100)
        String breed,

        @NotNull
        PetGender gender,

        @Min(0)
        @Max(50)
        Integer age,

        @NotNull
        EnergyLevel energyLevel,

        @Size(max = 1000)
        String description,

        Boolean vaccinated,

        Boolean neutered,

        @Valid
        List<PhotoUpdateRequest> photos) {
}