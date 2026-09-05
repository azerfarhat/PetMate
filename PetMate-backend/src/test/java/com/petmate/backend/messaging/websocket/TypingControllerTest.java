package com.petmate.backend.messaging.websocket;

import com.petmate.backend.messaging.MessageService;
import com.petmate.backend.messaging.dto.TypingSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Voie temps réel du signal "en train d'écrire" : un principal STOMP authentifié
 * déclenche le traitement métier (partagé avec la voie REST) ; une session
 * anonyme est ignorée.
 */
@ExtendWith(MockitoExtension.class)
class TypingControllerTest {

    private static final long CONVERSATION_ID = 100L;

    @Mock
    private MessageService messageService;

    @Test
    void typing_withAuthenticatedPrincipal_forwardsToService() {
        TypingController controller = new TypingController(messageService);

        controller.typing(new TypingSignal(CONVERSATION_ID, true), new StompPrincipal(1L));

        verify(messageService).typing(1L, CONVERSATION_ID, true);
    }

    @Test
    void typing_withAnonymousPrincipal_isIgnored() {
        TypingController controller = new TypingController(messageService);
        Principal anonymous = () -> "anonymous";

        controller.typing(new TypingSignal(CONVERSATION_ID, false), anonymous);

        verify(messageService, never()).typing(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }
}