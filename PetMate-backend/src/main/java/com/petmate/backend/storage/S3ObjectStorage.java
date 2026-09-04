package com.petmate.backend.storage;

import com.petmate.backend.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * Stockage MinIO (ou AWS S3) via le SDK AWS standard : il suffit de changer
 * {@code endpoint} / {@code publicEndpoint} pour basculer de MinIO vers AWS.
 *
 * <p>Deux clients distincts :</p>
 * <ul>
 *   <li>{@code S3Client} → administration du bucket (création, politique) sur
 *       l'endpoint interne du réseau docker ;</li>
 *   <li>{@code S3Presigner} → signature des URLs d'upload avec l'endpoint
 *       PUBLIC (dans le réseau docker, le client ne connaît que {@code localhost}).</li>
 * </ul>
 *
 * <p>Style de chemin (path-style) obligatoire pour MinIO. L'initialisation
 * (bucket + politique de lecture publique) est automatique et ré-essayée au
 * démarrage jusqu'à ce que MinIO soit prêt.</p>
 */
@Component
public class S3ObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);

    /** Région exigée par la signature AWS v4 ; MinIO l'ignore en pratique. */
    private static final Region REGION = Region.US_EAST_1;
    private static final int MAX_INIT_ATTEMPTS = 10;
    private static final long RETRY_DELAY_MS = 3_000;

    private final StorageProperties properties;
    private S3Client s3Client;
    private S3Presigner presigner;

    public S3ObjectStorage(StorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Préparation du bucket au démarrage : création si absent + politique de
     * lecture publique pour les médias. Ré-essayé si MinIO n'est pas encore prêt.
     */
    @PostConstruct
    void initialize() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));

        this.s3Client = S3Client.builder()
                .region(REGION)
                .credentialsProvider(credentials)
                .endpointOverride(URI.create(properties.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        this.presigner = S3Presigner.builder()
                .region(REGION)
                .credentialsProvider(credentials)
                .endpointOverride(URI.create(properties.getPublicEndpoint()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        for (int attempt = 1; attempt <= MAX_INIT_ATTEMPTS; attempt++) {
            try {
                ensureBucketExists();
                applyPublicReadPolicy();
                log.info("Stockage MinIO prêt : bucket '{}' sur {}", bucket(), properties.getEndpoint());
                return;
            } catch (Exception ex) {
                if (attempt == MAX_INIT_ATTEMPTS) {
                    log.error("Impossible d'initialiser le stockage MinIO après {} essais : {}",
                            MAX_INIT_ATTEMPTS, ex.getMessage(), ex);
                } else {
                    log.warn("Stockage MinIO pas encore prêt (essai {}/{}), nouvelle tentative dans {}s : {}",
                            attempt, MAX_INIT_ATTEMPTS, RETRY_DELAY_MS / 1000, ex.getMessage());
                }
                sleep();
            }
        }
    }

    @Override
    public String presignPut(String key, String contentType, long contentLength, Duration validity) {
        return presigner.presignPutObject(request -> request
                        .signatureDuration(validity)
                        .putObjectRequest(builder -> builder
                                .bucket(bucket())
                                .key(key)
                                .contentType(contentType)
                                .contentLength(contentLength)))
                .url()
                .toString();
    }

    @Override
    public String publicUrl(String key) {
        String base = properties.getPublicEndpoint().replaceAll("/+$", "");
        return base + "/" + bucket() + "/" + key;
    }

    private void ensureBucketExists() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket()).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            // Bucket déjà présent, ce n'est pas une erreur.
        }
    }

    private void applyPublicReadPolicy() {
        String policy = """
                {"Version":"2012-10-17",
                 "Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},
                   "Action":["s3:GetObject"],
                   "Resource":["arn:aws:s3:::%s/*"]}]}
                """.formatted(bucket());
        s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucket())
                .policy(policy)
                .build());
    }

    private String bucket() {
        return properties.getBucket();
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}