package com.sovereingschool.back_streaming.Services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_streaming.Models.ResolutionProfile;
import com.sovereingschool.back_streaming.Utils.GPUDetector;

@Service
@Transactional
public class StreamingService {
    private final Map<String, Process> ffmpegProcesses = new ConcurrentHashMap<>();

    private ClaseRepository claseRepo;

    private Logger logger = LoggerFactory.getLogger(StreamingService.class);

    private String uploadDir;

    private Executor executor;

    /**
     * Constructor de StreamingService
     *
     * @param uploadDir Ruta de carga de archivos
     * @param claseRepo Repositorio de clases
     */
    public StreamingService(
            @Value("${variable.VIDEOS_DIR}") String uploadDir,
            ClaseRepository claseRepo) {
        this.uploadDir = uploadDir;
        this.claseRepo = claseRepo;
        // Cambiar si hay más de una GPU, o si se procesan todos los videos con CPU
        this.executor = Executors.newFixedThreadPool(1);
    }

    /**
     * Función para convertir los videos de un curso en paralelo.
     * 
     * @param curso Curso con los videos
     * @throws NotFoundException Si no hay clases en el curso
     */
    @Async
    public void convertVideos(Curso curso) throws NotFoundException {
        if (curso.getClasesCurso() == null || curso.getClasesCurso().isEmpty()) {
            throw new NotFoundException("Curso sin clases");
        }

        Path baseUploadDir = Paths.get(uploadDir);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Clase clase : curso.getClasesCurso()) {
            Path destinationPath = baseUploadDir.resolve(curso.getIdCurso().toString())
                    .resolve(clase.getIdClase().toString());
            String direccion = clase.getDireccionClase();
            if (direccion == null ||
                    direccion.isEmpty() ||
                    direccion.contains(destinationPath.toString()) ||
                    direccion.endsWith(".m3u8") ||
                    !direccion.contains(".")) {
                continue;
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String originalName = Thread.currentThread().getName();
                try {
                    Thread.currentThread().setName(originalName + "-Conv-Clase-" + clase.getIdClase());
                    processSingleClase(curso, clase, baseUploadDir, destinationPath);
                } catch (Exception e) {
                    logger.error("Error en conversión de clase {}: {}", clase.getIdClase(), e.getMessage());
                } finally {
                    Thread.currentThread().setName(originalName);
                }
            }, this.executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        logger.info("Finalizado proceso paralelo para curso {}", curso.getIdCurso());
    }

    /**
     * Función para iniciar la transmisión en vivo
     * 
     * @param streamId     ID del flujo
     * @param inputStream  InputStream con el flujo
     * @param videoSetting String[] con la configuración de la transmisión
     * @throws IOException
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws RepositoryException
     * @throws InternalServerException
     */
    public void startLiveStreamingFromStream(String streamId, Object inputStream, String[] videoSetting)
            throws IOException, IllegalArgumentException, RepositoryException,
            InternalServerException {
        Clase clase = claseRepo.findByDireccionClase(streamId).orElseThrow(
                () -> new RepositoryException("No se encuentra la clase con la dirección " + streamId));
        Long idCurso = clase.getCursoClase().getIdCurso();
        Long idClase = clase.getIdClase();
        Path baseUploadDir = Paths.get(uploadDir);
        Path outputDir = baseUploadDir.resolve(idCurso.toString()).resolve(idClase.toString());
        claseRepo.updateClase(idClase, clase.getNombreClase(), clase.getTipoClase(),
                outputDir.toString() + "/master.m3u8",
                clase.getPosicionClase());

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        List<String> ffmpegCommand;
        if (inputStream instanceof String str) {
            ffmpegCommand = this.creaComandoFFmpeg(str, true, videoSetting);
        } else if (inputStream instanceof InputStream) {
            ffmpegCommand = this.creaComandoFFmpeg("pipe:0", true, videoSetting);
        } else {
            logger.error("Tipo de entrada no soportado");
            return;
        }
        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);

