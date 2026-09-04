package com.petmate.backend.upload.dto;

import com.petmate.backend.enums.UploadTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Demande de pré-signature d'upload. Le client déclare sa cible (Pet ou profil),
 * le type MIME et la taille du fichier ; le backend verrouille ces valeurs
 * dans la signature avant d'autoriser l'upload direct.
 */
public record PresignUploadRequest(
        @NotNull(message = "targetId ne peut pas être null")
        Long targetId,

        @NotNull(message = "targetType ne peut pas être null")
        UploadTargetType targetType,

        @NotBlank(message = "contentType ne peut pas être vide")
        String contentType,

        @NotNull(message = "contentLength ne peut pas être null")
        @Min(value = 1, message = "contentLength doit être au moins 1 octet")
        @Max(value = 10L * 1024 * 1024, message = "contentLength dépasse la limite autorisée")
        Long contentLength) {
}