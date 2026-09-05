package com.petmate.backend.user;

import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.User;
import com.petmate.backend.user.dto.PetPhotoResponse;
import com.petmate.backend.user.dto.PetResponse;
import com.petmate.backend.user.dto.UserResponse;

import java.util.Comparator;
import java.util.List;

/**
 * Mapping entités JPA {@code User}/{@code Pet}/{@code PetPhoto} vers les DTO
 * de réponse du profil, sans exposer les entités.
 */
public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserResponse toResponse(User user) {
        List<PetResponse> pets = user.getPets().stream()
                .sorted(Comparator.comparing(Pet::getId))
                .map(UserProfileMapper::toPetResponse)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getBio(),
                user.getLatitude(),
                user.getLongitude(),
                user.getSearchRadius(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                pets);
    }

    public static PetResponse toPetResponse(Pet pet) {
        List<PetPhotoResponse> photos = pet.getPhotos().stream()
                .sorted(Comparator.comparing(PetPhoto::getDisplayOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(UserProfileMapper::toPhoto)
                .toList();

        return new PetResponse(
                pet.getId(),
                pet.getOwner().getId(),
                pet.getName(),
                pet.getType(),
                pet.getBreed(),
                pet.getGender(),
                pet.getAge(),
                pet.getEnergyLevel(),
                pet.getDescription(),
                pet.isVaccinated(),
                pet.isNeutered(),
                photos);
    }

    private static PetPhotoResponse toPhoto(PetPhoto photo) {
        return new PetPhotoResponse(
                photo.getId(),
                photo.getUrl(),
                photo.isPrimaryPhoto(),
                photo.getDisplayOrder());
    }
}