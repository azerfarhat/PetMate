package com.petmate.backend.block;

import com.petmate.backend.block.dto.BlockedUserResponse;
import com.petmate.backend.security.AuthUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST des blocages (protégée par JWT). L'identifiant est toujours pris du
 * principal authentifié : un utilisateur ne peut gérer que ses propres blocages
 * (pas d'IDOR).
 */
@RestController
@RequestMapping("/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    /**
     * Mes utilisateurs bloqués, les plus récents d'abord.
     */
    @GetMapping
    public ResponseEntity<List<BlockedUserResponse>> listBlocked(Authentication authentication) {
        return ResponseEntity.ok(blockService.blockedUsers(userId(authentication)));
    }

    /**
     * Bloque un utilisateur. Idempotent : bloquer deux fois ne crée pas de doublon.
     */
    @PostMapping("/{userId}")
    public ResponseEntity<BlockedUserResponse> block(
            Authentication authentication,
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(blockService.block(userId(authentication), userId));
    }

    /**
     * Lève le blocage. Idempotent : débocaquer quelqu'un qu'on ne bloque pas
     * ne crée pas d'erreur.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unblock(
            Authentication authentication,
            @PathVariable("userId") Long userId) {
        blockService.unblock(userId(authentication), userId);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}