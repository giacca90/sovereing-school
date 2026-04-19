package com.sovereingschool.back_chat.Configurations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

class WebSocketAuthHandshakeInterceptorTest {

    private WebSocketAuthHandshakeInterceptor interceptor;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new WebSocketAuthHandshakeInterceptor();
    }

    @Test
    void beforeHandshake_WithToken_AddsTokenToAttributes() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        URI uri = URI.create("ws://localhost:8080/chat?token=validToken123");
        when(request.getURI()).thenReturn(uri);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertEquals("validToken123", attributes.get("token"));
    }

    @Test
    void beforeHandshake_WithoutToken_DoesNotAddTokenToAttributes() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        URI uri = URI.create("ws://localhost:8080/chat");
        when(request.getURI()).thenReturn(uri);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertNull(attributes.get("token"));
    }

    @Test
    void beforeHandshake_WithEmptyQuery_DoesNotAddTokenToAttributes() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        URI uri = URI.create("ws://localhost:8080/chat?");
        when(request.getURI()).thenReturn(uri);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertNull(attributes.get("token"));
    }

    @Test
    void beforeHandshake_WithOtherQueryParam_DoesNotAddTokenToAttributes() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        URI uri = URI.create("ws://localhost:8080/chat?user=test");
        when(request.getURI()).thenReturn(uri);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertNull(attributes.get("token"));
    }

    @Test
    void afterHandshake_DoesNothing() {
        // Act & Assert
        assertDoesNotThrow(
                () -> interceptor.afterHandshake(request, response, wsHandler, new Exception("Test exception")));
    }
}
