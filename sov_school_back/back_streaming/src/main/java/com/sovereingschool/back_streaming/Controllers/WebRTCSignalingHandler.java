package com.sovereingschool.back_streaming.Controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sovereingschool.back_streaming.Services.StreamingService;

public class WebRTCSignalingHandler extends BinaryWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Thread> ffmpegThreads = new ConcurrentHashMap<>();
    private final Map<String, String[]> sessionIdToStreamId = new ConcurrentHashMap<>();
    private final Executor executor; // Executor inyectado
    private final StreamingService streamingService;

    private Process pionProcess;
    private BufferedWriter pionWriter;
    private BufferedReader pionReader;
    private BufferedReader pionErrorReader;

    // Constructor modificado para aceptar Executor y StreamingService
    public WebRTCSignalingHandler(Executor executor, StreamingService streamingService) {
        this.executor = executor;
        this.streamingService = streamingService;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        try {
            String error = (String) session.getAttributes().get("Error");
            if (error != null) {
                session.sendMessage(new TextMessage("{\"type\":\"auth\",\"message\":\"" + error + "\"}"));
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
            System.out.println("Conexión establecida en WebOBS para el usuario: " + username);
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
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String userId = session.getId();
        sessions.remove(userId);

        Thread t = ffmpegThreads.remove(userId);
        if (t != null) {
            t.interrupt();
        }
        System.err.println("Conexión cerrada: " + userId + " Razón: " + status.getReason());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        // Parsear el mensaje recibido
        try {
            String payload = message.getPayload();
            JsonNode json = objectMapper.readTree(payload);

            if (json.has("userId")) {
                String userId = json.get("userId").asText();
                JsonNode videoSettings = json.get("videoSettings");

                String width = null;
                String height = null;
                String fps = null;

                if (videoSettings != null) {
                    width = videoSettings.has("width") ? videoSettings.get("width").asText() : null;
                    height = videoSettings.has("height") ? videoSettings.get("height").asText() : null;
                    fps = videoSettings.has("fps") ? videoSettings.get("fps").asText() : null;
                }

                String streamId = userId + "_" + session.getId();
                sessionIdToStreamId.put(session.getId(), new String[] { width, height, fps });

                // Enviar streamId al frontend
                try {
                    session.sendMessage(new TextMessage("{\"type\":\"streamId\",\"streamId\":\"" + streamId + "\"}"));
                } catch (IOException e) {
                    System.err.println("Error al enviar streamId: " + e.getMessage());
                    return;
                }

            } else if (json.has("detenerStreamWebcam")) {
                // Petición para detener el stream
                String streamId = json.get("detenerStreamWebcam").asText();
                System.out.println("Llega el mensaje de detener WebOBS");
                try {
                    streamingService.stopFFmpegProcessForUser(streamId);
                } catch (IOException | RuntimeException e) {
                    System.err.println(
                            "Error al detener el proceso FFmpeg para el stream: " + streamId + ": " + e.getMessage());
                }
            } else if (json.has("streamId") && json.has("sdp")) {
                String streamId = json.get("streamId").asText();
                if (!streamId.contains(session.getId())) {
                    System.err.println("El streamId no coincide con el sessionId");
                    return;
                }

                String sdpOffer = json.get("sdp").asText();
                String[] videoSetting = sessionIdToStreamId.remove(session.getId());

                if (pionProcess == null || !pionProcess.isAlive()) {
                    ProcessBuilder pb = new ProcessBuilder("go", "run", "pion-server.go");
                    pb.directory(new File("src/main/resources/pion/"));
                    pb.redirectErrorStream(false); // stderr separado

                    try {
                        pionProcess = pb.start();
                    } catch (IOException e) {
                        System.out.println("Error al iniciar proceso Go: " + e.getMessage());
                        return;
                    }

                    pionWriter = new BufferedWriter(new OutputStreamWriter(pionProcess.getOutputStream()));
                    pionReader = new BufferedReader(new InputStreamReader(pionProcess.getInputStream()));
                    pionErrorReader = new BufferedReader(new InputStreamReader(pionProcess.getErrorStream()));

                    // Leer logs del proceso Pion
                    new Thread(() -> {
                        String line;
                        try {
                            while ((line = pionErrorReader.readLine()) != null) {
                                System.err.println("PION: " + line);
                                if (line.contains("SDP generado:")) {
                                    // Iniciar el hilo de FFmpeg
                                    executor.execute(() -> {
                                        try {
                                            // Ejecutar ffmpeg y pasarle el stream
                                            streamingService.startLiveStreamingFromStream(
                                                    streamId,
                                                    "/tmp/" + streamId + ".sdp",
                                                    videoSetting);
                                            ffmpegThreads.put(session.getId(), Thread.currentThread());
                                        } catch (Exception e) {
                                            System.err
                                                    .println("Error al iniciar FFmpeg para el stream " + streamId + ": "
                                                            + e.getMessage());
                                        }
                                    });
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error en stderr PION: " + e.getMessage());
                        }
                    }).start();
                }

                // Enviar JSON con SDP offer al proceso Go
                ObjectNode request = objectMapper.createObjectNode();
                request.put("sessionId", streamId);
                request.put("sdp", sdpOffer);
                request.put("width", Integer.parseInt(videoSetting[0]));
                request.put("height", Integer.parseInt(videoSetting[1]));
                request.put("fps", Integer.parseInt(videoSetting[2]));

                synchronized (pionWriter) {
                    try {
                        pionWriter.write(request.toString());
                        pionWriter.newLine();
                        pionWriter.flush();
                    } catch (IOException e) {
                        System.err.println("Error al enviar al proceso Go: " + e.getMessage());
                        return;
                    }
                }

                // Leer SDP answer (solo la primera línea de salida del script)
                String answerLine;
                try {
                    answerLine = pionReader.readLine();
                } catch (IOException e) {
                    System.err.println("Error al leer la respuesta del proceso Go: " + e.getMessage());
                    return;
                }

                JsonNode sdpJson = objectMapper.readTree(answerLine);
                String sdpString = sdpJson.get("sdp").asText();

                // Enviar SDP al frontend
                ObjectNode response = objectMapper.createObjectNode();
                response.put("type", "webrtc-answer");
                response.put("sdp", sdpString);

                try {
                    session.sendMessage(new TextMessage(response.toString()));
                } catch (IOException e) {
                    System.out.println("Error al enviar la respuesta al frontend: " + e.getMessage());
                    return;
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error al parsear el mensaje JSON: " + e.getMessage());
        }
    }

    private boolean isAuthorized(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            System.err.println("No autenticado");
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_PROF") || role.equals("ROLE_ADMIN"));
    }
}