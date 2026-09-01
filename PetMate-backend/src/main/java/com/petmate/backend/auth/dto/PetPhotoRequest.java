package com.petmate.backend.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Photo d'un Pet, alignée sur l'entité {@code PetPhoto}.
 */
public record PetPhotoRequest(

        @NotBlank
        @Size(max = 2048)
        String url,

        boolean primaryPhoto,

        @Min(0)
        Integer displayOrder) {
}