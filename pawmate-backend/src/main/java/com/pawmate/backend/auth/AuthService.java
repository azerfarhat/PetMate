package com.pawmate.backend.auth;

import com.pawmate.backend.auth.dto.AuthResponse;
import com.pawmate.backend.auth.dto.ChangePasswordRequest;
import com.pawmate.backend.auth.dto.LoginRequest;
import com.pawmate.backend.auth.dto.LogoutRequest;
import com.pawmate.backend.auth.dto.MessageResponse;
import com.pawmate.backend.auth.dto.RefreshTokenRequest;
import com.pawmate.backend.entity.User;
import com.pawmate.backend.exception.RefreshTokenException;
import com.pawmate.backend.exception.TooManyLoginAttemptsException;
import com.pawmate.backend.repository.UserRepository;
import com.pawmate.backend.security.AuthUserPrincipal;
import com.pawmate.backend.security.LoginAttemptService;
import com.pawmate.backend.security.RefreshTokenStore;
import com.pawmate.backend.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestre le flux d'authentification : login, refresh (rotation) et logout.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenStore refreshTokenStore,
                       UserRepository userRepository,
                       LoginAttemptService loginAttemptService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        if (loginAttemptService.isLocked(request.getEmail())) {
            throw new TooManyLoginAttemptsException(
                    "Trop de tentatives échouées. Réessayez dans quelques minutes.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttemptService.registerFailure(request.getEmail());
            throw e;
        }

        loginAttemptService.reset(request.getEmail());

        User user = ((AuthUserPrincipal) authentication.getPrincipal()).getUser();
        return issueTokens(user, request.isRememberMe());
    }

    @Transactional
    public MessageResponse changePassword(AuthUserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new com.pawmate.backend.exception.UserNotFoundException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new MessageResponse("Mot de passe modifié avec succès");
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new RefreshTokenException("Refresh token invalide ou expiré");
        }

        String jti = claims.getId();
        if (!refreshTokenStore.isValid(jti)) {
            throw new RefreshTokenException("Refresh token révoqué ou plus valide");
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new RefreshTokenException("Refresh token invalide");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RefreshTokenException("Utilisateur introuvable"));
        if (!user.isActive()) {
            throw new RefreshTokenException("Compte utilisateur désactivé");
        }

        boolean rememberMe = Boolean.TRUE.equals(claims.get(JwtService.CLAIM_REMEMBER_ME, Boolean.class));

        // Rotation : invalide l'ancien Refresh Token avant d'en émettre un nouveau.
        refreshTokenStore.revoke(jti);

        return issueTokens(user, rememberMe);
    }

    public MessageResponse logout(LogoutRequest request) {
        // Idempotent : un token déjà invalide est simplement ignoré.
        try {
            Claims claims = jwtService.parseRefreshToken(request.getRefreshToken());
            refreshTokenStore.revoke(claims.getId());
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token déjà invalide ou expiré : le logout reste un succès.
        }
        return new MessageResponse("Déconnexion réussie");
    }

    private AuthResponse issueTokens(User user, boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        JwtService.RefreshToken refreshToken = jwtService.generateRefreshToken(user.getId(), rememberMe);
        refreshTokenStore.store(refreshToken.jti(), user.getId(), refreshToken.expiresAt());

        return AuthResponse.of(accessToken, refreshToken.token(), jwtService.getAccessTokenExpiration());
    }
}