package com.petmate.backend.messaging.websocket;

import com.petmate.backend.messaging.MessageService;
import com.petmate.backend.messaging.dto.TypingSignal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Entrées temps réel de la messagerie via STOMP/{@code /app}.
 *
 * <p>Le client authentifie sa session au frame CONNECT ({@link
 * JwtChannelInterceptor}) ; l'identité est alors portée par le {@link
 * Principal} de la session. Le traitement métier est partagé avec la voie REST
 * (mêmes règles d'accès et de blocage), seule la diffusion cible l'interlocuteur.
 */
@Controller
public class TypingController {

    private final MessageService messageService;

    public TypingController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Signal "en train d'écrire" reçu sur {@code /app/typing} ; rebondi sur la
     * file personnelle de l'interlocuteur. Ignoré pour une session anonyme.
     */
    @MessageMapping("/typing")
    public void typing(@Payload TypingSignal signal, Principal principal) {
        if (principal instanceof StompPrincipal stomp) {
            messageService.typing(stomp.userId(), signal.conversationId(), signal.typing());
        }
    }
}