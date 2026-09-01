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
 * Payload unique du wizard Pet, partagé entre l'inscription, la création et
 * la modification d'un Pet. Les photos sont fournies par URL (l'upload sera
 * traité ultérieurement) ; une photo sans {@code id} est créée, une photo
 * existante est mise à jour.
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

    public PetRequest {
        photos = photos == null ? List.of() : List.copyOf(photos);
    }
}