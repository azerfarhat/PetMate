package com.petmate.backend.match;

import com.petmate.backend.match.dto.MatchResponse;
import com.petmate.backend.match.dto.SwipeCandidateResponse;
import com.petmate.backend.match.dto.SwipeRequest;
import com.petmate.backend.match.dto.SwipeResponse;
import com.petmate.backend.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST du matching pet-to-pet (protégée par JWT). L'identifiant de
 * l'utilisateur est toujours pris du principal authentifié, jamais de l'URL.
 */
@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /**
     * Feed de découverte : une carte par owner, paginée (une seule Pet par
     * owner, photo principale en premier plan).
     */
    @GetMapping("/candidates")
    public ResponseEntity<List<SwipeCandidateResponse>> candidates(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(matchService.candidates(userId(authentication), limit, offset));
    }

    /**
     * LIKE / PASS sur la Pet d'un autre owner. Le LIKE croisé déclenche le
     * match : conversation ouverte + notifications pour les deux owners.
     */
    @PostMapping("/swipe")
    public ResponseEntity<SwipeResponse> swipe(Authentication authentication,
                                               @Valid @RequestBody SwipeRequest request) {
        return ResponseEntity.ok(matchService.swipe(userId(authentication), request));
    }

    /**
     * Mes matchs actifs (statut MATCHED) avec les deux Pet et la conversation.
     */
    @GetMapping
    public ResponseEntity<List<MatchResponse>> myMatches(Authentication authentication) {
        return ResponseEntity.ok(matchService.myMatches(userId(authentication)));
    }

    /**
     * Suppression du match : messages + conversation supprimés, match passé au
     * statut UNMATCHED. Seul un participant peut le faire.
     */
    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> unmatch(Authentication authentication,
                                        @PathVariable("matchId") Long matchId) {
        matchService.unmatch(userId(authentication), matchId);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}