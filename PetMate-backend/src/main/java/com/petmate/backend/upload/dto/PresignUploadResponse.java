package com.petmate.backend.upload.dto;

/**
 * Résultat d'une pré-signature : l'app uploade la photo directement via
 * {@code uploadUrl} (méthode PUT, en-têtes Content-Type et Content-Length
 * obligatoirement identiques à la demande), puis enregistre {@code finalUrl}
 * dans son profil / sa Pet.
 */
public record PresignUploadResponse(
        String uploadUrl,
        String finalUrl,
        String key,
        String contentType) {
}