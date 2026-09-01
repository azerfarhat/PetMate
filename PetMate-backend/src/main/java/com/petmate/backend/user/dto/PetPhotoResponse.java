package com.petmate.backend.user.dto;

/**
 * Photo d'un Pet exposée dans le profil.
 */
public record PetPhotoResponse(
        Long id,
        String url,
        boolean primaryPhoto,
        Integer displayOrder) {
}