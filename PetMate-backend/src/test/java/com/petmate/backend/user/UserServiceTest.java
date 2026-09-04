package com.petmate.backend.user;

import com.petmate.backend.config.AppProperties;
import com.petmate.backend.entity.PasswordChangeToken;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import com.petmate.backend.exception.PasswordChangeException;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.RateLimitExceededException;
import com.petmate.backend.exception.RegistrationException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.mail.EmailService;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.PasswordChangeTokenRepository;
import com.petmate.backend.repository.PetPhotoRepository;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.repository.UserRepository;
import com.petmate.backend.security.RefreshTokenStore;
import com.petmate.backend.user.dto.PasswordChangeRequest;
import com.petmate.backend.user.dto.PhotoUpdateRequest;
import com.petmate.backend.user.dto.PetUpdateRequest;
import com.petmate.backend.user.dto.PublicUserResponse;
import com.petmate.backend.user.dto.UpdateProfileRequest;
import com.petmate.backend.user.dto.UserResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 42L;
    private static final long VIEWER_ID = 7L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetPhotoRepository petPhotoRepository;
    @Mock
    private PasswordChangeTokenRepository passwordChangeTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private BlockRepository blockRepository;

    private AppProperties appProperties;
    private PetPhotoApplier petPhotoApplier;
    private UserService userService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        petPhotoApplier = new PetPhotoApplier(petPhotoRepository);
        userService = new UserService(
                userRepository, petRepository, petPhotoRepository,
                passwordChangeTokenRepository,
                passwordEncoder, emailService, appProperties, refreshTokenStore,
                petPhotoApplier, blockRepository);
    }

    @Test
    void me_returnsMappedProfileWithPetsAndPhotos() {
        User user = owner();
        user.getPets().add(pet(1L, "Rex"));
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(petPhotoRepository.findByPetIds(any()))
                .thenReturn(List.of(user.getPets().get(0).getPhotos().get(0)));

        UserResponse response = userService.me(USER_ID);

        assertEquals("Jane", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("jane@example.com", response.email());
        assertTrue(response.emailVerified());
        assertEquals(1, response.pets().size());
        assertEquals("Rex", response.pets().get(0).name());
        assertEquals(1, response.pets().get(0).photos().size());
        assertEquals("https://cdn.pawmate.app/Rex.jpg", response.pets().get(0).photos().get(0).url());
    }

    @Test
    void me_withUnknownUser_throwsUserNotFound() {
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.me(USER_ID));
    }

    @Test
    void publicProfile_activeUser_returnsOnlyPublicInfo() {
        User target = owner();
        target.setBio("Amoureux des chats");
        target.setProfilePicture("https://cdn/avatar.jpg");
        target.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        when(userRepository.findByIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(target));

        PublicUserResponse response = userService.publicProfile(VIEWER_ID, USER_ID);

        assertEquals(USER_ID, response.id());
        assertEquals("Jane", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("Amoureux des chats", response.bio());
        assertEquals("https://cdn/avatar.jpg", response.profilePicture());
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 0), response.createdAt());
        verify(blockRepository).existBetweenOwners(VIEWER_ID, USER_ID);
    }

    @Test
    void publicProfile_unknownOrInactiveUser_throwsUserNotFound() {
        when(userRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.publicProfile(VIEWER_ID, 99L));
        verify(blockRepository, never()).existBetweenOwners(anyLong(), anyLong());
    }

    @Test
    void publicProfile_whenBlocked_throwsUserNotFound() {
        when(userRepository.findByIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(owner()));
        when(blockRepository.existBetweenOwners(VIEWER_ID, USER_ID)).thenReturn(true);

        assertThrows(UserNotFoundException.class, () -> userService.publicProfile(VIEWER_ID, USER_ID));
    }

    @Test
    void publicProfile_ownProfile_skipsBlockCheck() {
        when(userRepository.findByIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(owner()));

        PublicUserResponse response = userService.publicProfile(USER_ID, USER_ID);

        assertEquals(USER_ID, response.id());
        verify(blockRepository, never()).existBetweenOwners(anyLong(), anyLong());
    }

    @Test
    void updateProfile_updatesScalarFieldsAndExistingPet() {
        User user = owner();
        Pet rex = pet(1L, "Rex");
        user.getPets().add(rex);
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        stubProfilePhotos(user);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "John", "Smith", "https://cdn/avatar.jpg", "Nouvelle bio",
                45.5, 4.8, 40, List.of(petUpdate(1L, "Rex Junior")));

        UserResponse response = userService.updateProfile(USER_ID, request);

        assertEquals("John", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("https://cdn/avatar.jpg", user.getProfilePicture());
        assertEquals("Nouvelle bio", user.getBio());
        assertEquals(45.5, user.getLatitude());
        assertEquals(4.8, user.getLongitude());
        assertEquals(40, user.getSearchRadius());
        assertEquals("Rex Junior", rex.getName());
        verify(userRepository, never()).deleteById(any());
        assertEquals("Rex Junior", response.pets().get(0).name());
    }

    @Test
    void updateProfile_addsNewPetWhenIdIsNull() {
        User user = owner();
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        Pet saved = pet(3L, "Minou");
        when(petRepository.save(any(Pet.class))).thenReturn(saved);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane", "Doe", null, null, null, null, null, List.of(petUpdate(null, "Minou")));

        userService.updateProfile(USER_ID, request);

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(captor.capture());
        assertEquals("Minou", captor.getValue().getName());
        assertEquals(user, captor.getValue().getOwner());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void updateProfile_removesMissingPetAndItsPhotos() {
        User user = owner();
        user.getPets().add(pet(1L, "Rex"));
        user.getPets().add(pet(2L, "Lea"));
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        stubProfilePhotos(user);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane", "Doe", null, null, null, null, null,
                List.of(petUpdate(1L, "Rex")));

        userService.updateProfile(USER_ID, request);

        verify(petPhotoRepository).deleteByPetIds(List.of(2L));
        verify(petRepository).deleteByIds(List.of(2L));
    }

    @Test
    void updateProfile_removesPhotosAbsentFromKeptPet() {
        User user = owner();
        Pet rex = pet(1L, "Rex");
        user.getPets().add(rex);
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        stubProfilePhotos(user);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane", "Doe", null, null, null, null, null,
                List.of(new PetUpdateRequest(
                        1L, "Rex", PetType.DOG, "Labrador", PetGender.MALE, 2,
                        EnergyLevel.HIGH, "Joueur", true, true, List.of())));

        userService.updateProfile(USER_ID, request);

        verify(petPhotoRepository).deleteByPetIdAndPhotoIds(1L, List.of(10L));
    }

    @Test
    void updateProfile_withEmptyPets_throwsRegistrationException() {
        User user = owner();
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane", "Doe", null, null, null, null, null, List.of());

        assertThrows(RegistrationException.class, () -> userService.updateProfile(USER_ID, request));
    }

    @Test
    void updateProfile_withUnownedPetId_throwsPetNotFound() {
        User user = owner();
        user.getPets().add(pet(1L, "Rex"));
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        stubProfilePhotos(user);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane", "Doe", null, null, null, null, null,
                List.of(petUpdate(999L, "Aliénor")));

        assertThrows(PetNotFoundException.class, () -> userService.updateProfile(USER_ID, request));
    }

    @Test
    void deleteAccount_disablesUserAndRevokesTokens() {
        User user = owner();
        user.setActive(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.deleteAccount(USER_ID);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
        verify(refreshTokenStore).revokeAllForUser(USER_ID);
    }

    @Test
    void deleteAccount_withUnknownUser_throwsUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteAccount(USER_ID));
    }

    @Test
    void requestPasswordChange_invalidatesOldTokensSavesCodeAndSendsEmail() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner()));

        userService.requestPasswordChange(USER_ID);

        verify(passwordChangeTokenRepository).invalidateActiveTokensForUser(USER_ID);
        ArgumentCaptor<PasswordChangeToken> captor = ArgumentCaptor.forClass(PasswordChangeToken.class);
        verify(passwordChangeTokenRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getCode().length());
        assertFalse(captor.getValue().isUsed());
        verify(emailService).sendPasswordChangeCodeEmail(any(), eq(captor.getValue().getCode()), eq(60L));
    }

    @Test
    void requestPasswordChange_respectsCooldown() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner()));

        userService.requestPasswordChange(USER_ID);

        assertThrows(RateLimitExceededException.class, () -> userService.requestPasswordChange(USER_ID));
        verify(emailService, times(1)).sendPasswordChangeCodeEmail(any(), anyString(), anyLong());
    }

    @Test
    void confirmPasswordChange_updatesPasswordMarksCodeUsedAndRevokesTokens() {
        User user = owner();
        PasswordChangeToken token = PasswordChangeToken.builder()
                .code("123456").user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(5)).used(false).build();
        when(passwordChangeTokenRepository.findByCode("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("encoded");

        userService.confirmPasswordChange(USER_ID, new PasswordChangeRequest("123456", "NewSecret123!"));

        verify(userRepository).save(user);
        assertEquals("encoded", user.getPassword());
        assertTrue(token.isUsed());
        verify(passwordChangeTokenRepository).save(token);
        verify(refreshTokenStore).revokeAllForUser(USER_ID);
    }

    @Test
    void confirmPasswordChange_withUnknownCode_throws() {
        when(passwordChangeTokenRepository.findByCode("999999")).thenReturn(Optional.empty());

        assertThrows(PasswordChangeException.class,
                () -> userService.confirmPasswordChange(USER_ID, new PasswordChangeRequest("999999", "NewSecret123!")));
    }

    @Test
    void confirmPasswordChange_withCodeOfAnotherUser_throws() {
        User other = owner();
        other.setId(7L);
        PasswordChangeToken token = PasswordChangeToken.builder()
                .code("123456").user(other)
                .expiresAt(LocalDateTime.now().plusMinutes(5)).used(false).build();
        when(passwordChangeTokenRepository.findByCode("123456")).thenReturn(Optional.of(token));

        assertThrows(PasswordChangeException.class,
                () -> userService.confirmPasswordChange(USER_ID, new PasswordChangeRequest("123456", "NewSecret123!")));
    }

    @Test
    void confirmPasswordChange_withUsedCode_throws() {
        User user = owner();
        PasswordChangeToken token = PasswordChangeToken.builder()
                .code("123456").user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(5)).used(true).build();
        when(passwordChangeTokenRepository.findByCode("123456")).thenReturn(Optional.of(token));

        assertThrows(PasswordChangeException.class,
                () -> userService.confirmPasswordChange(USER_ID, new PasswordChangeRequest("123456", "NewSecret123!")));
    }

    @Test
    void confirmPasswordChange_withExpiredCode_throws() {
        User user = owner();
        PasswordChangeToken token = PasswordChangeToken.builder()
                .code("123456").user(user)
                .expiresAt(LocalDateTime.now().minusMinutes(5)).used(false).build();
        when(passwordChangeTokenRepository.findByCode("123456")).thenReturn(Optional.of(token));

        assertThrows(PasswordChangeException.class,
                () -> userService.confirmPasswordChange(USER_ID, new PasswordChangeRequest("123456", "NewSecret123!")));
    }

    private User owner() {
        User user = User.builder()
                .id(USER_ID)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .password("hashed")
                .role(com.petmate.backend.enums.UserRole.USER)
                .active(true)
                .emailVerified(true)
                .build();
        return user;
    }

    private Pet pet(Long id, String name) {
        Pet pet = Pet.builder()
                .id(id).name(name).type(PetType.DOG).gender(PetGender.MALE)
                .energyLevel(EnergyLevel.HIGH).owner(owner()).active(true).build();
        pet.getPhotos().add(photo(pet, id * 10, "https://cdn.pawmate.app/" + name + ".jpg"));
        return pet;
    }

    private PetPhoto photo(Pet pet, Long id, String url) {
        return PetPhoto.builder().id(id).url(url).primaryPhoto(true).displayOrder(0).pet(pet).build();
    }

    private PetUpdateRequest petUpdate(Long id, String name) {
        return new PetUpdateRequest(
                id, name, PetType.DOG, "Labrador", PetGender.MALE, 2,
                EnergyLevel.HIGH, "Joueur", true, true, null);
    }

    /** Stub le rechargement des photos de profil, reproduisant le comportement du service. */
    private void stubProfilePhotos(User user) {
        when(petPhotoRepository.findByPetIds(any())).thenReturn(
                user.getPets().stream()
                        .flatMap(pet -> pet.getPhotos().stream())
                        .toList());
    }
}