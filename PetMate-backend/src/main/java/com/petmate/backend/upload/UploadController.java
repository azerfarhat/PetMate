package com.petmate.backend.upload;

import com.petmate.backend.security.AuthUserPrincipal;
import com.petmate.backend.upload.dto.PresignUploadRequest;
import com.petmate.backend.upload.dto.PresignUploadResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST d'upload direct des médias (protégée par JWT). Le backend signe des
 * URLs éphémères, les fichiers sont uploadés directement vers MinIO par l'app.
 * L'identité est prise du principal authentifié (pas d'IDOR).
 */
@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Demande une URL d'upload pré-signée pour une Pet possédée ou son profil.
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignUploadResponse> presignedUrl(
            Authentication authentication,
            @Valid @RequestBody PresignUploadRequest request) {
        return ResponseEntity.ok(uploadService.presignUpload(userId(authentication), request));
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}