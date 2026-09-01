package com.petmate.backend.user;

import com.petmate.backend.auth.dto.MessageResponse;
import com.petmate.backend.config.AppProperties;
import com.petmate.backend.entity.PasswordChangeToken;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.User;
import com.petmate.backend.exception.PasswordChangeException;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.RateLimitExceededException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.mail.EmailService;
import com.petmate.backend.repository.PasswordChangeTokenRepository;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.repository.UserRepository;
import com.petmate.backend.security.RefreshTokenStore;
import com.petmate.backend.user.dto.PetUpdateRequest;
import com.petmate.backend.user.dto.PasswordChangeRequest;
import com.petmate.backend.user.dto.UpdateProfileRequest;
import com.petmate.backend.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion du profil de l'utilisateur connecté : lecture, mise à jour
 * (champs + Pet avec condition d'au moins un Pet), suppression de compte
 * (soft delete) et changement de mot de passe sécurisé par code email
 * (usage unique, expirable, rythme limité).
 *
 * Toutes les écritures sont transactionnelles et utilisent des requêtes en
 * masse pour limiter le nombre de roundtrips SQL.
 */
@Service
public class UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final PasswordChangeTokenRepository passwordChangeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final RefreshTokenStore refreshTokenStore;
    private final PetPhotoApplier petPhotoApplier;

    /** Anti-spam : délai minimal entre deux demandes de code pour un même utilisateur. */
    private final Map<Long, Instant> lastCodeRequests = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository,
                       PetRepository petRepository,
                       PasswordChangeTokenRepository passwordChangeTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       AppProperties appProperties,
                       RefreshTokenStore refreshTokenStore,
                       PetPhotoApplier petPhotoApplier) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.passwordChangeTokenRepository = passwordChangeTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.appProperties = appProperties;
        this.refreshTokenStore = refreshTokenStore;
        this.petPhotoApplier = petPhotoApplier;
    }

    /**
     * Profil complet, chargé en une seule requête (fetch joins).
     */
    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserProfileMapper.toResponse(getProfile(userId));
    }

    /**
     * Met à jour les champs du profil et la liste des Pet (ajout / modification
     * / suppression), en garantissant au moins un Pet restant.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getProfile(userId);

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setProfilePicture(blankToNull(request.profilePicture()));
        user.setBio(blankToNull(request.bio()));
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        user.setSearchRadius(request.searchRadius());

        applyPets(user, request.pets());

        // Rechargement propre après les suppressions en masse (persistence context nettoyé).
        return UserProfileMapper.toResponse(getProfile(userId));
    }

    /**
     * Suppression de compte en douceur : l'utilisateur est désactivé et ses
     * refresh tokens révoqués (le login et les accès JWT sont immédiatement
     * bloqués). Réversible, sans cascade destructrice sur les relations.
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
        user.setActive(false);
        userRepository.save(user);
        refreshTokenStore.revokeAllForUser(userId);
    }

    /**
     * Demande un code à 6 chiffres par email pour changer son mot de passe.
     * Un seul code actif à la fois, soumis à un délai minimal anti-spam.
     */
    @Transactional
    public MessageResponse requestPasswordChange(Long userId) {
        Instant now = Instant.now();
        Instant lastRequest = lastCodeRequests.get(userId);
        long cooldownSeconds = appProperties.getPasswordChange().getCooldownSeconds();
        if (lastRequest != null && now.isBefore(lastRequest.plusSeconds(cooldownSeconds))) {
            throw new RateLimitExceededException(
                    "Un code a déjà été envoyé récemment. Réessayez dans quelques instants.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        passwordChangeTokenRepository.invalidateActiveTokensForUser(userId);

        String code = generateCode();
        long expirationMinutes = appProperties.getPasswordChange().getTokenExpirationMinutes();
        passwordChangeTokenRepository.save(PasswordChangeToken.builder()
                .code(code)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .used(false)
                .build());

        emailService.sendPasswordChangeCodeEmail(user, code, expirationMinutes);
        lastCodeRequests.put(userId, now);

        return new MessageResponse("Un code de vérification a été envoyé à votre adresse email.");
    }

    /**
     * Confirme le changement de mot de passe avec le code reçu (usage unique),
     * puis invalide tous les refresh tokens de l'utilisateur.
     */
    @Transactional
    public MessageResponse confirmPasswordChange(Long userId, PasswordChangeRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new PasswordChangeException("Code de vérification invalide");
        }

        PasswordChangeToken changeToken = passwordChangeTokenRepository.findByCode(request.code().trim())
                .orElseThrow(() -> new PasswordChangeException("Code de vérification invalide"));

        if (!changeToken.getUser().getId().equals(userId)) {
            throw new PasswordChangeException("Code de vérification invalide");
        }
        if (changeToken.isUsed()) {
            throw new PasswordChangeException("Code de vérification déjà utilisé");
        }
        if (changeToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PasswordChangeException("Code de vérification expiré, demandez-en un nouveau");
        }

        User user = changeToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        changeToken.setUsed(true);
        passwordChangeTokenRepository.save(changeToken);

        refreshTokenStore.revokeAllForUser(userId);

        return new MessageResponse("Mot de passe modifié avec succès");
    }

    /**
     * Applique la liste de Pet fournie : crée les nouveaux, met à jour les
     * existants, supprime ceux absents (photos comprises) en requêtes en masse.
     */
    private void applyPets(User user, List<PetUpdateRequest> petRequests) {
        if (petRequests.isEmpty()) {
            throw new com.petmate.backend.exception.RegistrationException(
                    "Au moins un Pet est requis");
        }

        Map<Long, Pet> existingById = user.getPets().stream()
                .collect(Collectors.toMap(Pet::getId, Function.identity()));

        Set<Long> keptPetIds = petRequests.stream()
                .map(PetUpdateRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long keptId : keptPetIds) {
            if (!existingById.containsKey(keptId)) {
                throw new PetNotFoundException("Pet introuvable");
            }
        }

        List<Long> petIdsToDelete = existingById.keySet().stream()
                .filter(id -> !keptPetIds.contains(id))
                .toList();

        for (PetUpdateRequest petRequest : petRequests) {
            if (petRequest.id() == null) {
                buildPet(user, petRequest);
            } else {
                applyPetFields(existingById.get(petRequest.id()), petRequest);
            }
        }

        if (!petIdsToDelete.isEmpty()) {
            petPhotoApplier.deleteAllByPetIds(petIdsToDelete);
        }

        if (!petIdsToDelete.isEmpty()) {
            petRepository.deleteByIds(petIdsToDelete);
        }
    }

    private void applyPetFields(Pet pet, PetUpdateRequest request) {
        pet.setName(request.name().trim());
        pet.setType(request.type());
        pet.setBreed(blankToNull(request.breed()));
        pet.setGender(request.gender());
        pet.setAge(request.age());
        pet.setEnergyLevel(request.energyLevel());
        pet.setDescription(blankToNull(request.description()));
        pet.setVaccinated(isTrue(request.vaccinated()));
        pet.setNeutered(isTrue(request.neutered()));
        pet.setActive(true);
        petPhotoApplier.replaceAll(pet, request.photos());
    }

    private Pet buildPet(User owner, PetUpdateRequest request) {
        Pet pet = Pet.builder()
                .name(request.name().trim())
                .type(request.type())
                .breed(blankToNull(request.breed()))
                .gender(request.gender())
                .age(request.age())
                .energyLevel(request.energyLevel())
                .description(blankToNull(request.description()))
                .vaccinated(isTrue(request.vaccinated()))
                .neutered(isTrue(request.neutered()))
                .active(true)
                .owner(owner)
                .build();
        pet = petRepository.save(pet);
        petPhotoApplier.replaceAll(pet, request.photos());
        return pet;
    }

    private User getProfile(Long userId) {
        return userRepository.findProfileById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}