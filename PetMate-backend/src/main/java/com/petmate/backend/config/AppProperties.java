package com.petmate.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés applicatives PawMate (email, vérification de compte) chargées
 * depuis les variables d'environnement (.env / Docker).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Mail mail = new Mail();
    private Verification verification = new Verification();
    private PasswordReset passwordReset = new PasswordReset();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Mail {
        /** Expéditeur des emails transactionnels, jamais hardcodé. */
        private String from = "noreply@pawmate.app";
    }

    @Getter
    @Setter
    public static class Verification {
        /** Durée de validité d'un jeton de vérification, en minutes. */
        private long tokenExpirationMinutes = 60;
        /** URL de base publique (schéma + hôte) pour construire le lien de vérification. */
        private String baseUrl = "http://localhost:8080";
        /** Schéma de deep-link mobile (ex. "pawmate"). Vide = on garde le lien web classique. */
        private String deepLinkScheme = "";
        /** Hôte/host du deep-link mobile utilisé avec {@link #getDeepLinkScheme()} (ex. "verify"). */
        private String deepLinkHost = "verify";
    }

    @Getter
    @Setter
    public static class PasswordReset {
        /** Durée de validité d'un jeton de réinitialisation de mot de passe, en minutes. */
        private long tokenExpirationMinutes = 30;
    }

    @Getter
    @Setter
    public static class RateLimit {
        /** Nombre maximal de créations de compte autorisé par IP sur la fenêtre glissante. */
        private int registerMaxPerWindow = 5;
        /** Durée de la fenêtre glissante du rate-limit d'inscription, en minutes. */
        private long registerWindowMinutes = 60;
    }
}