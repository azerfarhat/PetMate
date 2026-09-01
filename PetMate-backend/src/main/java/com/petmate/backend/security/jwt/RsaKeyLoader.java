package com.petmate.backend.security.jwt;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Charge des clés RSA (RS256) à partir de leurs représentations PEM.
 * La clé privée doit être au format PKCS8 ("BEGIN PRIVATE KEY"),
 * la clé publique au format X509/SEC1 ("BEGIN PUBLIC KEY").
 */
final class RsaKeyLoader {

    private RsaKeyLoader() {
    }

    static PrivateKey loadPrivateKey(String pem) {
        try {
            byte[] der = decode(pem, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger la clé privée RSA (JWT_PRIVATE_KEY)", e);
        }
    }

    static PublicKey loadPublicKey(String pem) {
        try {
            byte[] der = decode(pem, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger la clé publique RSA (JWT_PUBLIC_KEY)", e);
        }
    }

    private static byte[] decode(String pem, String header) {
        String cleaned = pem
                .replace("-----BEGIN " + header + "-----", "")
                .replace("-----END " + header + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }
}