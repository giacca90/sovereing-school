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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private Logger logger = LoggerFactory.getLogger(WebRTCSignalingHandler.class);

    /**
     * Constructor de WebRTCSignalingHandler
     *
     * @param executor         Ejecutor de tareas
     * @param streamingService Servicio de streaming
     */
    public WebRTCSignalingHandler(Executor executor,
            StreamingService streamingService) {
        this.executor = executor;
        this.streamingService = streamingService;
    }

    /**
     * Función para establecer la conexión
     * 
     * @param session WebSocketSession con la conexión
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String error = (String) session.getAttributes().get("Error");
            if (error != null) {
                session.sendMessage(new TextMessage("{\"type\":\"auth\",\"message\":\"" + error + "\"}"));
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            Authentication auth = (Authentication) session.getAttributes().get("Auth");
            if (!isAuthorized(auth)) {
                logger.error("Acceso denegado: usuario no autorizado");
                session.sendMessage(new TextMessage(
                        "{\"type\":\"auth\",\"message\":\"" + "Acceso denegado: usuario no autorizado" + "\"}"));
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            String username = (String) session.getAttributes().get("username");
            logger.info("Conexión establecida en WebOBS para el usuario: {}", username);
        } catch (Exception e) {
            logger.error("Error en enviar el mensaje de error en OBS: {}", e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ex) {
                logger.error("Error en cerrar la conexión: {}", ex.getMessage());
            }
        }
    }

    /**
     * Función para cerrar la conexión
     * 
     * @param session WebSocketSession con la conexión
     * @param status  CloseStatus con el estado de la conexión
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = session.getId();

        if (pionWriter != null) {
            try {
                pionWriter.close();
            } catch (IOException e) {
                logger.error("Error al cerrar pionWriter: {}", e.getMessage());
            }
        }

        if (pionReader != null) {
            try {
                pionReader.close();
            } catch (IOException e) {
                logger.error("Error al cerrar pionReader: {}", e.getMessage());
            }
        }

        if (pionProcess != null) {
            pionProcess.destroy();
        }

        Thread t = ffmpegThreads.remove(userId);
        if (t != null) {
            t.interrupt();
        }
        logger.info("Conexión cerrada: {} Razón: {}", userId, status.getReason());
    }

    /**
     * Función para manejar los mensajes de texto
     * 
     * @param session WebSocketSession con la conexión
     * @param message TextMessage con el mensaje
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            JsonNode json = objectMapper.readTree(payload);
            logger.info("Recibido desde el front: {}", payload);

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
                            logger.error("Error al enviar streamId: {}", e.getMessage());
                            try {
                                session.close();
                            } catch (IOException ex) {
                                logger.error("Error en cerrar la sesión: {}", ex.getMessage());
                            }
                            return;
                        }
                        break;

                    case "emitir": {
                        streamId = json.get("streamId").asText();
                        JsonNode videoSettings = json.get("videoSettings");
                        logger.info("VideoSettings: {}", videoSettings);
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
                        logger.info("Empieza a emitir Webcam");
                        try {
                            session.sendMessage(new TextMessage("{\"type\":\"info\",\"message\":\"ok\"}"));
                        } catch (Exception e) {
                            logger.error("Error al enviar mensaje de info: {}", e.getMessage());
                        }
                        break;
                    }

                    case "offer": {
                        logger.info("Recibimos un stream de Webcam");
                        streamId = json.get("streamId").asText();
                        if (!compruebaSesion(streamId, session)) {
                            return;
                        }

                        String sdpOffer = json.get("sdp").asText();
                        String[] videoSetting = streamIdToStreamSettings.remove(streamId);
                        if (videoSetting == null) {
                            return;
                        }
                        logger.info("VideoSetting: {}", Arrays.toString(videoSetting));
                        logger.info("SDP Offer: {}", sdpOffer);

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
                                logger.error("Error al enviar al proceso Go: {}", e.getMessage());
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
                                logger.error("Error al enviar al proceso Go: {}", e.getMessage());
                            }
                        }
                        break;
                    }

                    case "detenerStreamWebRTC": {
                        streamId = json.get("streamId").asText();
                        if (!compruebaSesion(streamId, session)) {
                            return;
                        }
                        logger.info("Llega el mensaje de detener WebOBS");
                        try {
                            // IMPORTANTE: añadir '\n' y flush para que el proceso Go lea la línea
                            this.pionWriter.write("{\"type\":\"stopStreamByID\",\"streamId\":\"" + streamId + "\"}\n");
                            this.pionWriter.flush();

                            // Opcional: pequeño retardo para reducir la probabilidad de race (200-500ms)
                            // Thread.sleep(250);

                            streamingService.stopFFmpegProcessForUser(streamId);
                        } catch (IOException | RuntimeException e) {
                            logger.error("Error al detener el proceso FFmpeg para el stream: {}", streamId);
                        } finally {
                            session.close();
                        }
                        break;
                    }

                    default:
                        logger.error("JSON desconocido: {}", json.toString());
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Error en handleTextMessage: {}", e.getMessage());
        }
    }

    /**
     * Función para verificar si el usuario está autorizado
     * 
     * @param auth Authentication con los datos del usuario
     * @return Booleano con el resultado de la verificación
     */
    protected boolean isAuthorized(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            logger.error("No autenticado");
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_PROF") || role.equals("ROLE_ADMIN"));
    }

    /**
     * Función para comprobar si la sesión coincide con el streamId
     * 
     * @param streamId String con el streamId
     * @param session  WebSocketSession con la sesión
     * @return Booleano con el resultado de la comprobación de sesión
     */
    protected boolean compruebaSesion(String streamId, WebSocketSession session) {
        if (!streamId.contains(session.getId())) {
            logger.error("El streamId no coincide con el sessionId");
            try {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"error\",\"message\":\"El streamId no coincide con el sessionId \"}"));
            } catch (IOException e) {
                logger.error("Error al enviar el mensaje de error: {}", e.getMessage());
            }
            try {
                session.close();
            } catch (IOException ex) {
                logger.error("Error en cerrar la sesión: {}", ex.getMessage());
            }
            return false;
        }
        return true;
    }

    /**
     * Función para iniciar el proceso Pion
     */
    protected void startPion() {
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
            // Ignorar excepciones
        }

        if (inDocker) {
            // En Docker (prod), usar el binario compilado que debería estar en
            // /app/bin/pion-server
            String prodBinary = "/app/pion-server";
            pb = new ProcessBuilder(prodBinary);
            logger.info("Iniciando Pion desde binario compilado en Docker: {}", prodBinary);
        } else {
            // En desarrollo, usar go run sobre el script
            pb = new ProcessBuilder("/usr/local/go/bin/go", "run", "pion-server.go");
            pb.directory(new File("src/main/resources/pion/"));
            logger.info("Iniciando Pion en modo desarrollo con go run");
        }

        pb.redirectErrorStream(false); // mantener stdout y stderr separados

        try {
            pionProcess = pb.start();
            logger.info("Proceso Pion iniciado correctamente");
        } catch (IOException e) {
            logger.error("Error al iniciar el proceso Pion: {}", e.getMessage());
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
                    System.out.println("PION Log: " + line);
                }
            } catch (IOException e) {
                logger.error("Error leyendo stderr de Pion: {}", e.getMessage());
            }
        });

        // Hilo para leer stdout
        executor.execute(() -> {
            String line;
            try {
                while ((line = pionReader.readLine()) != null) {
                    logger.info("STDOUT Pion: {}", line);
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

                                logger.info("Recibido SDP RTP para stream {}", streamId);
                                logger.info(sdp);
                                executor.execute(() -> {
                                    try (InputStream sdpStream = new ByteArrayInputStream(
                                            sdp.getBytes(StandardCharsets.UTF_8))) {
                                        streamingService.startLiveStreamingFromStream(streamId, sdpStream,
                                                videoSetting);
                                        ffmpegThreads.put(session.getId(), Thread.currentThread());
                                        logger.info("Iniciado hilo FFmpeg para stream {}", streamId);
                                    } catch (Exception e) {
                                        logger.error("Error iniciando FFmpeg para stream {}: {}", streamId,
                                                e.getMessage());
                                    }
                                });
                                break;

                            case "candidate": {
                                String streamIdCandidate = msg.get("streamId").asText();
                                if (session == null) {
                                    logger.error("No se encontró la sesión para el streamId {}", streamIdCandidate);
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
                                logger.error("Mensaje JSON desconocido de Pion: {}", msg.toString());
                        }
                    } catch (Exception ex) {
                        logger.error("Error parseando JSON desde stdout de Pion: {}", line);
                        ex.printStackTrace();
                    }
                }
            } catch (IOException e) {
                logger.error("Error leyendo stdout de Pion: {}", e.getMessage());
            }
        });
    }
}