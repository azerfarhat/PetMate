package com.petmate.backend.auth;

import com.petmate.backend.auth.dto.AuthResponse;
import com.petmate.backend.auth.dto.ForgotPasswordRequest;
import com.petmate.backend.auth.dto.LoginRequest;
import com.petmate.backend.auth.dto.LogoutRequest;
import com.petmate.backend.auth.dto.MessageResponse;
import com.petmate.backend.auth.dto.RefreshTokenRequest;
import com.petmate.backend.auth.dto.RegisterRequest;
import com.petmate.backend.auth.dto.ResendVerificationRequest;
import com.petmate.backend.auth.dto.ResetPasswordRequest;
import com.petmate.backend.auth.dto.VerifyCodeRequest;
import com.petmate.backend.security.RegistrationRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST d'authentification, indépendante de toute technologie web
 * (compatible Android, iOS, Flutter, clients natifs).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final RegistrationRateLimitService registrationRateLimitService;

    public AuthController(AuthService authService,
                          RegistrationService registrationService,
                          PasswordResetService passwordResetService,
                          RegistrationRateLimitService registrationRateLimitService) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
        this.registrationRateLimitService = registrationRateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request,
                                                    HttpServletRequest httpRequest) {
        registrationRateLimitService.checkAllowed(resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(registrationService.verifyEmail(token));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<MessageResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(registrationService.verifyByCode(request.code()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(registrationService.resendVerification(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestReset(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}