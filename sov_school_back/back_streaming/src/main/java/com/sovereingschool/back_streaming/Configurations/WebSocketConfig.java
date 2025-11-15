package com.sovereingschool.back_streaming.Configurations;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.sovereingschool.back_streaming.Controllers.OBSWebSocketHandler;
import com.sovereingschool.back_streaming.Controllers.WebRTCSignalingHandler;
import com.sovereingschool.back_streaming.Services.StreamingService;

import jakarta.servlet.ServletContext;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StreamingService streamingService;
    private final WebsocketAuthHandshakeInterceptor authHandshakeInterceptor;
    private final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private String RTMP_URL;

    private String RTMP_DOCKER;

    private String uploadDir;

    private final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1);

    public WebSocketConfig(
            @Value("${variable.RTMP}") String RTMP_URL,
            @Value("${variable.RTMP_DOCKER}") String RTMP_DOCKER,
            @Value("${variable.VIDEOS_DIR}") String uploadDir,
            StreamingService streamingService,
            WebsocketAuthHandshakeInterceptor authHandshakeInterceptor) {
        this.RTMP_URL = RTMP_URL;
        this.RTMP_DOCKER = RTMP_DOCKER;
        this.uploadDir = uploadDir;
        this.streamingService = streamingService;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        WebRTCSignalingHandler handler = new WebRTCSignalingHandler(webSocketTaskExecutor(), streamingService);
        registry.addHandler(handler, "/live-webcam")
                .setAllowedOrigins("*")
                .addInterceptors(authHandshakeInterceptor);

        registry.addHandler(
                new OBSWebSocketHandler(webSocketTaskExecutor(), streamingService,
                        RTMP_URL + "/live/", RTMP_DOCKER + "/live/", uploadDir),
                "/live-obs")
                .setAllowedOrigins("*")
                .addInterceptors(authHandshakeInterceptor);
    }

    /**
     * Solo se crea si hay un ServletContext disponible (en ejecución real, no en
     * tests).
     */
    @Bean
    @ConditionalOnClass(ServletContext.class)
    public ServletServerContainerFactoryBean createWebSocketContainer(ServletContext servletContext) {
        if (servletContext.getAttribute("jakarta.websocket.server.ServerContainer") == null) {
            // No hay contenedor web real (probablemente un test)
            logger.warn(
                    "No se encontró ServerContainer, omitiendo configuración WebSocketContainer para entorno de test.");
            return null;
        }

        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        container.setAsyncSendTimeout(30_000L);
        container.setMaxSessionIdleTimeout(3_600_000L);
        return container;
    }

    @Bean(name = "webSocketTaskExecutor")
    public Executor webSocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("WS-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ScheduledExecutorService pingScheduler() {
        return pingScheduler;
    }

    public void startPingPong(WebSocketSession session) {
        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new PingMessage());
                } else {
                    pingScheduler.shutdown();
                }
            } catch (Exception e) {
                logger.error("Error enviando PING: {}", e.getMessage());
                try {
                    session.close();
                } catch (Exception closeEx) {
                    logger.error("Error cerrando sesión: {}", closeEx.getMessage());
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
}
