package com.petmate.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import com.petmate.backend.enums.UserRole;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service central de création et de validation des JWT (algorithme RS256).
 */
@Service
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_REMEMBER_ME = "remember_me";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.privateKey = RsaKeyLoader.loadPrivateKey(properties.getPrivateKey());
        this.publicKey = RsaKeyLoader.loadPublicKey(properties.getPublicKey());
    }

    public String generateAccessToken(Long userId, UserRole role) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(properties.getAccessTokenExpiration());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey)
                .compact();
    }

    /**
     * Génère un Refresh Token avec rotation implicite (nouveau jti à chaque appel).
     */
    public RefreshToken generateRefreshToken(Long userId, boolean rememberMe) {
        Instant now = Instant.now();
        long lifetime = rememberMe
                ? properties.getRefreshTokenRememberMeExpiration()
                : properties.getRefreshTokenExpiration();
        Instant expiration = now.plusSeconds(lifetime);

        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_REMEMBER_ME, rememberMe)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey)
                .compact();

        return new RefreshToken(jti, token, expiration, rememberMe);
    }

    /**
     * Vérifie la signature (RS256), la structure, l'expiration et le type.
     */
    private Jws<Claims> parse(String token, String expectedType) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token);
        Claims claims = jws.getPayload();
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new IllegalArgumentException("Type de JWT inattendu");
        }
        return jws;
    }

    public Claims parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS).getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH).getPayload();
    }

    public long getAccessTokenExpiration() {
        return properties.getAccessTokenExpiration();
    }

    public record RefreshToken(String jti, String token, Instant expiresAt, boolean rememberMe) {
    }
}