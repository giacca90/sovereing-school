package com.sovereingschool.back_chat.Configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;

import com.sovereingschool.back_common.Utils.JwtUtil;

class WebSocketAuthInterceptorTest {

    private JwtUtil jwtUtil;
    private ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private WebSocketAuthInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        messagingTemplateProvider = mock(ObjectProvider.class);
        interceptor = new WebSocketAuthInterceptor(jwtUtil, messagingTemplateProvider);
        channel = mock(MessageChannel.class);
    }

    @Test
    void preSend_ConnectCommandWithToken_SetsUser() {
        String token = "validToken";
        Long userId = 1L;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("token", token);
        accessor.setSessionAttributes(sessionAttributes);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Authentication auth = mock(Authentication.class);
        when(jwtUtil.getIdUsuario(token)).thenReturn(userId);
        when(jwtUtil.createAuthenticationFromToken(token)).thenReturn(auth);

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals(userId.toString(), resultAccessor.getUser().getName());
    }

    @Test
    void preSend_ConnectCommandWithoutToken_ThrowsMessagingException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessagingException.class, () -> {
            interceptor.preSend(message, channel);
        });
    }
}
