package com.petmate.backend.user;

import com.petmate.backend.auth.dto.MessageResponse;
import com.petmate.backend.security.AuthUserPrincipal;
import com.petmate.backend.user.dto.PasswordChangeRequest;
import com.petmate.backend.user.dto.UpdateProfileRequest;
import com.petmate.backend.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST du profil de l'utilisateur connecté (protégée par JWT). L'identifiant
 * est toujours pris du principal authentifié, jamais de l'URL (pas d'IDOR).
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.me(userId(authentication)));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId(authentication), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        userService.deleteAccount(userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/password/request")
    public ResponseEntity<MessageResponse> requestPasswordChange(Authentication authentication) {
        return ResponseEntity.ok(userService.requestPasswordChange(userId(authentication)));
    }

    @PostMapping("/me/password")
    public ResponseEntity<MessageResponse> confirmPasswordChange(Authentication authentication,
                                                                 @Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(userService.confirmPasswordChange(userId(authentication), request));
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}