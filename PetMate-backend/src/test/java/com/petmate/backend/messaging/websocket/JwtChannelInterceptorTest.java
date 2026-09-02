package com.petmate.backend.messaging.websocket;

import com.petmate.backend.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L'authentification WebSocket se fait au frame CONNECT : le JWT est extrait du
 * header Authorization, validé signature/expiration, puis attaché à la session
 * comme {@link StompPrincipal}. Un frame sans token ou invalide est rejeté
 * (connexion refusée).
 */
@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    private JwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtChannelInterceptor(jwtService);
    }

    @Test
    void connectWithValidToken_attachesStompPrincipal() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("42");
        when(jwtService.parseAccessToken("abc")).thenReturn(claims);

        Message<?> result = interceptor.preSend(connectMessage("Bearer abc"), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(accessor.getUser());
        assertInstanceOf(StompPrincipal.class, accessor.getUser());
        assertEquals(42L, ((StompPrincipal) accessor.getUser()).userId());
    }

    @Test
    void connectWithoutToken_rejectsConnection() {
        Message<?> result = interceptor.preSend(connectMessage(null), mock(MessageChannel.class));

        assertNull(result);
    }

    @Test
    void connectWithInvalidToken_rejectsConnection() {
        when(jwtService.parseAccessToken("bad")).thenThrow(new JwtException("token invalide"));

        Message<?> result = interceptor.preSend(connectMessage("bad"), mock(MessageChannel.class));

        assertNull(result);
    }

    @Test
    void nonConnectFrame_isLeftUntouched() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setNativeHeader("Authorization", "Bearer abc");
        Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(frame, mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor out = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(out.getUser());
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}