package com.petmate.backend.upload;

import com.petmate.backend.config.StorageProperties;
import com.petmate.backend.enums.UploadTargetType;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.UploadException;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.storage.ObjectStorage;
import com.petmate.backend.upload.dto.PresignUploadRequest;
import com.petmate.backend.upload.dto.PresignUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * Signature d'upload déléguée au stockage d'objets. Le backend ne reçoit
 * jamais les fichiers : il vérifie la légitimité de la cible (les photos d'une
 * Pet ne sont modifiables que par son owner), valide type MIME et taille, puis
 * signe une URL d'upload directe, éphémère et verrouillée sur ces valeurs.
 */
@Service
public class UploadService {

    private final ObjectStorage objectStorage;
    private final PetRepository petRepository;
    private final StorageProperties properties;

    public UploadService(ObjectStorage objectStorage,
                         PetRepository petRepository,
                         StorageProperties properties) {
        this.objectStorage = objectStorage;
        this.petRepository = petRepository;
        this.properties = properties;
    }

    /**
     * Pré-signe un upload pour une Pet de l'utilisateur ou son propre profil.
     *
     * @param userId identité tirée du JWT (jamais du corps de requête)
     */
    @Transactional(readOnly = true)
    public PresignUploadResponse presignUpload(Long userId, PresignUploadRequest request) {
        validateContentType(request.contentType());
        validateContentLength(request.contentLength());

        String key = buildKey(userId, request.targetType(), request.targetId(), request.contentType());
        String uploadUrl = objectStorage.presignPut(
                key,
                request.contentType(),
                request.contentLength(),
                Duration.ofMinutes(properties.getPresignExpirationMinutes()));

        return new PresignUploadResponse(uploadUrl, objectStorage.publicUrl(key), key, request.contentType());
    }

    /**
     * Construit la clé de l'objet et vérifie que l'utilisateur a bien le droit
     * d'écrire sur cette cible (404 ambigu côté Pet, 400 côté profil).
     */
    private String buildKey(Long userId, UploadTargetType targetType, Long targetId, String contentType) {
        String filename = UUID.randomUUID().toString().replace("-", "")
                + extensionFromContentType(contentType);
        return switch (targetType) {
            case PET -> {
                // La Pet doit exister ET appartenir à l'utilisateur authentifié.
                petRepository.findOwnedActiveById(userId, targetId)
                        .orElseThrow(() -> new PetNotFoundException("Pet introuvable"));
                yield "pets/" + targetId + "/" + filename;
            }
            case PROFILE -> {
                if (!targetId.equals(userId)) {
                    throw new UploadException("Cible d'upload invalide");
                }
                yield "users/" + userId + "/avatar/" + filename;
            }
        };
    }

    private void validateContentType(String contentType) {
        if (!properties.getAllowedContentTypes().contains(contentType)) {
            throw new UploadException("Type de fichier non autorisé : " + contentType);
        }
    }

    private void validateContentLength(Long contentLength) {
        if (contentLength == null || contentLength <= 0) {
            throw new UploadException("Taille de fichier invalide");
        }
        if (contentLength > properties.getMaxFileBytes()) {
            throw new UploadException("Fichier trop volumineux (maximum "
                    + (properties.getMaxFileBytes() / (1024 * 1024)) + " Mo)");
        }
    }

    /** Extension de fichier déduite du MIME (JPEG→jpg, PNG→png, WEBP→webp). */
    private static String extensionFromContentType(String contentType) {
        String base = contentType.toLowerCase(Locale.ROOT);
        return base.contains("jpeg") ? ".jpg"
                : base.contains("png") ? ".png"
                : base.contains("webp") ? ".webp"
                : "";
    }
}