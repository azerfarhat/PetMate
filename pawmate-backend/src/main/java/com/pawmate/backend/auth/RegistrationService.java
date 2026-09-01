package com.pawmate.backend.auth;

import com.pawmate.backend.auth.dto.MessageResponse;
import com.pawmate.backend.auth.dto.PetPhotoRequest;
import com.pawmate.backend.auth.dto.PetRequest;
import com.pawmate.backend.auth.dto.RegisterRequest;
import com.pawmate.backend.auth.dto.ResendVerificationRequest;
import com.pawmate.backend.config.AppProperties;
import com.pawmate.backend.entity.EmailVerificationToken;
import com.pawmate.backend.entity.Pet;
import com.pawmate.backend.entity.PetPhoto;
import com.pawmate.backend.entity.User;
import com.pawmate.backend.enums.UserRole;
import com.pawmate.backend.exception.EmailAlreadyExistsException;
import com.pawmate.backend.exception.RegistrationException;
import com.pawmate.backend.exception.UserNotFoundException;
import com.pawmate.backend.exception.VerificationTokenException;
import com.pawmate.backend.mail.EmailService;
import com.pawmate.backend.repository.EmailVerificationTokenRepository;
import com.pawmate.backend.repository.PetPhotoRepository;
import com.pawmate.backend.repository.PetRepository;
import com.pawmate.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * Orchestre la création de compte Owner (User + 1..* Pets), la vérification
 * d'adresse email et le renvoi de l'email de vérification.
 *
 * Toute la création est transactionnelle : si la création d'un Pet ou l'envoi
 * de l'email échoue, le User n'est pas persisté.
 */
@Service
public class RegistrationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final PetPhotoRepository petPhotoRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public RegistrationService(UserRepository userRepository,
                               PetRepository petRepository,
                               PetPhotoRepository petPhotoRepository,
                               EmailVerificationTokenRepository verificationTokenRepository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService,
                               AppProperties appProperties) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.petPhotoRepository = petPhotoRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Un compte avec cet email existe déjà");
        }
        if (request.pets().isEmpty()) {
            throw new RegistrationException("Au moins un Pet est requis pour créer un compte");
        }

        User user = buildUser(request);
        user = userRepository.save(user);
        persistPets(user, request.pets());
        sendVerificationEmail(user);

        return new MessageResponse("Compte créé avec succès. Veuillez vérifier votre adresse email.");
    }

    @Transactional
    public MessageResponse verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new VerificationTokenException("Token de vérification invalide");
        }

        EmailVerificationToken verificationToken = verificationTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new VerificationTokenException("Token de vérification invalide"));

        return verify(verificationToken);
    }

    @Transactional
    public MessageResponse verifyByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new VerificationTokenException("Code de vérification invalide");
        }

        EmailVerificationToken verificationToken = verificationTokenRepository.findByCode(code)
                .orElseThrow(() -> new VerificationTokenException("Code de vérification invalide"));

        return verify(verificationToken);
    }

    private MessageResponse verify(EmailVerificationToken verificationToken) {
        if (verificationToken.isUsed()) {
            throw new VerificationTokenException("Jeton de vérification déjà utilisé");
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VerificationTokenException("Jeton de vérification expiré");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        return new MessageResponse("Adresse email vérifiée avec succès");
    }

    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("Aucun compte associé à cette adresse email"));

        if (user.isEmailVerified()) {
            throw new VerificationTokenException("Cette adresse email est déjà vérifiée");
        }

        // Invalide l'ancien jeton actif avant d'en créer un nouveau.
        verificationTokenRepository.invalidateActiveTokensForUser(user.getId());
        sendVerificationEmail(user);

        return new MessageResponse("Un nouvel email de vérification a été envoyé");
    }

    private User buildUser(RegisterRequest request) {
        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .profilePicture(request.profilePicture())
                .bio(request.bio())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .searchRadius(request.searchRadius())
                .role(UserRole.USER)
                .active(false)
                .emailVerified(false)
                .build();
    }

    private void persistPets(User owner, List<PetRequest> petRequests) {
        for (PetRequest petRequest : petRequests) {
            Pet pet = Pet.builder()
                    .name(petRequest.name())
                    .type(petRequest.type())
                    .breed(petRequest.breed())
                    .gender(petRequest.gender())
                    .age(petRequest.age())
                    .energyLevel(petRequest.energyLevel())
                    .description(petRequest.description())
                    .vaccinated(Boolean.TRUE.equals(petRequest.vaccinated()))
                    .neutered(Boolean.TRUE.equals(petRequest.neutered()))
                    .active(false)
                    .owner(owner)
                    .build();
            pet = petRepository.save(pet);
            persistPhotos(pet, petRequest.photos());
        }
    }

    private void persistPhotos(Pet pet, List<PetPhotoRequest> photoRequests) {
        if (photoRequests == null || photoRequests.isEmpty()) {
            return;
        }
        for (PetPhotoRequest photoRequest : photoRequests) {
            PetPhoto photo = PetPhoto.builder()
                    .url(photoRequest.url())
                    .primaryPhoto(photoRequest.primaryPhoto())
                    .displayOrder(photoRequest.displayOrder())
                    .pet(pet)
                    .build();
            petPhotoRepository.save(photo);
        }
    }

    private void sendVerificationEmail(User user) {
        String token = generateToken();
        String code = generateCode();
        long expirationMinutes = appProperties.getVerification().getTokenExpirationMinutes();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        verificationTokenRepository.save(EmailVerificationToken.builder()
                .token(token)
                .code(code)
                .user(user)
                .expiresAt(expiresAt)
                .used(false)
                .build());

        emailService.sendVerificationEmail(user, token, code, expirationMinutes);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}