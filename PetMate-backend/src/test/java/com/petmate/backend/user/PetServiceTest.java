package com.petmate.backend.user;

import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.RegistrationException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.PetPhotoRepository;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.repository.UserRepository;
import com.petmate.backend.user.dto.PetRequest;
import com.petmate.backend.user.dto.PetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    private static final long OWNER_ID = 42L;
    private static final long VIEWER_ID = 7L;

    @Mock
    private PetRepository petRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PetPhotoRepository petPhotoRepository;
    @Mock
    private BlockRepository blockRepository;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, userRepository,
                new PetPhotoApplier(petPhotoRepository), blockRepository);
    }

    @Test
    void getOwnedPet_returnsAllPetInformationWhenOwned() {
        Pet pet = pet(1L, owner(OWNER_ID));
        when(petRepository.findByIdWithPhotos(1L)).thenReturn(Optional.of(pet));

        PetResponse response = petService.getOwnedPet(OWNER_ID, 1L);

        assertEquals(1L, response.id());
        assertEquals(OWNER_ID, response.ownerId());
        assertEquals("Rex", response.name());
        assertEquals(PetType.DOG, response.type());
        assertEquals("Labrador", response.breed());
        assertEquals(PetGender.MALE, response.gender());
        assertEquals(2, response.age());
        assertEquals(EnergyLevel.HIGH, response.energyLevel());
        assertEquals("Très joueur", response.description());
        assertEquals(true, response.vaccinated());
        assertEquals(true, response.neutered());
        assertEquals(1, response.photos().size());
        assertEquals("https://cdn.pawmate.app/Rex.jpg", response.photos().get(0).url());
    }

    @Test
    void getOwnedPet_withUnknownId_throwsPetNotFound() {
        when(petRepository.findByIdWithPhotos(99L)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class, () -> petService.getOwnedPet(OWNER_ID, 99L));
    }

    @Test
    void getOwnedPet_withPetOfAnotherUser_throwsPetNotFound() {
        Pet pet = pet(1L, owner(7L));
        when(petRepository.findByIdWithPhotos(1L)).thenReturn(Optional.of(pet));

        assertThrows(PetNotFoundException.class, () -> petService.getOwnedPet(OWNER_ID, 1L));
    }

    @Test
    void listOwnedPets_returnsOnlyActivePetsWithPhotos() {
        Pet rex = pet(1L, owner(OWNER_ID));
        Pet lea = pet(2L, owner(OWNER_ID));
        lea.setActive(false);
        when(petRepository.findAllActiveByOwnerIdWithPhotos(OWNER_ID)).thenReturn(List.of(rex));

        List<PetResponse> pets = petService.listOwnedPets(OWNER_ID);

        assertEquals(1, pets.size());
        assertEquals("Rex", pets.get(0).name());
        assertEquals(1, pets.get(0).photos().size());
        verify(petRepository).findAllActiveByOwnerIdWithPhotos(OWNER_ID);
    }

    @Test
    void listPublicPets_activeOwner_returnsOnlyActivePetsWithPhotos() {
        Pet rex = pet(1L, owner(OWNER_ID));
        Pet lea = pet(2L, owner(OWNER_ID));
        lea.setActive(false);
        when(userRepository.findByIdAndActiveTrue(OWNER_ID)).thenReturn(Optional.of(owner(OWNER_ID)));
        when(petRepository.findAllActiveByOwnerIdWithPhotos(OWNER_ID)).thenReturn(List.of(rex));

        List<PetResponse> pets = petService.listPublicPets(VIEWER_ID, OWNER_ID);

        assertEquals(1, pets.size());
        assertEquals("Rex", pets.get(0).name());
        assertEquals(1, pets.get(0).photos().size());
        verify(blockRepository).existBetweenOwners(VIEWER_ID, OWNER_ID);
    }

    @Test
    void listPublicPets_unknownOrInactiveOwner_throwsUserNotFound() {
        when(userRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> petService.listPublicPets(VIEWER_ID, 99L));
        verify(blockRepository, never()).existBetweenOwners(anyLong(), anyLong());
        verify(petRepository, never()).findAllActiveByOwnerIdWithPhotos(anyLong());
    }

    @Test
    void listPublicPets_whenBlocked_throwsUserNotFound() {
        when(userRepository.findByIdAndActiveTrue(OWNER_ID)).thenReturn(Optional.of(owner(OWNER_ID)));
        when(blockRepository.existBetweenOwners(VIEWER_ID, OWNER_ID)).thenReturn(true);

        assertThrows(UserNotFoundException.class, () -> petService.listPublicPets(VIEWER_ID, OWNER_ID));
        verify(petRepository, never()).findAllActiveByOwnerIdWithPhotos(anyLong());
    }

    @Test
    void listPublicPets_ownPets_skipsBlockCheck() {
        Pet rex = pet(1L, owner(VIEWER_ID));
        when(userRepository.findByIdAndActiveTrue(VIEWER_ID)).thenReturn(Optional.of(owner(VIEWER_ID)));
        when(petRepository.findAllActiveByOwnerIdWithPhotos(VIEWER_ID)).thenReturn(List.of(rex));

        petService.listPublicPets(VIEWER_ID, VIEWER_ID);

        verify(blockRepository, never()).existBetweenOwners(anyLong(), anyLong());
    }

    @Test
    void createOwnedPet_savesActivePetOwnedByUser() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner(OWNER_ID)));
        Pet saved = pet(5L, owner(OWNER_ID));
        when(petRepository.save(any(Pet.class))).thenReturn(saved);

        PetResponse response = petService.createOwnedPet(OWNER_ID, petRequest());

        assertEquals(5L, response.id());
        assertEquals("Rex", response.name());
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    void createOwnedPet_withUnknownUser_throwsUserNotFound() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> petService.createOwnedPet(OWNER_ID, petRequest()));
        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void updateOwnedPet_updatesAllFieldsAndForcesActive() {
        Pet pet = pet(1L, owner(OWNER_ID));
        when(petRepository.findOwnedActiveById(OWNER_ID, 1L)).thenReturn(Optional.of(pet));

        PetResponse response = petService.updateOwnedPet(OWNER_ID, 1L,
                new PetRequest("Rex Junior", PetType.DOG, "Labrador", PetGender.MALE, 3,
                        EnergyLevel.HIGH, "Nouvelle bio", true, false, null));

        assertEquals("Rex Junior", pet.getName());
        assertEquals(3, pet.getAge());
        assertFalse(pet.isNeutered());
        assertTrue(pet.isActive());
        assertEquals("Rex Junior", response.name());
    }

    @Test
    void updateOwnedPet_withInactivePet_throwsPetNotFound() {
        when(petRepository.findOwnedActiveById(OWNER_ID, 1L)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class,
                () -> petService.updateOwnedPet(OWNER_ID, 1L, petRequest()));
    }

    @Test
    void updateOwnedPet_withPetOfAnotherUser_throwsPetNotFound() {
        when(petRepository.findOwnedActiveById(OWNER_ID, 1L)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class,
                () -> petService.updateOwnedPet(OWNER_ID, 1L, petRequest()));
    }

    @Test
    void deleteOwnedPet_deactivatesPet() {
        Pet pet = pet(1L, owner(OWNER_ID));
        when(petRepository.findOwnedActiveById(OWNER_ID, 1L)).thenReturn(Optional.of(pet));
        when(petRepository.countActiveByOwnerIdAndIdNot(OWNER_ID, 1L)).thenReturn(1L);

        petService.deleteOwnedPet(OWNER_ID, 1L);

        assertFalse(pet.isActive());
        verify(petRepository).save(pet);
    }

    @Test
    void deleteOwnedPet_lastActivePet_throwsRegistrationException() {
        Pet pet = pet(1L, owner(OWNER_ID));
        when(petRepository.findOwnedActiveById(OWNER_ID, 1L)).thenReturn(Optional.of(pet));
        when(petRepository.countActiveByOwnerIdAndIdNot(OWNER_ID, 1L)).thenReturn(0L);

        assertThrows(RegistrationException.class, () -> petService.deleteOwnedPet(OWNER_ID, 1L));
        assertTrue(pet.isActive());
    }

    @Test
    void deleteOwnedPet_withPetOfAnotherUser_throwsPetNotFound() {
        when(petRepository.findOwnedActiveById(OWNER_ID, 1L)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class, () -> petService.deleteOwnedPet(OWNER_ID, 1L));
    }

    private User owner(Long id) {
        return User.builder().id(id).firstName("Jane").lastName("Doe")
                .email("jane@example.com").active(true).build();
    }

    private Pet pet(Long id, User owner) {
        Pet pet = Pet.builder()
                .id(id).name("Rex").type(PetType.DOG).breed("Labrador")
                .gender(PetGender.MALE).age(2).energyLevel(EnergyLevel.HIGH)
                .description("Très joueur").vaccinated(true).neutered(true)
                .owner(owner).active(true).build();
        pet.getPhotos().add(PetPhoto.builder()
                .id(id * 10).url("https://cdn.pawmate.app/Rex.jpg")
                .primaryPhoto(true).displayOrder(0).pet(pet).build());
        return pet;
    }

    private PetRequest petRequest() {
        return new PetRequest(
                "Rex", PetType.DOG, "Labrador", PetGender.MALE, 2,
                EnergyLevel.HIGH, "Très joueur", true, true, null);
    }
}