package com.sovereingschool.back_streaming.Controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sovereingschool.back_streaming.Services.StreamingService;

public class WebRTCSignalingHandler extends BinaryWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // <streamId, WebSocketSession>
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Thread> ffmpegThreads = new ConcurrentHashMap<>();
    private final Map<String, String[]> streamIdToStreamSettings = new ConcurrentHashMap<>();
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

            String username = (String) session.getAttributes().get("username");
            System.out.println("Conexión establecida en WebOBS para el usuario: " + username);
        } catch (Exception e) {
            System.err.println("Error en enviar el mensaje de error en OBS: " + e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ex) {
                System.err.println("Error en cerrar la conexión: " + ex.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String userId = session.getId();

        if (pionWriter != null) {
            try {
                pionWriter.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar pionWriter: " + e.getMessage());
            }
        }

        if (pionReader != null) {
            try {
                pionReader.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar pionReader: " + e.getMessage());
            }
        }

        if (pionProcess != null) {
            pionProcess.destroy();
        }

        Thread t = ffmpegThreads.remove(userId);
        if (t != null) {
            t.interrupt();
        }
        System.err.println("Conexión cerrada: " + userId + " Razón: " + status.getReason());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            String payload = message.getPayload();
            JsonNode json = objectMapper.readTree(payload);
            System.out.println("Recibido desde el front: " + payload);

            if (json.has("type")) {
                String type = json.get("type").asText();

                switch (type) {
                    case "userId":
                        Long userId = (Long) session.getAttributes().get("idUsuario");
                        String streamId = userId + "_" + session.getId();
                        // Enviar streamId al frontend
                        try {
                            session.sendMessage(
                                    new TextMessage("{\"type\":\"streamId\",\"streamId\":\"" + streamId + "\"}"));
                            sessions.put(streamId, session);
                        } catch (IOException e) {
                            System.err.println("Error al enviar streamId: " + e.getMessage());
                            try {
                                session.close();
                            } catch (IOException ex) {
                                System.err.println("Error en cerrar la sesión: " + ex.getMessage());
                            }
                            return;
                        }
                        break;

                    case "emitir": {
                        streamId = json.get("streamId").asText();
                        JsonNode videoSettings = json.get("videoSettings");
                        System.out.println("VideoSettings: " + videoSettings);
                        if (!compruebaSesion(streamId, session)) {
                            return;
                        }
                        String width = null;
                        String height = null;
                        String fps = null;

                        if (videoSettings != null) {
                            width = videoSettings.has("width") ? videoSettings.get("width").asText() : null;
                            height = videoSettings.has("height") ? videoSettings.get("height").asText() : null;
                            fps = videoSettings.has("fps") ? videoSettings.get("fps").asText() : null;
                        }

                        streamIdToStreamSettings.put(streamId, new String[] { width, height, fps });
                        System.out.println("Empieza a emitir Webcam");
                        try {
                            session.sendMessage(new TextMessage("{\"type\":\"info\",\"message\":\"ok\"}"));
                        } catch (Exception e) {
                            System.err.println("Error al enviar mensaje de info: " + e.getMessage());
                        }
                        break;
                    }

                    case "offer": {
                        System.out.println("Recibimos un stream de Webcam");
                        streamId = json.get("streamId").asText();
                        if (!compruebaSesion(streamId, session)) {
                            return;
                        }

                        String sdpOffer = json.get("sdp").asText();
                        String[] videoSetting = streamIdToStreamSettings.remove(streamId);
                        if (videoSetting == null) {
                            return;
                        }
                        System.out.println("VideoSetting: " + Arrays.toString(videoSetting));
                        System.out.println("SDP Offer: " + sdpOffer);

                        this.startPion(); // Iniciar proceso Go si no está iniciado
                        // Enviar JSON con SDP offer al proceso Go
                        ObjectNode request = objectMapper.createObjectNode();
                        request.put("type", "offer");
                        request.put("streamId", streamId);
                        request.put("sdp", sdpOffer);
                        // Convertir array de Java a ArrayNode de Jackson
                        ArrayNode videoSettingsNode = objectMapper.createArrayNode();
                        for (String setting : videoSetting) {
                            videoSettingsNode.add(setting);
                        }
                        request.set("videoSettings", videoSettingsNode);

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
                        break;
                    }

                    case "candidate": {
                        // Pasamos los candidate a PION
                        streamId = json.get("streamId").asText();
                        if (!compruebaSesion(streamId, session)) {
                            return;
                        }
                        JsonNode candidateNode = json.get("candidate"); // obtienes el objeto completo
                        ObjectNode candidateResponse = objectMapper.createObjectNode();
                        candidateResponse.put("type", "candidate");
                        candidateResponse.put("streamId", streamId);
                        candidateResponse.set("candidate", candidateNode);
                        synchronized (pionWriter) {
                            try {
                                pionWriter.write(candidateResponse.toString());
                                pionWriter.newLine();
                                pionWriter.flush();
                            } catch (IOException e) {
                                System.err.println("Error al enviar al proceso Go: " + e.getMessage());
                                return;
                            }
                        }
                        break;
                    }

                    case "detenerStreamWebRTC": {
                        streamId = json.get("streamId").asText();
                        if (!compruebaSesion(streamId, session)) {
                            return;
                        }
                        System.out.println("Llega el mensaje de detener WebOBS");
                        try {
                            // IMPORTANTE: añadir '\n' y flush para que el proceso Go lea la línea
                            this.pionWriter.write("{\"type\":\"stopStreamByID\",\"streamId\":\"" + streamId + "\"}\n");
                            this.pionWriter.flush();

                            // Opcional: pequeño retardo para reducir la probabilidad de race (200-500ms)
                            // Thread.sleep(250);

                            streamingService.stopFFmpegProcessForUser(streamId);
                        } catch (IOException | RuntimeException e) {
                            System.err.println(
                                    "Error al detener el proceso FFmpeg para el stream: " + streamId + ": "
                                            + e.getMessage());
                        } finally {
                            session.close();
                        }
                        break;
                    }

                    default:
                        System.err.println("JSON desconocido: " + json.toString());
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error en handleTextMessage: " + e.getMessage());
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

    private boolean compruebaSesion(String streamId, WebSocketSession session) {
        if (!streamId.contains(session.getId())) {
            System.err.println("El streamId no coincide con el sessionId");
            try {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"error\",\"message\":\"El streamId no coincide con el sessionId \"}"));
            } catch (IOException e) {
                System.err.println("Error al enviar el mensaje de error: " + e.getMessage());
            }
            try {
                session.close();
            } catch (IOException ex) {
                System.err.println("Error en cerrar la sesión: " + ex.getMessage());
            }
            return false;
        }
        return true;
    }

    private void startPion() {
        if (pionProcess != null && pionProcess.isAlive()) {
            return; // Ya está corriendo
        }

        ProcessBuilder pb;

        // Detectar si estamos dentro de Docker
        boolean inDocker = false;
        try {
            File dockerEnv = new File("/.dockerenv");
            inDocker = dockerEnv.exists();
        } catch (Exception ignored) {
        }

        if (inDocker) {
            // En Docker (prod), usar el binario compilado que debería estar en
            // /app/bin/pion-server
            String prodBinary = "/app/pion-server";
            pb = new ProcessBuilder(prodBinary);
            System.out.println("Iniciando Pion desde binario compilado en Docker: " + prodBinary);
        } else {
            // En desarrollo, usar go run sobre el script
            pb = new ProcessBuilder("go", "run", "pion-server.go");
            pb.directory(new File("src/main/resources/pion/"));
            System.out.println("Iniciando Pion en modo desarrollo con go run");
        }

        pb.redirectErrorStream(false); // mantener stdout y stderr separados

        try {
            pionProcess = pb.start();
            System.out.println("Proceso Pion iniciado correctamente");
        } catch (IOException e) {
            System.err.println("Error al iniciar el proceso Pion: " + e.getMessage());
            return;
        }

        // Inicializar readers/writers
        pionWriter = new BufferedWriter(new OutputStreamWriter(pionProcess.getOutputStream()));
        pionReader = new BufferedReader(new InputStreamReader(pionProcess.getInputStream()));
        pionErrorReader = new BufferedReader(new InputStreamReader(pionProcess.getErrorStream()));

        // Hilo para leer stderr
        executor.execute(() -> {
            String line;
            try {
                while ((line = pionErrorReader.readLine()) != null) {
                    System.err.println("PION Log: " + line);
                }
            } catch (IOException e) {
                System.err.println("Error leyendo stderr de Pion: " + e.getMessage());
            }
        });

        // Hilo para leer stdout
        executor.execute(() -> {
            String line;
            try {
                while ((line = pionReader.readLine()) != null) {
                    System.out.println("STDOUT Pion: " + line);
                    try {
                        JsonNode msg = objectMapper.readTree(line);
                        String type = msg.get("type").asText();
                        String streamId = msg.get("streamId").asText();
                        WebSocketSession session = sessions.get(streamId);

                        switch (type) {
                            case "webrtc-answer":
                                String sdpAnswer = msg.get("sdp").asText();
                                ObjectNode response = objectMapper.createObjectNode();
                                response.put("type", "webrtc-answer");
                                response.put("sdp", sdpAnswer);
                                session.sendMessage(new TextMessage(response.toString()));
                                // System.out.println("Respuesta webrtc-answer enviada al frontend");
                                // System.out.println(sdpAnswer);
                                break;

                            case "rtp-sdp":
                                String sdp = msg.get("sdp").asText();
                                JsonNode videoSettingsNode = msg.get("videoSettings");
                                String[] videoSetting = new String[videoSettingsNode.size()];
                                for (int i = 0; i < videoSettingsNode.size(); i++) {
                                    videoSetting[i] = videoSettingsNode.get(i).asText();
                                }

                                System.out.println("Recibido SDP RTP para stream " + streamId);
                                System.out.println(sdp);
                                executor.execute(() -> {
                                    try (InputStream sdpStream = new ByteArrayInputStream(
                                            sdp.getBytes(StandardCharsets.UTF_8))) {
                                        streamingService.startLiveStreamingFromStream(streamId, sdpStream,
                                                videoSetting);
                                        ffmpegThreads.put(session.getId(), Thread.currentThread());
                                        System.out.println("Iniciado hilo FFmpeg para stream " + streamId);
                                    } catch (Exception e) {
                                        System.err.println("Error iniciando FFmpeg para stream " + streamId + ": "
                                                + e.getMessage());
                                    }
                                });
                                break;

                            case "candidate": {
                                String streamIdCandidate = msg.get("streamId").asText();
                                if (session == null) {
                                    System.err
                                            .println("No se encontró la sesión para el streamId " + streamIdCandidate);
                                    break;
                                }

                                // candidate llega como string plano desde Pion
                                String candidateStr = msg.get("candidate").asText();

                                ObjectNode candidateResponse = objectMapper.createObjectNode();
                                candidateResponse.put("type", "candidate");

                                // Crear estructura compatible con RTCIceCandidateInit
                                ObjectNode candidateInit = objectMapper.createObjectNode();
                                candidateInit.put("candidate", candidateStr);
                                candidateInit.put("sdpMid", "0"); // valores neutros válidos
                                candidateInit.put("sdpMLineIndex", 0);

                                candidateResponse.set("candidate", candidateInit);

                                session.sendMessage(new TextMessage(candidateResponse.toString()));
                                break;
                            }

                            default:
                                System.out.println("Mensaje JSON desconocido de Pion: " + msg.toString());
                        }
                    } catch (Exception ex) {
                        System.err.println("Error parseando JSON desde stdout de Pion: " + line);
                        ex.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error leyendo stdout de Pion: " + e.getMessage());
            }
        });
    }
}