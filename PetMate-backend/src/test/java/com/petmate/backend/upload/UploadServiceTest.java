package com.petmate.backend.upload;

import com.petmate.backend.config.StorageProperties;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.enums.UploadTargetType;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.UploadException;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.storage.ObjectStorage;
import com.petmate.backend.upload.dto.PresignUploadRequest;
import com.petmate.backend.upload.dto.PresignUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'upload est délégué au stockage d'objets : le backend ne touche jamais les
 * octets. Le service doit refuser toute cible illégitime (Pet d'un autre
 * owner, profil d'autrui), tout type MIME hors liste et toute taille hors
 * limites, et forger des clés propres (UUID, extension déduite du MIME).
 */
@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    private static final long ME = 14L;
    private static final long PET_ID = 13L;

    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private PetRepository petRepository;

    private StorageProperties properties;
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setMaxFileBytes(5L * 1024 * 1024);
        properties.setAllowedContentTypes(new HashSet<>(Set.of("image/jpeg", "image/png", "image/webp")));
        uploadService = new UploadService(objectStorage, petRepository, properties);
    }

    @Test
    void presignUpload_ownPet_buildsPetKeyAndSigns() {
        when(petRepository.findOwnedActiveById(ME, PET_ID)).thenReturn(Optional.of(Pet.builder().id(PET_ID).build()));
        when(objectStorage.presignPut(anyString(), eq("image/jpeg"), eq(1024L), any())).thenReturn("http://upload/signed");
        when(objectStorage.publicUrl(anyString())).thenAnswer(invocation -> "http://public/" + invocation.getArgument(0));

        PresignUploadResponse response = uploadService.presignUpload(
                ME, request(PET_ID, UploadTargetType.PET, "image/jpeg", 1024L));

        assertTrue(response.key().startsWith("pets/13/"));
        assertTrue(response.key().endsWith(".jpg"));
        assertEquals("http://upload/signed", response.uploadUrl());
        assertEquals("http://public/" + response.key(), response.finalUrl());
        verify(petRepository).findOwnedActiveById(ME, PET_ID);
    }

    @Test
    void presignUpload_foreignPet_throwsNotFound() {
        when(petRepository.findOwnedActiveById(ME, PET_ID)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class,
                () -> uploadService.presignUpload(ME, request(PET_ID, UploadTargetType.PET, "image/jpeg", 1024L)));
    }

    @Test
    void presignUpload_ownProfile_buildsAvatarKey() {
        when(objectStorage.presignPut(anyString(), eq("image/png"), eq(512L), any())).thenReturn("http://upload/signed");
        when(objectStorage.publicUrl(anyString())).thenAnswer(invocation -> "http://public/" + invocation.getArgument(0));

        PresignUploadResponse response = uploadService.presignUpload(
                ME, request(ME, UploadTargetType.PROFILE, "image/png", 512L));

        assertTrue(response.key().startsWith("users/14/avatar/"));
        assertTrue(response.key().endsWith(".png"));
        verify(petRepository, never()).findOwnedActiveById(anyLong(), anyLong());
    }

    @Test
    void presignUpload_anotherProfile_throwsUploadException() {
        assertThrows(UploadException.class,
                () -> uploadService.presignUpload(ME, request(99L, UploadTargetType.PROFILE, "image/png", 512L)));
    }

    @Test
    void presignUpload_webp_usesWebpExtension() {
        when(petRepository.findOwnedActiveById(ME, PET_ID)).thenReturn(Optional.of(Pet.builder().id(PET_ID).build()));
        when(objectStorage.presignPut(anyString(), eq("image/webp"), eq(100L), any())).thenReturn("http://upload/signed");
        when(objectStorage.publicUrl(anyString())).thenAnswer(invocation -> "http://public/" + invocation.getArgument(0));

        PresignUploadResponse response = uploadService.presignUpload(
                ME, request(PET_ID, UploadTargetType.PET, "image/webp", 100L));

        assertTrue(response.key().endsWith(".webp"));
    }

    @Test
    void presignUpload_forbiddenContentType_throws() {
        assertThrows(UploadException.class,
                () -> uploadService.presignUpload(ME, request(PET_ID, UploadTargetType.PET, "text/html", 100L)));
    }

    @Test
    void presignUpload_oversizeFile_throws() {
        long tooBig = 6L * 1024 * 1024;
        assertThrows(UploadException.class,
                () -> uploadService.presignUpload(ME, request(PET_ID, UploadTargetType.PET, "image/jpeg", tooBig)));
    }

    @Test
    void presignUpload_zeroOrNegativeSize_throws() {
        assertThrows(UploadException.class,
                () -> uploadService.presignUpload(ME, request(PET_ID, UploadTargetType.PET, "image/jpeg", 0L)));
        assertThrows(UploadException.class,
                () -> uploadService.presignUpload(ME, request(PET_ID, UploadTargetType.PET, "image/jpeg", -10L)));
    }

    private PresignUploadRequest request(Long targetId, UploadTargetType type, String contentType, long size) {
        return new PresignUploadRequest(targetId, type, contentType, size);
    }
}