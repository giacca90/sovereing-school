package com.sovereingschool.back_streaming.Configurations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.sovereingschool.back_streaming.Services.StreamingService;

/**
 * Tests para validar la configuración de WebSocket.
 * 
 * Verifica que los handlers de WebSocket se registren correctamente,
 * el executor de tareas se configure y el scheduler de ping esté disponible.
 */
@SpringBootTest
@DisplayName("WebSocketConfig - Configuración de WebSocket")
class WebSocketConfigTest {

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Autowired
    @Qualifier("webSocketTaskExecutor")
    private Executor webSocketTaskExecutor;

    @Autowired
    private ScheduledExecutorService pingScheduler;

    @MockitoBean
    private StreamingService streamingService;

    @MockitoBean
    private WebsocketAuthHandshakeInterceptor authHandshakeInterceptor;

    @Test
    @DisplayName("debe crear la configuración de WebSocket correctamente")
    void shouldCreateWebSocketConfigBean() {
        assertNotNull(webSocketConfig, "WebSocketConfig no debe ser null");
    }

    @Test
    @DisplayName("debe crear el executor de WebSocket correctamente")
    void shouldCreateWebSocketTaskExecutor() {
        assertNotNull(webSocketTaskExecutor, "El webSocketTaskExecutor no debe ser null");
    }

    @Test
    @DisplayName("debe ser una instancia de ThreadPoolTaskExecutor")
    void shouldBeThreadPoolTaskExecutor() {
        assertInstanceOf(ThreadPoolTaskExecutor.class, webSocketTaskExecutor,
                "El executor debe ser instancia de ThreadPoolTaskExecutor");
    }

    @Test
    @DisplayName("debe configurar corePoolSize en 20")
    void shouldHaveCorePoolSize20() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) webSocketTaskExecutor;
        assertEquals(20, executor.getCorePoolSize(),
                "El corePoolSize debe ser 20 para WebSocket");
    }

    @Test
    @DisplayName("debe configurar maxPoolSize en 50")
    void shouldHaveMaxPoolSize50() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) webSocketTaskExecutor;
        assertEquals(50, executor.getMaxPoolSize(),
                "El maxPoolSize debe ser 50 para WebSocket");
    }

    @Test
    @DisplayName("debe configurar threadNamePrefix como 'WS-'")
    void shouldHaveCorrectWebSocketThreadNamePrefix() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) webSocketTaskExecutor;
        assertEquals("WS-", executor.getThreadNamePrefix(),
                "El prefijo de nombre de hilo debe ser 'WS-'");
    }

    @Test
    @DisplayName("debe crear el pingScheduler correctamente")
    void shouldCreatePingScheduler() {
        assertNotNull(pingScheduler, "El pingScheduler no debe ser null");
    }

    @Test
    @DisplayName("debe registrar handlers de WebSocket")
    void shouldRegisterWebSocketHandlers() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration registration = mock(
                org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration.class);

        when(registry.addHandler(any(), anyString())).thenReturn(registration);
        when(registration.setAllowedOrigins(any())).thenReturn(registration);
        when(registration.addInterceptors(any())).thenReturn(registration);

        assertDoesNotThrow(() -> webSocketConfig.registerWebSocketHandlers(registry),
                "El registro de handlers no debe lanzar excepciones");

        verify(registry, atLeastOnce()).addHandler(any(), anyString());
    }
}
