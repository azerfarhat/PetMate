package com.pawmate.backend.auth;

import com.pawmate.backend.auth.dto.ForgotPasswordRequest;
import com.pawmate.backend.auth.dto.MessageResponse;
import com.pawmate.backend.auth.dto.ResetPasswordRequest;
import com.pawmate.backend.config.AppProperties;
import com.pawmate.backend.entity.PasswordResetToken;
import com.pawmate.backend.entity.User;
import com.pawmate.backend.exception.PasswordResetTokenException;
import com.pawmate.backend.exception.VerificationRequiredException;
import com.pawmate.backend.mail.EmailService;
import com.pawmate.backend.repository.PasswordResetTokenRepository;
import com.pawmate.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Flux de réinitialisation de mot de passe : envoi d'un email avec un jeton
 * à usage unique et réinitialisation effective du mot de passe.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                AppProperties appProperties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    @Transactional
    public MessageResponse requestReset(ForgotPasswordRequest request) {
        // Ne révélons pas si l'adresse existe : réponse identique dans les deux cas.
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                throw new VerificationRequiredException("Ce compte n'a pas encore vérifié son adresse email");
            }
            tokenRepository.invalidateActiveTokensForUser(user.getId());
            String token = generateToken();
            long expirationMinutes = appProperties.getPasswordReset().getTokenExpirationMinutes();
            tokenRepository.save(PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                    .used(false)
                    .build());
            emailService.sendPasswordResetEmail(user, token, expirationMinutes);
        });

        return new MessageResponse("Si un compte existe pour cet email, un lien de réinitialisation a été envoyé");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (request.token() == null || request.token().isBlank()) {
            throw new PasswordResetTokenException("Jeton de réinitialisation invalide");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new PasswordResetTokenException("Jeton de réinitialisation invalide"));

        if (resetToken.isUsed()) {
            throw new PasswordResetTokenException("Jeton de réinitialisation déjà utilisé");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PasswordResetTokenException("Jeton de réinitialisation expiré");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return new MessageResponse("Mot de passe réinitialisé avec succès");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
