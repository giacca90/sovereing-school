package com.sovereingschool.back_streaming.Configurations;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.sovereingschool.back_streaming.Controllers.OBSWebSocketHandler;
import com.sovereingschool.back_streaming.Controllers.WebRTCSignalingHandler;
import com.sovereingschool.back_streaming.Services.StreamingService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private StreamingService streamingService;

    @Autowired
    private WebsocketAuthHandshakeInterceptor authHandshakeInterceptor;

    @Value("${variable.RTMP}")
    private String RTMP_URL;

    // Executor para tareas de ping-pong
    private final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        // Registrar el handler para la webcam
        WebRTCSignalingHandler handler = new WebRTCSignalingHandler(webSocketTaskExecutor(), streamingService);
        registry.addHandler(handler, "/live-webcam")
                .setAllowedOrigins("*")// Cambiar "*" por dominios específicos en producción
                .addInterceptors(authHandshakeInterceptor);
        // Registrar el handler para OBS
        registry.addHandler(new OBSWebSocketHandler(webSocketTaskExecutor(), streamingService, RTMP_URL + "/live/"),
                "/live-obs")
                .setAllowedOrigins("*") // Cambia "*" a los dominios permitidos en producción
                .addInterceptors(authHandshakeInterceptor);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        // Incrementar tamaño del buffer para manejar mensajes grandes
        container.setMaxTextMessageBufferSize(1024 * 1024); // 512 KB para mensajes de texto
        container.setMaxBinaryMessageBufferSize(1024 * 1024); // 512 KB para mensajes binarios

        // Configurar tiempo de espera y heartbeats
        container.setAsyncSendTimeout(30_000L); // 30 segundos para enviar mensajes asíncronos
        container.setMaxSessionIdleTimeout(3_600_000L); // 1 hora para sesiones inactivas

        return container;
    }

    @Bean(name = "webSocketTaskExecutor")
    public Executor webSocketTaskExecutor() {
        // Crear un pool de hilos para manejar mensajes en paralelo
        return Executors.newFixedThreadPool(50);
    }

    @Bean
    public ScheduledExecutorService pingScheduler() {
        return pingScheduler;
    }

    /**
     * Método para iniciar el ping-pong.
     * Envía un `PING` cada 10 segundos y verifica que se reciba un `PONG`.
     * Si no se recibe el `PONG`, la conexión se cerrará.
     */
    public void startPingPong(WebSocketSession session) {
        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    // Enviar mensaje de Ping
                    session.sendMessage(new PingMessage());
                } else {
                    pingScheduler.shutdown();
                }
            } catch (Exception e) {
                System.err.println("Error enviando PING: " + e.getMessage());
                try {
                    session.close();
                } catch (Exception closeEx) {
                    System.err.println("Error cerrando sesión: " + closeEx.getMessage());
                }
            }
        }, 0, 10, TimeUnit.SECONDS); // Intervalo de 10 segundos
    }
}
