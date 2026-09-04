package com.petmate.backend.user;

import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.User;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.RegistrationException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.repository.UserRepository;
import com.petmate.backend.user.dto.PetRequest;
import com.petmate.backend.user.dto.PetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion du cycle de vie des Pet d'un utilisateur connecté : liste, création,
 * modification et suppression (soft delete). Un Pet n'est accessible qu'à son
 * propriétaire (vérification d'appartenance, aucune fuite d'information).
 *
 * Le payload {@link PetRequest} est partagé avec l'inscription : le wizard
 * (espèce, race, âge, énergie) est donc strictement identique à la création
 * d'un compte et à l'ajout d'un Pet ultérieur.
 */
@Service
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetPhotoApplier petPhotoApplier;
    private final BlockRepository blockRepository;

    public PetService(PetRepository petRepository,
                      UserRepository userRepository,
                      PetPhotoApplier petPhotoApplier,
                      BlockRepository blockRepository) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.petPhotoApplier = petPhotoApplier;
        this.blockRepository = blockRepository;
    }

    /**
     * Tous les Pet actifs de l'utilisateur, photos incluses, en une requête.
     */
    @Transactional(readOnly = true)
    public List<PetResponse> listOwnedPets(Long ownerId) {
        return petRepository.findAllActiveByOwnerIdWithPhotos(ownerId).stream()
                .map(UserProfileMapper::toPetResponse)
                .toList();
    }

    /**
     * Pet actifs publics d'un membre actif, pour le profil d'un tiers
     * (match, like, découverte). Aucune donnée privée n'est exposée.
     * Retourne 404 si le compte est inconnu/inactif ou si l'un des deux a
     * bloqué l'autre (le profil "n'existe pas"), id == viewer autorisé pour
     * revenir sur ses propres Pet.
     */
    @Transactional(readOnly = true)
    public List<PetResponse> listPublicPets(Long viewerId, Long ownerId) {
        User owner = userRepository.findByIdAndActiveTrue(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        if (!ownerId.equals(viewerId) && blockRepository.existBetweenOwners(viewerId, ownerId)) {
            throw new UserNotFoundException("Utilisateur introuvable");
        }

        return petRepository.findAllActiveByOwnerIdWithPhotos(ownerId).stream()
                .map(UserProfileMapper::toPetResponse)
                .toList();
    }

    /**
     * Crée un Pet actif immédiatement (le compte est déjà vérifié).
     *
     * {@code PetPhotoApplier} maintient la collection de photos à jour : la
     * réponse est mappée directement depuis l'entité, sans requête de relecture.
     */
    @Transactional
    public PetResponse createOwnedPet(Long ownerId, PetRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        Pet pet = buildPet(owner, request);
        pet.setActive(true);
        pet = petRepository.save(pet);
        petPhotoApplier.replaceAll(pet, request.photos());

        return UserProfileMapper.toPetResponse(pet);
    }

    /**
     * Met à jour un Pet possédé et actif (remplacement complet des champs et
     * des photos fournis). Un Pet supprimé (inactif) n'est plus modifiable.
     */
    @Transactional
    public PetResponse updateOwnedPet(Long ownerId, Long petId, PetRequest request) {
        Pet pet = ownedActivePet(ownerId, petId);
        applyFields(pet, request);
        petPhotoApplier.replaceAll(pet, request.photos());

        return UserProfileMapper.toPetResponse(pet);
    }

    /**
     * Suppression en douceur du Pet : passage à {@code active = false}. Le Pet
     * disparaît du feed de découverte et de la liste, mais l'historique des
     * matchs et des swipes reste intact (aucun viol de contrainte FK).
     *
     * L'invariant "au moins un Pet actif" (nécessaire pour swiper) est garanti :
     * la suppression du dernier Pet actif est refusée.
     */
    @Transactional
    public void deleteOwnedPet(Long ownerId, Long petId) {
        Pet pet = ownedActivePet(ownerId, petId);
        if (petRepository.countActiveByOwnerIdAndIdNot(ownerId, petId) == 0) {
            throw new RegistrationException("Au moins un Pet actif est requis");
        }

        pet.setActive(false);
        petRepository.save(pet);
    }

    /**
     * Retourne toutes les informations d'un Pet (photos comprises, une seule
     * requête) si et seulement s'il appartient à {@code ownerId}.
     */
    @Transactional(readOnly = true)
    public PetResponse getOwnedPet(Long ownerId, Long petId) {
        Pet pet = petRepository.findByIdWithPhotos(petId)
                .orElseThrow(() -> new PetNotFoundException("Pet introuvable"));

        if (!pet.getOwner().getId().equals(ownerId)) {
            throw new PetNotFoundException("Pet introuvable");
        }

        return UserProfileMapper.toPetResponse(pet);
    }

    private Pet ownedActivePet(Long ownerId, Long petId) {
        return petRepository.findOwnedActiveById(ownerId, petId)
                .orElseThrow(() -> new PetNotFoundException("Pet introuvable"));
    }

    private Pet buildPet(User owner, PetRequest request) {
        return Pet.builder()
                .name(request.name().trim())
                .type(request.type())
                .breed(blankToNull(request.breed()))
                .gender(request.gender())
                .age(request.age())
                .energyLevel(request.energyLevel())
                .description(blankToNull(request.description()))
                .vaccinated(Boolean.TRUE.equals(request.vaccinated()))
                .neutered(Boolean.TRUE.equals(request.neutered()))
                .active(false)
                .owner(owner)
                .build();
    }

    private void applyFields(Pet pet, PetRequest request) {
        pet.setName(request.name().trim());
        pet.setType(request.type());
        pet.setBreed(blankToNull(request.breed()));
        pet.setGender(request.gender());
        pet.setAge(request.age());
        pet.setEnergyLevel(request.energyLevel());
        pet.setDescription(blankToNull(request.description()));
        pet.setVaccinated(Boolean.TRUE.equals(request.vaccinated()));
        pet.setNeutered(Boolean.TRUE.equals(request.neutered()));
        pet.setActive(true);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}