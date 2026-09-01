package com.pawmate.backend.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registre des Refresh Tokens émis et encore valides.
 *
 * Stockage mémoire (par instance) : la rotation et la révocation sont garanties
 * tant que l'instance tourne. Un stockage persistant (table, Redis) pourra
 * remplacer cette implémentation sans changer l'API.
 */
@Component
public class RefreshTokenStore {

    private final Map<String, StoredToken> tokens = new ConcurrentHashMap<>();

    public void store(String jti, Long userId, Instant expiresAt) {
        purgeExpired();
        tokens.put(jti, new StoredToken(jti, userId, expiresAt, false));
    }

    public boolean isValid(String jti) {
        StoredToken stored = tokens.get(jti);
        return stored != null
                && !stored.revoked()
                && stored.expiresAt().isAfter(Instant.now());
    }

    public void revoke(String jti) {
        purgeExpired();
        tokens.computeIfPresent(jti, (key, value) -> value.revoked() ? value
                : new StoredToken(value.jti(), value.userId(), value.expiresAt(), true));
    }

    public Optional<Long> findUserId(String jti) {
        StoredToken stored = tokens.get(jti);
        return stored == null ? Optional.empty() : Optional.ofNullable(stored.userId());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record StoredToken(String jti, Long userId, Instant expiresAt, boolean revoked) {
    }
}