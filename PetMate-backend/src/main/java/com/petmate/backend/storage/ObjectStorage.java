package com.petmate.backend.storage;

import java.time.Duration;

/**
 * Abstraction du stockage d'objets (MinIO / AWS S3). Le backend ne signe que
 * des URLs d'upload : il ne reçoit jamais les octets des images (upload direct
 * par le client).
 */
public interface ObjectStorage {

    /**
     * Génère une URL d'upload limitée dans le temps pour la clé donnée. La
     * signature verrouille {@code contentType} ET {@code contentLength} : un
     * upload avec une autre taille ou un autre type est rejeté par le serveur.
     */
    String presignPut(String key, String contentType, long contentLength, Duration validity);

    /**
     * URL publique permanente d'un objet (pas de signature : le bucket est en
     * lecture publique, les médias PawMate sont des contenus publics).
     */
    String publicUrl(String key);
}