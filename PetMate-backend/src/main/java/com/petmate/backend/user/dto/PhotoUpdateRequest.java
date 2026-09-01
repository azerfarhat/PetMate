package com.petmate.backend.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Photo d'un Pet lors d'une mise à jour. {@code id} est {@code null} pour une
 * nouvelle photo, sinon il doit référencer une photo du Pet concerné.
 */
public record PhotoUpdateRequest(

        Long id,

        @NotBlank
        @Size(max = 2048)
        @Pattern(
                regexp = "^(http|https)://\\S+$",
                message = "L'URL de la photo doit être une URL HTTP(S)")
        String url,

        boolean primaryPhoto,

        @Min(0)
        Integer displayOrder) {
}