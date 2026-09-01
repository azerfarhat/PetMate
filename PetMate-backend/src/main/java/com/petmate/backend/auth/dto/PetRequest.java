package com.petmate.backend.auth.dto;

import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Informations d'un Pet à créer lors de l'inscription, alignées sur les
 * attributs de l'entité {@code Pet}.
 */
public record PetRequest(

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
        Integer age,

        @NotNull
        EnergyLevel energyLevel,

        @Size(max = 1000)
        String description,

        Boolean vaccinated,

        Boolean neutered,

        @Valid
        List<PetPhotoRequest> photos) {
}