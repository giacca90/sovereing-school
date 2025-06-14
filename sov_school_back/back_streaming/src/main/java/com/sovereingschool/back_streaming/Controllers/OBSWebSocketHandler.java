package com.sovereingschool.back_streaming.Controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
    public final Map<String, Process> previewProcesses = new ConcurrentHashMap<>();
    private final StreamingService streamingService;
    private final Executor executor;
    private final String RTMP_URL;
    private final String uploadDir;

    public OBSWebSocketHandler(Executor executor, StreamingService streamingService, String RTMP_URL,
            String uploadDir) {
        this.streamingService = streamingService;
        this.executor = executor;
        this.RTMP_URL = RTMP_URL;
        this.uploadDir = uploadDir;
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

        Process preProcess = this.previewProcesses.remove(sessionId);
        if (preProcess != null && preProcess.isAlive()) {
            try {
                // Enviar una señal de terminación controlada
                OutputStream os = preProcess.getOutputStream();
                os.write('q'); // Enviar la letra 'q'
                os.flush(); // Asegurarse de que se envíe
                os.close();
                // Esperar a que el proceso termine de forma controlada
                // Esperar un segundo para que termine de manera controlada
                boolean finished = preProcess.waitFor(1, TimeUnit.SECONDS);

                if (finished) {
                    // El proceso terminó correctamente
                    int exitCode = preProcess.exitValue();
                    if (exitCode == 0) {
                    } else {
                        System.err.println("FFmpeg preview terminó con un error. Código de salida: " + exitCode);
                    }
                } else {
                    // Si no terminó en 1 segundo, forzar la terminación
                    System.err.println(
                            "El proceso FFmpeg preview no respondió en el tiempo esperado. Terminando de forma forzada...");
                    preProcess.destroy(); // Intentar una terminación limpia
                    if (preProcess.isAlive()) {
                        preProcess.destroyForcibly(); // Forzar si sigue vivo
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("El proceso fue interrumpido: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restaurar el estado de interrupción
                throw new RuntimeException("El proceso fue interrumpido: " + e.getMessage());
            }

            // Elimina la carpeta de la preview
            Path baseUploadDir = Paths.get(uploadDir);
            Path previewsDir = baseUploadDir.resolve("previews");
            try (Stream<Path> paths = Files.walk(previewsDir, 1)) {
                Optional<Path> matchingDir = paths
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().endsWith("_" + sessionId))
                        .findFirst();

                if (matchingDir.isPresent()) {
                    Path previewDir = baseUploadDir.resolve("previews");
                    String streamId = previewDir.getFileName().toString();
                    if (!Files.exists(previewDir) || !Files.isDirectory(previewDir)) {
                        // Si la carpeta no existe, buscar la carpeta con el mismo nombre
                        String temp = streamId;
                        streamId = Files.walk(previewDir.getParent())
                                .sorted(Comparator.reverseOrder())
                                .filter(path -> path.getFileName().toString().contains(temp))
                                .findFirst().get().toString();
                        previewDir = baseUploadDir.resolve("previews").resolve(streamId);
                    }
                    try {
                        Files.walk(previewDir)
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException e) {
                        System.err.println("Error al eliminar la carpeta de la previsualización: " + e.getMessage());
                        throw new RuntimeException(
                                "Error al eliminar la carpeta de la previsualización: " + e.getMessage());
                    }
                    Path m3u8 = baseUploadDir.resolve("previews").resolve(streamId + ".m3u8");
                    if (Files.exists(m3u8)) {
                        Files.delete(m3u8);
                    }
                } else {
                    System.out.println("No se encontró una carpeta para el sessionId: " + sessionId);
                }
            } catch (IOException e) {
                System.err.println("Error al buscar la carpeta: " + e.getMessage());
            }
        }
    }

    /**
     * Función para iniciar la previsualización del flujo de RTMP
     * 
     * @param rtmpUrl
     * @throws IOException
     * @throws InterruptedException
     */
    public void startPreview(String rtmpUrl) throws IOException, InterruptedException, RuntimeException {
        String previewId = rtmpUrl.substring(rtmpUrl.lastIndexOf("/") + 1);
        Path baseUploadDir = Paths.get(uploadDir);
        Path previewDir = baseUploadDir.resolve("previews");
        // Crear el directorio de salida si no existe
        if (!Files.exists(previewDir)) {
            Files.createDirectories(previewDir);
        }

        Path outputDir = previewDir.resolve(previewId);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // preparar comando FFmpeg preview
        List<String> ffmpegCommand = List.of(
                "ffmpeg",
                "-vaapi_device", "/dev/dri/renderD128", // Dispositivo VAAPI
                "-re",
                "-i", rtmpUrl,
                "-vf", "format=nv12,hwupload", // Filtro para VAAPI
                "-c:v", "h264_vaapi", // Codificador VAAPI
                // "-preset", "veryfast", // No funciona con vaapi
                "-qp", "24", // Calidad para vaapi
                // "-tune", "zerolatency", // No funciona con vaapi
                "-fflags", "nobuffer",
                "-loglevel", "warning",
                "-f", "hls",
                "-hls_time", "0.5",
                "-hls_list_size", "2",
                "-hls_flags", "delete_segments+independent_segments+program_date_time",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", outputDir + "/%03d.ts",
                "-hls_base_url", previewId + "/",
                "-g", "10",
                previewDir + "/" + previewId + ".m3u8");

        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        this.previewProcesses.put(previewId.substring(previewId.lastIndexOf("_") + 1), process);

        // Capturar logs del proceso FFmpeg
        Thread logReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("FFmpeg preview: " + line); // Mostrar logs en la consola
                }
            } catch (IOException e) {
                System.err.println("Error leyendo salida de FFmpeg preview: " + e.getMessage());
                throw new RuntimeException("Error leyendo salida de FFmpeg preview: " + e.getMessage());
            }
        });
        logReader.start();

        logReader.join(); // Esperar a que se terminen de leer los logs
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            // Parsear el mensaje recibido
            String payload = message.getPayload();

            if (payload.contains("request_rtmp_url")) {
                // Extraer userId
                Long userId = (Long) session.getAttributes().get("idUsuario");

                if (userId == null) {
                    // Enviar error si no se encuentra el userId
                    session.sendMessage(
                            new TextMessage("{\"type\":\"error\",\"message\":\"userId no proporcionado\"}"));
                    return;
                }
                // Generar URL RTMP para OBS
                String rtmpUrl = RTMP_URL + userId + "_" + session.getId();
                System.out.println("RTMP URL: " + rtmpUrl);

                // Preparar la previsualización de la transmisión
                executor.execute(() -> {
                    Thread currentThread = Thread.currentThread();
                    previews.put(session.getId(), currentThread); // Añadir el hilo al mapa
                    try {
                        this.startPreview(rtmpUrl);
                    } catch (IOException | InterruptedException e) {
                        System.err.println(
                                "Error al iniciar la previsualización de la transmisión: " + e.getMessage());
                        currentThread.interrupt();
                        previews.remove(session.getId());
                    }
                });
                // Enviar la URL generada al cliente
                session.sendMessage(new TextMessage("{\"type\":\"rtmp_url\",\"rtmpUrl\":\"" + rtmpUrl + "\"}"));

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

    private void startFFmpegProcessForUser(String streamId, String rtmpUrl) {
        String sessionId = streamId.substring(streamId.lastIndexOf('_') + 1);
        String userId = streamId.substring(0, streamId.lastIndexOf('_'));
        if (ffmpegThreads.containsKey(sessionId)) {
            System.err.println("El proceso FFmpeg ya está corriendo para el usuario " + userId);
            throw new RuntimeException("El proceso FFmpeg ya está corriendo para el usuario " + userId);
        }

        // Usar el Executor para ejecutar el proceso FFmpeg en un hilo separado
        executor.execute(() -> {
            Thread currentThread = Thread.currentThread();
            ffmpegThreads.put(sessionId, currentThread); // Añadir el hilo al mapa
            try {
                this.streamingService.startLiveStreamingFromStream(streamId, rtmpUrl, null);
            } catch (Exception e) {
                currentThread.interrupt();
                ffmpegThreads.remove(sessionId);
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