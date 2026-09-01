package com.petmate.backend.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Protection basique contre la force brute sur le login : après un certain
 * nombre d'échecs consécutifs pour un même email, les tentatives sont
 * temporairement bloquées. Implémentation en mémoire (par instance).
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 300; // 5 min

    private final Map<String, Attempt> attemptsByEmail = new ConcurrentHashMap<>();

    /**
     * Enregistre un échec pour {@code email}. Renvoie vrai si le compte est
     * désormais verrouillé à cause de trop d'échecs.
     */
    public void registerFailure(String email) {
        if (email == null) {
            return;
        }
        String key = email.toLowerCase();
        long now = Instant.now().getEpochSecond();
        Attempt current = attemptsByEmail.getOrDefault(key, new Attempt(0, now));

        if (isLocked(key, now, current)) {
            return;
        }

        int count = current.failedCount() + 1;
        long windowStart = current.firstFailureAt();
        if (count == 1) {
            windowStart = now;
        }
        attemptsByEmail.put(key, new Attempt(count, windowStart));
    }

    /**
     * Renvoie vrai si {@code email} est actuellement bloqué.
     */
    public boolean isLocked(String email) {
        if (email == null) {
            return false;
        }
        String key = email.toLowerCase();
        long now = Instant.now().getEpochSecond();
        Attempt current = attemptsByEmail.get(key);
        return isLocked(key, now, current);
    }

    public void reset(String email) {
        if (email != null) {
            attemptsByEmail.remove(email.toLowerCase());
        }
    }

    private boolean isLocked(String key, long now, Attempt current) {
        if (current == null) {
            return false;
        }
        long lockUntil = current.firstFailureAt() + LOCK_DURATION_SECONDS;
        if (now < lockUntil && current.failedCount() >= MAX_ATTEMPTS) {
            return true;
        }
        if (now >= lockUntil) {
            attemptsByEmail.remove(key);
        }
        return false;
    }

    private record Attempt(int failedCount, long firstFailureAt) {
    }
}
