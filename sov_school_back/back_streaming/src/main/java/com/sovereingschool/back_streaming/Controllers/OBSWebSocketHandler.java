package com.sovereingschool.back_streaming.Controllers;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_streaming.Services.StreamingService;

public class OBSWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Thread> ffmpegThreads = new ConcurrentHashMap<>();
    private final Map<String, Thread> previews = new ConcurrentHashMap<>();
    private final StreamingService streamingService;
    private final Executor executor;
    private final String RTMP_URL;

    public OBSWebSocketHandler(Executor executor, StreamingService streamingService, String RTMP_URL) {
        this.streamingService = streamingService;
        this.executor = executor;
        this.RTMP_URL = RTMP_URL;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        try {
            String error = (String) session.getAttributes().get("Error");
            if (error != null) {
                session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"" + error + "\"}"));
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            Authentication auth = (Authentication) session.getAttributes().get("Auth");
            if (!isAuthorized(auth)) {
                System.err.println("Acceso denegado: usuario no autorizado");
                session.sendMessage(new TextMessage(
                        "{\"type\":\"auth\",\"message\":\"" + "Acceso denegado: usuario no autorizado" + "\"}"));
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            sessions.put(session.getId(), session);
            String username = (String) session.getAttributes().get("username");
            System.out.println("Conexión establecida en OBS para el usuario: " + username);
        } catch (Exception e) {
            System.err.println("Error en enviar el mensaje de error en OBS: " + e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ex) {
                System.err.println("Error en cerrar la conexión: " + ex.getMessage());
            } finally {
                sessions.remove(session.getId());
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String sessionId = session.getId();
        try {
            streamingService.stopFFmpegProcessForUser(sessionId);
        } catch (Exception e) {
            System.err.println("Error al finalizar la transmisión: " + e.getMessage());
        } finally {
            sessions.remove(sessionId);
            Optional.ofNullable(ffmpegThreads.remove(sessionId)).ifPresent(Thread::interrupt);
            Optional.ofNullable(previews.remove(sessionId)).ifPresent(Thread::interrupt);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            // Parsear el mensaje recibido
            String payload = message.getPayload();

            if (payload.contains("request_rtmp_url")) {
                // Extraer userId
                Long userId = (Long) session.getAttributes().get("idUsuario");

                if (userId != null) {
                    // Generar URL RTMP para OBS
                    String rtmpUrl = RTMP_URL + userId + "_" + session.getId();
                    System.out.println("RTMP URL: " + rtmpUrl);

                    // Preparar la previsualización de la transmisión
                    executor.execute(() -> {
                        Thread currentThread = Thread.currentThread();
                        previews.put(session.getId(), currentThread); // Añadir el hilo al mapa
                        try {
                            this.streamingService.startPreview(rtmpUrl);
                        } catch (IOException | InterruptedException e) {
                            System.err.println(
                                    "Error al iniciar la previsualización de la transmisión: " + e.getMessage());
                            currentThread.interrupt();
                            previews.remove(session.getId());
                            throw new RuntimeException("Error al iniciar la previsualización de la transmisión: "
                                    + e.getMessage());
                        }
                    });
                    // Enviar la URL generada al cliente
                    session.sendMessage(new TextMessage("{\"type\":\"rtmp_url\",\"rtmpUrl\":\"" + rtmpUrl + "\"}"));
                } else {
                    // Enviar error si no se encuentra el userId
                    session.sendMessage(
                            new TextMessage("{\"type\":\"error\",\"message\":\"userId no proporcionado\"}"));
                }
            } else if (payload.contains("emitirOBS") && payload.contains("rtmpUrl")) {
                String streamId = this.extractStreamId(payload);
                try {
                    this.startFFmpegProcessForUser(streamId, RTMP_URL + streamId);
                    session.sendMessage(
                            new TextMessage("{\"type\":\"start\",\"message\":\" \"}"));
                } catch (Exception e) {
                    session.sendMessage(
                            new TextMessage("{\"type\":\"error\",\"message\":" + e.getMessage() + "}"));
                }
            } else if (payload.contains("detenerStreamOBS")) {
                this.streamingService.stopFFmpegProcessForUser(this.extractStreamId(payload));
            } else {
                session.sendMessage(
                        new TextMessage("{\"type\":\"error\",\"message\":\"Tipo de mensaje no reconocido\"}"));
            }
        } catch (RuntimeException e) {
            session.sendMessage(
                    new TextMessage("{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}"));
        } catch (Exception e) {
            System.err.println("Error en el manejo del mensaje en OBS: " + e.getMessage());
            session.sendMessage(
                    new TextMessage("{\"type\":\"error\",\"message\":\"Error en el manejo del mensaje en OBS: "
                            + e.getMessage() + "\"}"));
        }
    }

    private void startFFmpegProcessForUser(String userId, String rtmpUrl) {
        if (ffmpegThreads.containsKey(userId)) {
            System.err.println("El proceso FFmpeg ya está corriendo para el usuario " + userId);
            throw new RuntimeException("El proceso FFmpeg ya está corriendo para el usuario " + userId);
        }

        // Usar el Executor para ejecutar el proceso FFmpeg en un hilo separado
        executor.execute(() -> {
            Thread currentThread = Thread.currentThread();
            ffmpegThreads.put(userId.substring(userId.lastIndexOf("_") + 1), currentThread); // Añadir el hilo al mapa
            try {
                String[] streamIdAndSettings = { userId, null, null, null };
                this.streamingService.startLiveStreamingFromStream(streamIdAndSettings, rtmpUrl, null);
            } catch (Exception e) {
                currentThread.interrupt();
                ffmpegThreads.remove(userId.substring(userId.lastIndexOf("_") + 1));
                System.err.println("Error al iniciar FFmpeg para usuario " + userId + ": " + e.getMessage());
                throw new RuntimeException("Error al iniciar FFmpeg para usuario " + userId + ": " + e.getMessage());
            }
        });
    }

    private String extractStreamId(String payload) {
        if (payload.contains("rtmpUrl")) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(payload);
                String streamURL = jsonNode.get("rtmpUrl").asText();
                return streamURL.substring(streamURL.lastIndexOf("/") + 1);
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean isAuthorized(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_PROF") || role.equals("ROLE_ADMIN"));
    }
}