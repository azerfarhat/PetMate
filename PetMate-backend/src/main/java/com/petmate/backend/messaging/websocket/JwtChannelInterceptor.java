package com.petmate.backend.messaging.websocket;

import com.petmate.backend.security.jwt.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * Authentifie chaque connexion STOMP avec le JWT du client (compatible
 * clients mobiles : le jeton est transmis dans un header du frame CONNECT, et
 * non dans l'en-tête HTTP du handshake). Un token absent ou invalide rejette
 * la connexion.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        Principal principal = authenticate(accessor.getNativeHeader(AUTH_HEADER));
        if (principal == null) {
            return null;
        }
        accessor.setUser(principal);
        return message;
    }

    private Principal authenticate(List<String> authHeaders) {
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String header = authHeaders.get(0);
        String token = header.startsWith(BEARER_PREFIX) ? header.substring(BEARER_PREFIX.length()) : header;
        try {
            long userId = Long.parseLong(jwtService.parseAccessToken(token).getSubject());
            return new StompPrincipal(userId);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}