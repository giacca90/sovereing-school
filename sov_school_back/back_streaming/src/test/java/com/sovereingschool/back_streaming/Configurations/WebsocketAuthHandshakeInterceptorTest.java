package com.sovereingschool.back_streaming.Configurations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;

import com.sovereingschool.back_common.Utils.JwtUtil;

/**
 * Tests para validar el interceptor de autenticación de WebSocket.
 * 
 * Verifica que el handshake de WebSocket extraiga correctamente los tokens
 * JWT y cree el contexto de autenticación.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebsocketAuthHandshakeInterceptor - Autenticación en Handshake")
class WebsocketAuthHandshakeInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private WebsocketAuthHandshakeInterceptor interceptor;

    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        interceptor = new WebsocketAuthHandshakeInterceptor(jwtUtil);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
    }

    @Test
    @DisplayName("debe permitir handshake cuando hay token válido")
    void shouldAllowHandshakeWithValidToken() throws Exception {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "testuser";
        Long userId = 1L;
        Authentication auth = mock(Authentication.class);

        when(request.getURI()).thenReturn(new URI("ws://localhost:8080/ws?token=" + validToken));
        when(jwtUtil.createAuthenticationFromToken(validToken)).thenReturn(auth);
        when(jwtUtil.getUsername(validToken)).thenReturn(username);
        when(jwtUtil.getIdUsuario(validToken)).thenReturn(userId);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result, "El handshake debe ser permitido con token válido");
        assertEquals(auth, attributes.get("Auth"), "La autenticación debe guardarse en atributos");
        assertEquals(username, attributes.get("username"), "El nombre de usuario debe guardarse");
        assertEquals(userId, attributes.get("idUsuario"), "El ID de usuario debe guardarse");
    }

    @Test
    @DisplayName("debe seguir permitiendo handshake cuando no hay token")
    void shouldAllowHandshakeWithoutToken() throws Exception {
        // Arrange
        when(request.getURI()).thenReturn(new URI("ws://localhost:8080/ws"));

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result, "El handshake debe ser permitido incluso sin token (será validado después)");
        assertTrue(attributes.containsKey("Error"), "Debe guardarse un mensaje de error cuando no hay token");
    }

    @Test
    @DisplayName("debe guardar error cuando el token está vacío")
    void shouldStoreErrorWhenTokenIsEmpty() throws Exception {
        // Arrange
        when(request.getURI()).thenReturn(new URI("ws://localhost:8080/ws?token="));

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result, "El handshake debe ser permitido");
        assertTrue(attributes.containsKey("Error"), "Debe guardarse un error cuando el token está vacío");
    }

    @Test
    @DisplayName("debe manejar excepciones durante la validación de token")
    void shouldHandleExceptionDuringTokenValidation() throws Exception {
        // Arrange
        String invalidToken = "invalid.token";
        when(request.getURI()).thenReturn(new URI("ws://localhost:8080/ws?token=" + invalidToken));
        when(jwtUtil.createAuthenticationFromToken(invalidToken))
                .thenThrow(new IllegalArgumentException("Token inválido"));

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result, "El handshake debe ser permitido incluso con token inválido");
        assertTrue(attributes.containsKey("Error"), "Debe guardarse un error cuando hay excepción");
    }

    @Test
    @DisplayName("debe ejecutar afterHandshake sin efectos secundarios")
    void shouldExecuteAfterHandshakeWithoutSideEffects() {
        // El método afterHandshake no hace nada, solo comprobamos que se ejecute sin
        // errores
        assertDoesNotThrow(() -> interceptor.afterHandshake(request, response, wsHandler, null),
                "afterHandshake no debe lanzar excepciones");
    }
}
