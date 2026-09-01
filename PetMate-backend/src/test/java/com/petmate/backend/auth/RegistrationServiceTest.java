package com.petmate.backend.auth;

import com.petmate.backend.auth.dto.MessageResponse;
import com.petmate.backend.auth.dto.RegisterRequest;
import com.petmate.backend.auth.dto.ResendVerificationRequest;
import com.petmate.backend.config.AppProperties;
import com.petmate.backend.entity.EmailVerificationToken;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import com.petmate.backend.enums.UserRole;
import com.petmate.backend.exception.EmailAlreadyExistsException;
import com.petmate.backend.exception.EmailDeliveryException;
import com.petmate.backend.exception.RegistrationException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.exception.VerificationTokenException;
import com.petmate.backend.mail.EmailService;
import com.petmate.backend.repository.EmailVerificationTokenRepository;
import com.petmate.backend.repository.PetPhotoRepository;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.repository.UserRepository;
import com.petmate.backend.user.dto.PetRequest;
import com.petmate.backend.user.dto.PhotoUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetPhotoRepository petPhotoRepository;
    @Mock
    private EmailVerificationTokenRepository verificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    private AppProperties appProperties;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        registrationService = new RegistrationService(
                userRepository, petRepository, petPhotoRepository, verificationTokenRepository,
                passwordEncoder, emailService, appProperties);
    }

    @Test
    void register_withOnePet_createsUserTokenAndSendsEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(petRepository.save(any(Pet.class))).then(returnsFirstArg());
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded:" + inv.getArgument(0));
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).then(returnsFirstArg());

        RegisterRequest request = validRequest(List.of(validPet(null)));

        MessageResponse response = registrationService.register(request);

        assertEquals("Compte créé avec succès. Veuillez vérifier votre adresse email.", response.message());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("encoded:Secret123!", saved.getPassword());
        assertEquals(UserRole.USER, saved.getRole());
        assertFalse(saved.isActive());
        assertFalse(saved.isEmailVerified());
        assertEquals("Jane", saved.getFirstName());

        ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(petCaptor.capture());
        assertEquals(saved, petCaptor.getValue().getOwner());
        assertEquals("Rex", petCaptor.getValue().getName());
        assertFalse(petCaptor.getValue().isActive());

        verify(verificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(eq(saved), anyString(), anyString(), eq(60L));
    }

    @Test
    void register_withMultiplePets_createsAllPetsLinkedToOwner() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(petRepository.save(any(Pet.class))).then(returnsFirstArg());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).then(returnsFirstArg());

        PetRequest rex = validPet("Rex");
        PetRequest minou = new PetRequest(
                "Minou", PetType.CAT, null, PetGender.FEMALE, 3, EnergyLevel.LOW,
                "Calme", true, true, null);

        registrationService.register(validRequest(List.of(rex, minou)));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository, times(2)).save(petCaptor.capture());
        assertEquals(2, petCaptor.getAllValues().size());
        petCaptor.getAllValues().forEach(pet -> assertEquals(saved, pet.getOwner()));
    }

    @Test
    void register_withNoPet_throwsRegistrationException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        RegisterRequest request = validRequest(List.of());

        assertThrows(RegistrationException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_withExistingEmail_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RegisterRequest request = validRequest(List.of(validPet(null)));

        assertThrows(EmailAlreadyExistsException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(any(), anyString(), anyString(), anyLong());
    }

    @Test
    void register_withPhotos_savesPetPhotos() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(petRepository.save(any(Pet.class))).then(returnsFirstArg());
        when(petPhotoRepository.save(any(PetPhoto.class))).then(returnsFirstArg());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).then(returnsFirstArg());

        PetRequest pet = new PetRequest(
                "Rex", PetType.DOG, "Labrador", PetGender.MALE, 2, EnergyLevel.HIGH,
                "Câlin", true, true,
                List.of(new PhotoUpdateRequest(null, "https://cdn.pawmate.app/rex.jpg", true, 0)));

        registrationService.register(validRequest(List.of(pet)));

        ArgumentCaptor<PetPhoto> photoCaptor = ArgumentCaptor.forClass(PetPhoto.class);
        verify(petPhotoRepository).save(photoCaptor.capture());
        assertEquals("https://cdn.pawmate.app/rex.jpg", photoCaptor.getValue().getUrl());
        assertNotNull(photoCaptor.getValue().getPet());
    }

    @Test
    void register_whenEmailFails_throwsEmailDeliveryException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(petRepository.save(any(Pet.class))).then(returnsFirstArg());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).then(returnsFirstArg());
        org.mockito.Mockito.doThrow(new EmailDeliveryException("erreur smtp"))
                .when(emailService).sendVerificationEmail(any(), anyString(), anyString(), anyLong());

        assertThrows(EmailDeliveryException.class,
                () -> registrationService.register(validRequest(List.of(validPet(null)))));
    }

    @Test
    void verifyEmail_withValidToken_marksUserVerifiedAndTokenUsed() {
        User user = new User();
        user.setEmail("owner@example.com");

        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("abc123")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(verificationTokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).then(returnsFirstArg());

        MessageResponse response = registrationService.verifyEmail("abc123");

        assertEquals("Adresse email vérifiée avec succès", response.message());
        assertEquals(true, user.isEmailVerified());
        assertEquals(true, user.isActive());
        verify(userRepository).save(user);
        verify(verificationTokenRepository).save(token);
        org.junit.jupiter.api.Assertions.assertTrue(token.isUsed());
    }

    @Test
    void verifyEmail_withUnknownToken_throwsVerificationException() {
        when(verificationTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(VerificationTokenException.class, () -> registrationService.verifyEmail("inconnu"));
    }

    @Test
    void verifyEmail_withBlankToken_throwsVerificationException() {
        assertThrows(VerificationTokenException.class, () -> registrationService.verifyEmail("  "));
    }

    @Test
    void verifyEmail_withExpiredToken_throwsVerificationException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("expired")
                .user(new User())
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
        when(verificationTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenException.class, () -> registrationService.verifyEmail("expired"));
    }

    @Test
    void verifyEmail_withUsedToken_throwsVerificationException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("used")
                .user(new User())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .build();
        when(verificationTokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenException.class, () -> registrationService.verifyEmail("used"));
    }

    @Test
    void verifyByCode_withValidCode_marksUserVerifiedAndTokenUsed() {
        User user = new User();
        user.setEmail("owner@example.com");

        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("abc123")
                .code("483920")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(verificationTokenRepository.findByCode("483920")).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).then(returnsFirstArg());

        MessageResponse response = registrationService.verifyByCode("483920");

        assertEquals("Adresse email vérifiée avec succès", response.message());
        assertEquals(true, user.isEmailVerified());
        verify(userRepository).save(user);
        verify(verificationTokenRepository).save(token);
        org.junit.jupiter.api.Assertions.assertTrue(token.isUsed());
    }

    @Test
    void verifyByCode_withUnknownCode_throwsVerificationException() {
        when(verificationTokenRepository.findByCode(anyString())).thenReturn(Optional.empty());

        assertThrows(VerificationTokenException.class, () -> registrationService.verifyByCode("000000"));
    }

    @Test
    void verifyByCode_withExpiredToken_throwsVerificationException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("expired")
                .code("111111")
                .user(new User())
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
        when(verificationTokenRepository.findByCode("111111")).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenException.class, () -> registrationService.verifyByCode("111111"));
    }

    @Test
    void verifyByCode_withUsedToken_throwsVerificationException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("used")
                .code("222222")
                .user(new User())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .build();
        when(verificationTokenRepository.findByCode("222222")).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenException.class, () -> registrationService.verifyByCode("222222"));
    }

    @Test
    void resendVerification_invalidatesOldTokenSendsNewEmailAndDoesNotVerify() {
        User user = new User();
        user.setEmail("owner@example.com");
        user.setEmailVerified(false);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).then(returnsFirstArg());

        MessageResponse response = registrationService.resendVerification(
                new ResendVerificationRequest("owner@example.com"));

        assertEquals("Un nouvel email de vérification a été envoyé", response.message());
        verify(verificationTokenRepository).invalidateActiveTokensForUser(user.getId());
        verify(emailService).sendVerificationEmail(eq(user), anyString(), anyString(), eq(60L));
        assertFalse(user.isEmailVerified());
    }

    @Test
    void resendVerification_withUnknownEmail_throwsUserNotFoundException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> registrationService.resendVerification(
                new ResendVerificationRequest("ghost@example.com")));
    }

    @Test
    void resendVerification_withAlreadyVerifiedEmail_throwsVerificationException() {
        User user = new User();
        user.setEmail("owner@example.com");
        user.setEmailVerified(true);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        assertThrows(VerificationTokenException.class, () -> registrationService.resendVerification(
                new ResendVerificationRequest("owner@example.com")));
    }

    private RegisterRequest validRequest(List<PetRequest> pets) {
        return new RegisterRequest(
                "Jane", "Doe", "jane.doe@example.com", "Secret123!",
                null, null, null, null, null, pets);
    }

    private PetRequest validPet(String name) {
        return new PetRequest(
                name == null ? "Rex" : name, PetType.DOG, "Labrador", PetGender.MALE,
                2, EnergyLevel.HIGH, "Très joueur", true, true, null);
    }
}