        processBuilder.directory(outputDir.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String processKey = streamId.substring(streamId.lastIndexOf("_") + 1);
        ffmpegProcesses.put(processKey, process);

        executor.execute(() -> {
            String originalName = Thread.currentThread().getName();
            Thread.currentThread().setName("FFmpegLog-" + processKey);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean sdpSent = false;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg clase " + clase.getIdClase() + ": " + line);
                    if (!sdpSent && inputStream instanceof InputStream sdpStream && line.contains("ffmpeg version")) {
                        sdpSent = this.sendSDP(process, sdpStream);
                    }
                }
            } catch (IOException e) {
                logger.error("Error leyendo salida de FFmpeg para el stream {}: {}", processKey, e.getMessage());
            } finally {
                Thread.currentThread().setName(originalName);
                ffmpegProcesses.remove(processKey);
            }
        });
    }

    /**
     * Función para detener el proceso FFmpeg para un usuario
     * 
     * @param streamId ID del flujo
     * @throws InternalServerException
     */
    public void stopFFmpegProcessForUser(String streamId) throws InternalServerException {
        String sessionId = streamId.substring(streamId.lastIndexOf('_') + 1);
        Process process = ffmpegProcesses.remove(sessionId);

        if (process == null)
            return;

        try {
            process.destroy();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("El proceso fue interrumpido: " + e.getMessage(), e);
        }
    }

    /**
     * Función para obtener la URL de la previsualización
     * 
     * @param idPreview ID de la previsualización
     * @return Path con la URL de la previsualización
     * @throws InternalServerException
     */
    public Path getPreview(String idPreview) throws InternalServerException {
        Path baseUploadDir = Paths.get(uploadDir);
        Path previewDir = baseUploadDir.resolve("previews");
        Path m3u8 = previewDir.resolve(idPreview + ".m3u8");
        while (!Files.exists(m3u8)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InternalServerException("Error al esperar a que se genere el preview: " + e.getMessage());
            }
        }
        return m3u8;
    }

    /**
     * Función para crear el comando FFmpeg
     * 
     * @param inputFilePath String con la ruta del flujo
     * @param live          Booleano con el tipo de transmisión
     * @param videoSetting  String[] con la configuración de la transmisión
     * @return List<String> con el comando FFmpeg
     * @throws IOException
     * @throws InterruptedException
     * @throws InternalServerException
     */
    protected List<String> creaComandoFFmpeg(String inputFilePath, boolean live, String[] videoSetting)
            throws InternalServerException {

        String[] settings;
        boolean tieneAudio;

        // 1. Identificar si es un Pipe (Pion) o un flujo analizable (Archivo/RTMP)
        // El pipe:0 es el que causa el bloqueo con Pion si se intenta usar ffprobe
        if (inputFilePath.contains("pipe:")) {
            // En WebRTC/Pion, confiamos en los settings pasados o valores por defecto
            settings = (videoSetting != null && videoSetting.length >= 3 && videoSetting[0] != null)
                    ? videoSetting
                    : new String[] { "1280", "720", "30" };
            tieneAudio = true; // Asumimos audio para WebRTC para preparar el grafo
        } else {
            // Para Archivos Y flujos RTMP (rtmp://...), usamos ffprobe
            // ffprobe funcionará con RTMP siempre que el servidor RTMP ya esté emitiendo
            settings = this.ffprobe(inputFilePath);
            tieneAudio = settings.length > 3 && settings[3] != null && !settings[3].isEmpty();
        }

        int inputWidth = Integer.parseInt(settings[0]);
        int inputHeight = Integer.parseInt(settings[1]);
        int inputFps = Integer.parseInt(settings[2]);

        // 2. Calcular perfiles y detectar aceleración
        List<ResolutionProfile> resolutionPairs = this.calculateResolutionPairs(inputWidth * inputHeight, inputFps);
        GPUDetector.VideoAcceleration accel = GPUDetector.detectAcceleration();

        // 3. Construcción del comando base
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.addAll(List.of("-loglevel", "info"));

        applyHardwareAcceleration(command, accel);
        configureInputSource(command, inputFilePath, live, settings);

        // 4. Filtros y Mapeos (Usando la detección de audio anterior)
        command.add("-filter_complex");
        command.addAll(resolveFilters(accel, resolutionPairs));

        command.add("-var_stream_map");
        command.add(buildStreamMap(resolutionPairs, tieneAudio));

        // 5. Configuración HLS
        String hlsPlaylistType = live ? "event" : "vod";
        String hlsFlags = live ? "independent_segments+append_list+program_date_time" : "independent_segments";

        command.addAll(List.of(
                "-master_pl_name", "master.m3u8",
                "-f", "hls",
                "-hls_time", "2",
                "-hls_playlist_type", hlsPlaylistType,
                "-hls_flags", hlsFlags,
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", "%v/data%05d.ts",
                "%v/stream.m3u8"));

        // Guardar original si es live (RTMP o WebRTC)
        if (live) {
            command.addAll(List.of("-map", "0:v?", "-map", "0:a?", "-c:v", "copy", "-c:a", "aac", "original.mp4"));
        }

        if (logger.isInfoEnabled()) {
            logger.info("Comando FFmpeg generado para {}: {}", inputFilePath, String.join(" ", command));
        }

        return command;
    }

    /**
     * Función para aplicar la aceleración de hardware
     * 
     * @param command List<String> con el comando FFmpeg
     * @param accel   GPUDetector.VideoAcceleration con la aceleración de hardware
     */
    protected void applyHardwareAcceleration(List<String> command, GPUDetector.VideoAcceleration accel) {
        switch (accel) {
            case VAAPI -> command.addAll(List.of("-vaapi_device", "/dev/dri/renderD128"));
            case NVIDIA -> command.addAll(List.of("-hwaccel", "cuda", "-hwaccel_output_format", "cuda"));
            default -> {
                // No hacemos nada
            }
        }
    }

    /**
     * Función para configurar la fuente de entrada
     * 
     * @param command  List<String> con el comando FFmpeg
     * @param path     String con la ruta del flujo
     * @param live     Booleano con el tipo de transmisión
     * @param settings String[] con la configuración de la transmisión
     */
    protected void configureInputSource(List<String> command, String path, boolean live, String[] settings) {
        if (live && !path.contains("pipe:")) {
            command.add("-re");
        }

        if (path.contains("pipe:") && settings != null) {
            command.addAll(List.of(
                    "-analyzeduration", "10000000", "-probesize", "5000000",
                    "-protocol_whitelist", "file,udp,rtp,stdin,fd",
                    "-f", "sdp", "-i", "-",
                    "-fflags", "+genpts", "-use_wallclock_as_timestamps", "1",
                    "-fps_mode", "passthrough"));
        } else {
            command.addAll(List.of("-i", path));
        }
    }

    /**
     * Función para resolver los filtros
     * 
     * @param accel    GPUDetector.VideoAcceleration con la aceleración de hardware
     * @param profiles List<ResolutionProfile> con los perfiles de resolución
     * @return List<String> con los filtros FFmpeg
     * @throws IOException
     * @throws InterruptedException
     */
    protected List<String> resolveFilters(GPUDetector.VideoAcceleration accel, List<ResolutionProfile> profiles) {
        return switch (accel) {
            case VAAPI -> createIntelGPUFilter(profiles);
            case NVIDIA -> createNvidiaGPUFilter(profiles);
            default -> createCPUFilter(profiles);
        };
    }

    /**
     * Función para construir el stream map
     * 
     * @param profiles   List<ResolutionProfile> con los perfiles de resolución
     * @param tieneAudio Booleano con si hay audio
     * @return String con el stream map
     */
    protected String buildStreamMap(List<ResolutionProfile> profiles, boolean tieneAudio) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < profiles.size(); i++) {
            ResolutionProfile p = profiles.get(i);
            if (i > 0)
                sb.append(" ");

            // Si no hay audio, NO incluimos la referencia 'a:' en el stream map
            if (tieneAudio) {
                sb.append(String.format("v:%d,a:%d,name:%dx%d@%d", i, i, p.getWidth(), p.getHeight(), p.getFps()));
            } else {
                sb.append(String.format("v:%d,name:%dx%d@%d", i, p.getWidth(), p.getHeight(), p.getFps()));
            }
        }
        return sb.toString();
    }

    /**
     * Función para enviar el SDP a FFmpeg
     * 
     * @param process   Process con el proceso FFmpeg
     * @param sdpStream InputStream con el flujo de entrada
     * @return Booleano con el resultado de la operación
     * @throws IOException
     */
    protected boolean sendSDP(Process process, InputStream sdpStream) throws IOException {
        try {
            logger.info("➡️ Enviando SDP a FFmpeg...");
            OutputStream os = process.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = sdpStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            os.close();
            logger.info("✅ SDP enviado y stdin cerrado");
            return true;
        } catch (IOException e) {
            logger.error("Error al enviar SDP a FFmpeg: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Función para obtener la información de la resolución del video
     * 
     * @param inputFilePath String con la ruta del flujo
     * @return String[] con la información de la resolución del video
     * @throws IOException
     * @throws InterruptedException
     * @throws InternalServerException
     */
    protected String[] ffprobe(String inputFilePath)
            throws InternalServerException {
        String width = null;
        String height = null;
        String fps = null;
        String audioCodec = null;

        logger.info("Obteniendo la resolución del video con ffprobe");
        // Buscamos info de video y audio simultáneamente
        ProcessBuilder processBuilder = new ProcessBuilder("ffprobe",
                "-v", "error",
                "-show_entries", "stream=width,height,r_frame_rate,codec_type,codec_name",
                "-of", "csv=p=0", inputFilePath);
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
        } catch (Exception e) {
            logger.error("Error al ejecutar ffprobe: {}", e.getMessage());
            throw new InternalServerException("Error al ejecutar ffprobe");
        }

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("FFProbe line: {}", line);
                    String[] parts = line.split(",");
                    if (line.contains("video")) {
                        width = parts[2];
                        height = parts[3];
                        String[] frameRateParts = parts[4].split("/");
                        if (frameRateParts.length == 2) {
                            fps = String.valueOf((int) Math
                                    .round(Double.parseDouble(frameRateParts[0])
                                            / Double.parseDouble(frameRateParts[1])));
                        }
                    } else if (line.contains("audio")) {
                        audioCodec = parts[1]; // codec_name
                    }
                }
            }

            process.waitFor();
            if (width == null || height == null || fps == null) {
                throw new InternalServerException("No se pudo obtener la resolución del streaming");
            }

            logger.info("Resolución: {}x{}@{} | Audio: {}", width, height, fps,
                    (audioCodec != null ? audioCodec : "NONE"));
            return new String[] { width, height, fps, audioCodec };
        } catch (IOException e) {
            throw new InternalServerException("Error al leer la salida de ffprobe: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("FFprobe se interrumpió: " + e.getMessage());
        }
    }

    /**
     * Función para calcular los perfiles de resolución
     * 
     * @param totalPixels Integer con el número total de píxeles
     * @param inputFps    Integer con el FPS del flujo
     * @return List<ResolutionProfile> con los perfiles de resolución
     */
    protected List<ResolutionProfile> calculateResolutionPairs(int totalPixels, int inputFps) {
        return Arrays.stream(ResolutionProfile.values())
                .filter(r -> r.getWidth() * r.getHeight() <= totalPixels
                        && (r.getFps() == 30 || r.getFps() <= inputFps))
                .sorted((a, b) -> Integer.compare(b.getWidth() * b.getHeight(), a.getWidth() * a.getHeight()))
                .distinct().toList();
    }

    /**
     * Función para crear el filtro CPU
     * 
     * @param profiles List<ResolutionProfile> con los perfiles de resolución
     * @return List<String> con los filtros FFmpeg
     */
    protected List<String> createCPUFilter(List<ResolutionProfile> profiles) {
        if (profiles == null || profiles.isEmpty())
            return Collections.emptyList();
        int n = profiles.size();
        List<String> filters = new ArrayList<>();
        StringBuilder graph = new StringBuilder();
        graph.append("[0:v]split=").append(n);
        for (int i = 0; i < n; i++)
            graph.append("[v").append(i + 1).append("]");
        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = profiles.get(i);
            graph.append(";[v").append(i + 1).append("]scale=w=").append(rp.getWidth()).append(":h=")
                    .append(rp.getHeight()).append("[v").append(i + 1).append("out]");
        }
        filters.add(graph.toString());

        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = profiles.get(i);
            filters.addAll(Arrays.asList("-map", "[v" + (i + 1) + "out]", "-c:v:" + i, "libx264", "-profile:v:" + i,
                    rp.getProfile(), "-level:v:" + i, rp.getLevel(), "-b:v:" + i, rp.getBitrate(), "-maxrate:v:" + i,
                    rp.getMaxrate(), "-bufsize:v:" + i, rp.getBufsize(), "-g", String.valueOf(rp.getFps()),
                    "-keyint_min", String.valueOf(rp.getFps())));
        }

        for (int i = 0; i < n; i++) {
            filters.addAll(Arrays.asList("-map", "0:a:0?", "-c:a:" + i, "aac", "-b:a:" + i,
                    profiles.get(i).getAudioBitrate()));
            if (i == 0)
                filters.addAll(Arrays.asList("-ac", "2"));
        }
        return filters;
    }

    /**
     * Función para crear el filtro GPU Intel
     * 
     * @param profiles List<ResolutionProfile> con los perfiles de resolución
     * @return List<String> con los filtros FFmpeg
     */
    protected List<String> createIntelGPUFilter(List<ResolutionProfile> profiles) {
        List<String> filters = new ArrayList<>();
        // Forzamos nv12 para compatibilidad con streams yuv444p
        StringBuilder filtroBuilder = new StringBuilder("[0:v]format=nv12,hwupload,split=");
        filtroBuilder.append(profiles.size());

        for (int i = 0; i < profiles.size(); i++)
            filtroBuilder.append("[v").append(i + 1).append("]");
        for (int i = 0; i < profiles.size(); i++) {
            filtroBuilder.append("; [v").append(i + 1).append("]scale_vaapi=w=").append(profiles.get(i).getWidth())
                    .append(":h=").append(profiles.get(i).getHeight()).append("[v").append(i + 1).append("out]");
        }
        filters.add(filtroBuilder.toString());

        for (int i = 0; i < profiles.size(); i++) {
            ResolutionProfile rp = profiles.get(i);
            filters.addAll(Arrays.asList("-map", "[v" + (i + 1) + "out]", "-c:v:" + i, "h264_vaapi", "-profile:v:" + i,
                    rp.getProfile(), "-level:v:" + i, rp.getLevel(), "-b:v:" + i, rp.getBitrate(), "-maxrate:v:" + i,
                    rp.getMaxrate(), "-bufsize:v:" + i, rp.getBufsize(), "-g", String.valueOf(rp.getFps()),
                    "-keyint_min", String.valueOf(rp.getFps())));
        }

        for (int i = 0; i < profiles.size(); i++) {
            // Usamos '?' para que no falle si no hay audio, aunque en var_stream_map ya lo
            // gestionamos
            filters.addAll(Arrays.asList("-map", "0:a:0?", "-c:a:" + i, "aac", "-b:a:" + i,
                    profiles.get(i).getAudioBitrate()));
            if (i == 0)
                filters.addAll(Arrays.asList("-ac", "2"));
        }
        return filters;
    }

    /**
     * Función para crear el filtro GPU NVIDIA
     * 
     * @param profiles List<ResolutionProfile> con los perfiles de resolución
     * @return List<String> con los filtros FFmpeg
     */
    protected List<String> createNvidiaGPUFilter(List<ResolutionProfile> profiles) {
        if (profiles == null || profiles.isEmpty())
            return Collections.emptyList();
        List<String> filters = new ArrayList<>();
        int n = profiles.size();
        StringBuilder filtro = new StringBuilder();
        filtro.append("[0:v]format=nv12,hwupload_cuda,split=").append(n);
        for (int i = 0; i < n; i++)
            filtro.append("[v").append(i + 1).append("]");
        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = profiles.get(i);
            filtro.append(";[v").append(i + 1).append("]").append("scale_cuda=w=").append(rp.getWidth()).append(":h=")
                    .append(rp.getHeight()).append("[v").append(i + 1).append("out]");
        }
        filters.add(filtro.toString());

        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = profiles.get(i);
            filters.addAll(Arrays.asList("-map", "[v" + (i + 1) + "out]", "-c:v:" + i, "h264_nvenc", "-profile:v:" + i,
                    rp.getProfile(), "-level:v:" + i, rp.getLevel(), "-b:v:" + i, rp.getBitrate(), "-maxrate:v:" + i,
                    rp.getMaxrate(), "-bufsize:v:" + i, rp.getBufsize(), "-g", String.valueOf(rp.getFps()),
                    "-keyint_min", String.valueOf(rp.getFps())));
        }

        for (int i = 0; i < n; i++) {
            filters.addAll(Arrays.asList("-map", "0:a:0?", "-c:a:" + i, "aac", "-b:a:" + i,
                    profiles.get(i).getAudioBitrate()));
            if (i == 0)
                filters.addAll(Arrays.asList("-ac", "2"));
        }
        return filters;
    }

    /**
     * Función para procesar una clase
     * 
     * @param curso         Curso con los videos
     * @param clase         Clase con los videos
     * @param baseUploadDir Path con la ruta de carga de archivos
     * @throws IOException
     * @throws InterruptedException
     * @throws InternalServerException
     */
    protected void processSingleClase(Curso curso, Clase clase, Path baseUploadDir, Path destinationPath)
            throws InternalServerException {

        Path inputPath = Paths.get(clase.getDireccionClase());
        Path targetPath;
        try {
            Files.createDirectories(destinationPath);
            targetPath = destinationPath.resolve(inputPath.getFileName());
            Files.move(inputPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            logger.error("Error en mover el video de la clase {}: {}", clase.getIdClase(), e.getMessage());
            throw new InternalServerException("Error en mover el video de la clase " + clase.getIdClase());
        }

        List<String> ffmpegCommand = this.creaComandoFFmpeg(targetPath.toAbsolutePath().toString(), false, null);

        if (ffmpegCommand != null) {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);
            processBuilder.directory(destinationPath.toFile());
            processBuilder.redirectErrorStream(true);
            try {
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("FFmpeg [Clase " + clase.getIdClase() + "]: " + line);
                    }
                } catch (IOException e) {
                    process.destroy();
                    logger.error("Error leyendo salida de FFmpeg en clase {}: {}", clase.getIdClase(), e.getMessage());
                    return;
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logger.warn("FFmpeg terminó con errores (code {}) en clase {}", exitCode, clase.getIdClase());
                    return;
                }

                clase.setDireccionClase(destinationPath.resolve("master.m3u8").toString());
                clase.setCursoClase(curso);
                this.claseRepo.save(clase);
                logger.info("Clase {} convertida con éxito.", clase.getIdClase());
            } catch (IOException e) {
                logger.error("Error al convertir la clase {}: {}", clase.getIdClase(), e.getMessage());
                throw new InternalServerException("Error al convertir la clase " + clase.getIdClase());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error al convertir la clase {}: {}", clase.getIdClase(), e.getMessage());
                throw new InternalServerException("Error al convertir la clase " + clase.getIdClase());
            }
        }
    }
}