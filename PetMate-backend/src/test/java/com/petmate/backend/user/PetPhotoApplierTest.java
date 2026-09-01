package com.petmate.backend.user;

import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.repository.PetPhotoRepository;
import com.petmate.backend.user.dto.PhotoUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie la garantie "une seule photo principale" du {@code PetPhotoApplier}
 * ainsi que le maintien de la collection lors d'un remplacement photos.
 */
@ExtendWith(MockitoExtension.class)
class PetPhotoApplierTest {

    @Mock
    private PetPhotoRepository petPhotoRepository;

    private PetPhotoApplier applier;

    @BeforeEach
    void setUp() {
        applier = new PetPhotoApplier(petPhotoRepository);
    }

    @Test
    void replaceAll_withoutPrimary_declaresFirstByDisplayOrderAsPrimary() {
        when(petPhotoRepository.save(any(PetPhoto.class))).then(returnsFirstArg());

        applier.replaceAll(petWithoutPhotos(), List.of(
                new PhotoUpdateRequest(null, "https://cdn/a.jpg", false, 2),
                new PhotoUpdateRequest(null, "https://cdn/b.jpg", false, 1)));

        Map<String, Boolean> primaryByUrl = savedPhotosByUrl();

        assertEquals(2, primaryByUrl.size());
        assertFalse(primaryByUrl.get("https://cdn/a.jpg"));
        assertTrue(primaryByUrl.get("https://cdn/b.jpg"), "la photo la mieux classée devient primaire");
    }

    @Test
    void replaceAll_withMultiplePrimaries_keepsOnlyTheFirst() {
        when(petPhotoRepository.save(any(PetPhoto.class))).then(returnsFirstArg());

        applier.replaceAll(petWithoutPhotos(), List.of(
                new PhotoUpdateRequest(null, "https://cdn/a.jpg", true, null),
                new PhotoUpdateRequest(null, "https://cdn/b.jpg", true, null)));

        Map<String, Boolean> primaryByUrl = savedPhotosByUrl();

        assertTrue(primaryByUrl.get("https://cdn/a.jpg"));
        assertFalse(primaryByUrl.get("https://cdn/b.jpg"));
        assertEquals(1, primaryByUrl.values().stream().filter(v -> v).count());
    }

    @Test
    void replaceAll_removesAbsentPhotosFromDbAndCollection() {
        Pet pet = petWithPhotos(
                photo(10L, "https://cdn/kept.jpg", true, 0),
                photo(20L, "https://cdn/dropped.jpg", false, 1));

        applier.replaceAll(pet, List.of(
                new PhotoUpdateRequest(10L, "https://cdn/kept.jpg", true, 0)));

        verify(petPhotoRepository).deleteByPetIdAndPhotoIds(1L, List.of(20L));
        assertEquals(1, pet.getPhotos().size(), "la collection est purgée des photos supprimées");
        assertEquals("https://cdn/kept.jpg", pet.getPhotos().get(0).getUrl());
    }

    @Test
    void replaceAll_withPhotoOfAnotherPet_throwsPetNotFound() {
        Pet pet = petWithPhotos(photo(10L, "https://cdn/old.jpg", true, 0));

        assertThrows(PetNotFoundException.class, () -> applier.replaceAll(pet,
                List.of(new PhotoUpdateRequest(99L, "https://cdn/x.jpg", true, 0))));
    }

    private Map<String, Boolean> savedPhotosByUrl() {
        ArgumentCaptor<PetPhoto> captor = ArgumentCaptor.forClass(PetPhoto.class);
        verify(petPhotoRepository, times(2)).save(captor.capture());
        return captor.getAllValues().stream()
                .collect(Collectors.toMap(PetPhoto::getUrl, PetPhoto::isPrimaryPhoto));
    }

    private Pet petWithoutPhotos() {
        return Pet.builder()
                .id(1L).name("Rex").type(PetType.DOG).gender(PetGender.MALE)
                .energyLevel(EnergyLevel.HIGH).active(true).build();
    }

    private Pet petWithPhotos(PetPhoto... photos) {
        Pet pet = petWithoutPhotos();
        for (PetPhoto photo : photos) {
            pet.getPhotos().add(photo);
        }
        return pet;
    }

    private PetPhoto photo(Long id, String url, boolean primary, int order) {
        return PetPhoto.builder().id(id).url(url).primaryPhoto(primary).displayOrder(order).build();
    }
}