package com.petmate.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration du stockage d'objets (MinIO en dev, AWS S3 compatible en prod).
 * <ul>
 *   <li>{@code endpoint} : adresse vue par le BACKEND (réseau docker, ex. http://minio:9000).</li>
 *   <li>{@code publicEndpoint} : adresse vue par les CLIENTS (ex. http://localhost:9000).
 *       C'est celle utilisée pour signer les URLs d'upload et construire les URLs publiques.</li>
 * </ul>
 * Les secrets passent toujours par les variables d'environnement, jamais en dur.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Endpoint interne (réseau docker) : ex. http://minio:9000 */
    private String endpoint = "http://localhost:9000";

    /** Endpoint public que les clients (app) doivent joindre pour uploader/lire. */
    private String publicEndpoint = "http://localhost:9000";

    private String accessKey;
    private String secretKey;

    /** Bucket dédié aux médias publics PawMate. */
    private String bucket = "pawmate-media";

    /** Durée de validité d'une pre-signed URL d'upload, en minutes. */
    private long presignExpirationMinutes = 5;

    /** Taille maximale autorisée d'un fichier uploadé, en octets. */
    private long maxFileBytes = 5L * 1024 * 1024;

    /** Types MIME acceptés (restriction stricte côté serveur). */
    private Set<String> allowedContentTypes = new HashSet<>(Set.of(
            "image/jpeg", "image/png", "image/webp"));
}