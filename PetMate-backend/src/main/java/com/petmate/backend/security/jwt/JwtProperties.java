package com.petmate.backend.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés JWT provenant des variables d'environnement (.env / Docker).
 * Les clés privées ne sont JAMAIS en dur dans le code.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    private String privateKey;

    @NotBlank
    private String publicKey;

    @Positive
    private long accessTokenExpiration;

    @Positive
    private long refreshTokenExpiration;

    @Positive
    private long refreshTokenRememberMeExpiration;
}