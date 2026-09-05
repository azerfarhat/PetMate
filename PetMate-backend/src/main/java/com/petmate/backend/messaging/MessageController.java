package com.petmate.backend.messaging;

import com.petmate.backend.messaging.dto.ConversationResponse;
import com.petmate.backend.messaging.dto.MessagePageResponse;
import com.petmate.backend.messaging.dto.MessageResponse;
import com.petmate.backend.messaging.dto.SendMessageRequest;
import com.petmate.backend.messaging.dto.TypingRequest;
import com.petmate.backend.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de la messagerie (protégée par JWT). Une conversation n'existe que
 * pour les deux participants d'un match actif : l'identifiant est pris du
 * principal authentifié, les conversations des autres renvoient un 404.
 */
@RestController
@RequestMapping("/conversations")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Mes conversations, triées par dernière activité, avec le dernier message
     * et le nombre de non-lus.
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(messageService.listConversations(userId(authentication), limit, offset));
    }

    /**
     * Historique d'une conversation, paginé par curseur keyset. Sans
     * {@code beforeId}, la page la plus récente est retournée ; sinon les
     * messages strictement antérieurs au curseur. La taille est plafonnée à 50.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<MessagePageResponse> messages(
            Authentication authentication,
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(value = "beforeId", required = false) Long beforeId) {
        return ResponseEntity.ok(
                messageService.getMessagePage(userId(authentication), conversationId, limit, beforeId));
    }

    /**
     * Envoi d'un message : notification MESSAGE créée chez l'interlocuteur.
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(userId(authentication), conversationId, request));
    }

    /**
     * Signal "en train d'écrire", diffusé en temps réel à l'interlocuteur.
     * Transitoire et non persisté.
     */
    @PostMapping("/{conversationId}/typing")
    public ResponseEntity<Void> typing(Authentication authentication,
                                       @PathVariable("conversationId") Long conversationId,
                                       @Valid @RequestBody TypingRequest request) {
        messageService.typing(userId(authentication), conversationId, request.typing());
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque comme lus les messages de l'interlocuteur. Idempotent.
     */
    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Void> markRead(Authentication authentication,
                                         @PathVariable("conversationId") Long conversationId) {
        messageService.markRead(userId(authentication), conversationId);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}