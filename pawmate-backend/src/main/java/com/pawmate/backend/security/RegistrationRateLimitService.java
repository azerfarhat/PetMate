package com.pawmate.backend.security;

import com.pawmate.backend.config.AppProperties;
import com.pawmate.backend.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de créations de compte par adresse IP sur une fenêtre
 * glissante, afin d'empêcher le spam d'inscriptions (fake accounts).
 * Implémentation en mémoire (par instance). Si le client passe derrière un
 * reverse proxy, le contrôleur doit fournir l'adresse IP réelle (X-Forwarded-For).
 */
@Component
public class RegistrationRateLimitService {

    private final int maxPerWindow;
    private final long windowSeconds;

    private final Map<String, Deque<Long>> registrationsByIp = new ConcurrentHashMap<>();

    public RegistrationRateLimitService(AppProperties appProperties) {
        this.maxPerWindow = appProperties.getRateLimit().getRegisterMaxPerWindow();
        this.windowSeconds = appProperties.getRateLimit().getRegisterWindowMinutes() * 60L;
    }

    /**
     * Vérifie que {@code clientIp} n'a pas dépassé la limite d'inscriptions.
     * Lève {@link RateLimitExceededException} dans le cas contraire, sinon
     * enregistre cette nouvelle tentative.
     */
    public void checkAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }

        long now = Instant.now().getEpochSecond();
        long cutoff = now - windowSeconds;

        Deque<Long> timestamps = registrationsByIp.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxPerWindow) {
                throw new RateLimitExceededException(
                        "Trop de comptes créés depuis cette adresse. Réessayez plus tard.");
            }
            timestamps.addLast(now);
        }
    }
}
