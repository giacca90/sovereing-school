package com.sovereingschool.back_streaming.Services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    }

    /**
     * Función para convertir los videos de un curso
     * 
     * @param curso
     * @throws IOException
     * @throws InterruptedException
     * @throws NotFoundException
     * @throws InternalServerException
     */
    @Async
    // TODO: Modificar para trabajar solo con clases concretas
    public void convertVideos(Curso curso)
            throws IOException, InterruptedException, NotFoundException, InternalServerException {
        if (curso.getClasesCurso() == null || curso.getClasesCurso().isEmpty()) {
            throw new NotFoundException("Curso sin clases");
        }
        Path baseUploadDir = Paths.get(uploadDir);
        for (Clase clase : curso.getClasesCurso()) {
            Path destinationPath = Paths.get(baseUploadDir.toString(), curso.getIdCurso().toString(),
                    clase.getIdClase().toString());

            // Verificar que la dirección de la clase no esté vacía y que sea diferente de
            // la
            // base
            String direccion = clase.getDireccionClase();
            if (!direccion.isEmpty()
                    && !direccion.contains(destinationPath.toString())
                    && !direccion.endsWith(".m3u8")
                    && direccion.contains(".")) {

                // Extraer el directorio y el nombre del archivo de entrada
                Path inputPath = Paths.get(clase.getDireccionClase());
                File destinationFile = destinationPath.toFile();
                File inputFile = inputPath.toFile();
                File destino = new File(destinationFile, inputPath.getFileName().toString());

                // Mover el archivo de entrada a la carpeta de destino
                if (!inputFile.renameTo(destino)) {
                    // System.err.println("Error al mover el video a la carpeta de destino");
                    continue;
                }
                List<String> ffmpegCommand = null;
                ffmpegCommand = this.creaComandoFFmpeg(destino.getAbsolutePath(), false, null);
                if (ffmpegCommand != null) {
                    // Ejecutar el comando FFmpeg
                    ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);

                    // Establecer el directorio de trabajo
                    processBuilder.directory(destinationFile);
                    processBuilder.redirectErrorStream(true);

                    Process process = processBuilder.start();

                    // Leer la salida del proceso
                    try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("FFmpeg: " + line);
                        }
                    } catch (IOException e) {
                        process.destroy();
                        throw new InternalServerException("Error leyendo salida de FFmpeg: " + e.getMessage());
                    }

                    int exitCode = process.waitFor();
                    logger.info("Salida del proceso de FFmpeg: {}", exitCode);
                    if (exitCode != 0) {
                        throw new IOException("El proceso de FFmpeg falló con el código de salida " + exitCode);
                    }

                    clase.setDireccionClase(destinationPath.toString() + "/master.m3u8");
                    clase.setCursoClase(curso);
                    clase = this.claseRepo.save(clase);
                }
            }
        }
    }

    /**
     * Función para iniciar la transmisión en vivo
     * 
     * @param streamId     String: identificador del streaming en directo
     *                     (idUsuario_idSession)
     * @param inputStream  String: flujo de entrada del video para ffmpeg
     * @param videoSetting String[]: configuración del video (ancho, alto, fps)
     * @throws IOException
     * @throws InterruptedException
     * @throws RepositoryException
     * @throws Exception
     */
    public void startLiveStreamingFromStream(String streamId, Object inputStream, String[] videoSetting)
            throws IOException, InterruptedException, RuntimeException, IllegalArgumentException, RepositoryException {
        Optional<Clase> claseOpt = claseRepo.findByDireccionClase(streamId);
        if (!claseOpt.isPresent())
            throw new RepositoryException("No se encuentra la clase con la dirección " + streamId);
        Clase clase = claseOpt.get();
        Long idCurso = clase.getCursoClase().getIdCurso();
        Long idClase = clase.getIdClase();
        Path baseUploadDir = Paths.get(uploadDir);
        Path outputDir = baseUploadDir.resolve(idCurso.toString()).resolve(idClase.toString());
        claseRepo.updateClase(idClase, clase.getNombreClase(), clase.getTipoClase(),
                outputDir.toString() + "/master.m3u8",
                clase.getPosicionClase());
        // Crear el directorio de salida si no existe
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Comando FFmpeg para procesar el streaming
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
        // Guardar el proceso en el mapa
        ffmpegProcesses.put(streamId.substring(streamId.lastIndexOf("_") + 1), process);

        // Hilo para leer los logs de FFmpeg
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread logReader = new Thread(() -> {
            try {
                String line;
                boolean sdpSent = false;

                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg: " + line);

                    // Enviar SDP a FFmpeg (solo para WebRTC)
                    if (!sdpSent && inputStream instanceof InputStream sdpStream && line.contains("ffmpeg version")) {
                        this.sendSDP(process, sdpStream, sdpSent);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error leyendo salida de FFmpeg: " + e.getMessage());
            }
        });
        logReader.start();
        // logReader.join(); // Esperar a que se terminen de leer los logs
    }

    /**
     * Función para detener el proceso FFmpeg para un usuario
     * 
     * @param streamId ID del streaming
     * @throws RuntimeException
     * @throws InternalServerException
     */
    public void stopFFmpegProcessForUser(String streamId)
            throws InternalServerException {
        String sessionId = streamId.substring(streamId.lastIndexOf('_') + 1);
        Process process = ffmpegProcesses.remove(sessionId);

        if (process == null) {
            logger.error("⚠️ No se encontró proceso FFmpeg activo para sessionId={}", sessionId);
            return;
        }

        if (!process.isAlive()) {
            logger.info("✅ FFmpeg ya estaba detenido para sessionId={}", sessionId);
            return;
        }

        try {
            logger.info("🛑 Deteniendo FFmpeg enviando SIGTERM...");
            process.destroy(); // Señal de terminación suave

            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                logger.error("⏰ FFmpeg no respondió, forzando terminación (SIGKILL)...");
                process.destroyForcibly();
            } else {
                logger.info("✅ FFmpeg detenido correctamente (exitCode={})", process.exitValue());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("El proceso fue interrumpido: " + e.getMessage(), e);
        }
    }

    /**
     * Función para obtener el preview del streaming
     * 
     * @param idPreview ID del preview
     * @return Path con la ruta del preview
     * @throws InternalServerException
     */
    public Path getPreview(String idPreview) throws InternalServerException {
        Path baseUploadDir = Paths.get(uploadDir);
        Path previewDir = baseUploadDir.resolve("previews");
        Path m3u8 = previewDir.resolve(idPreview + ".m3u8");
        // Espera a que se genere el preview
        while (!Files.exists(m3u8)) {
            // Espera medio segundo y reintenta
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InternalServerException("Error al esperar a que se genere el preview: " + e.getMessage());
            }
        }
        return m3u8;
    }

    /**
     * Función para generar el comando ffmpeg.
     * El comando debe ser ejecutado en la carpeta de salida.
     * 
     * @param inputFilePath String: dirección del video original
     * @param live          Boolean: bandera para eventos en vivo
     * @param streamId      String: identificador del streamong en directo
     *                      (isUsuario_idSession)
     * @param videoSetting  String[]: configuración del video (ancho, alto, fps)
     * @return List<String>: el comando generado
     * @throws IOException
     * @throws InterruptedException
     */
    protected List<String> creaComandoFFmpeg(String inputFilePath, boolean live, String[] videoSetting)
            throws IOException, InterruptedException, RuntimeException {

        String hlsPlaylistType = live ? "event" : "vod";
        String hlsFlags = live ? "independent_segments+append_list+program_date_time" : "independent_segments";

        String width = null;
        String height = null;
        String fps = null;
        if (videoSetting != null) {
            width = videoSetting[0];
            height = videoSetting[1];
            fps = videoSetting[2];
        }

        // Obtener la resolución si falta
        if (width == null || height == null || fps == null) {
            videoSetting = this.ffprobe(inputFilePath);
            width = videoSetting[0];
            height = videoSetting[1];
            fps = videoSetting[2];
        }

        int inputWidth = Integer.parseInt(width);
        int inputHeight = Integer.parseInt(height);
        int inputFps = Integer.parseInt(fps);
        int totalPixels = inputWidth * inputHeight;

        // calcular perfiles a usar
        List<ResolutionProfile> resolutionPairs = this.calculateResolutionPairs(totalPixels, inputFps);

        // Detectar aceleración una sola vez
        GPUDetector.VideoAcceleration accel = GPUDetector.detectAcceleration();

        // Crear los filtros según aceleración
        List<String> filters;
        switch (accel) {
            case VAAPI:
                filters = createIntelGPUFilter(resolutionPairs);
                break;
            case NVIDIA:
                filters = createNvidiaGPUFilter(resolutionPairs);
                break;
            case CPU:
            default:
                filters = createCPUFilter(resolutionPairs);
                break;
        }

        // Base del comando
        List<String> ffmpegCommand = new ArrayList<>();
        ffmpegCommand.add("ffmpeg");
        ffmpegCommand.add("-loglevel");
        ffmpegCommand.add("info");

        // Opciones específicas de hw antes del -i
        switch (accel) {
            case VAAPI:
                // Vaapi: especificamos dispositivo VAAPI (debe estar presente y montado)
                ffmpegCommand.addAll(List.of("-vaapi_device", "/dev/dri/renderD128"));
                break;
            case NVIDIA:
                // NVIDIA: habilitar hwaccel y salida en formato cuda. Puede necesitar ajustes
                // si el ffmpeg/driver requiere init_hw_device/filter_hw_device.
                ffmpegCommand.addAll(List.of("-hwaccel", "cuda", "-hwaccel_output_format", "cuda"));
                break;
            case CPU:
                // CPU: no añadimos -hwaccel (no existe -hwaccel cpu)
                break;
        }

        // Lectura input / opciones para pipe
        if (live && !inputFilePath.contains("pipe:")) {
            ffmpegCommand.add("-re");
        }

        if (inputFilePath.contains("pipe:") && videoSetting != null) {
            ffmpegCommand.addAll(List.of("-analyzeduration", "10000000",
                    "-probesize", "5000000", "-protocol_whitelist",
                    "file,udp,rtp,stdin,fd",
                    "-f", "sdp", "-i", "-",
                    "-fflags", "+genpts",
                    "-use_wallclock_as_timestamps", "1",
                    "-fps_mode", "passthrough"));
        } else {
            ffmpegCommand.addAll(List.of("-i", inputFilePath));
        }

        // Añadir filter_complex: asume que el primer elemento de filters es la cadena
        // del grafo
        ffmpegCommand.add("-filter_complex");
        ffmpegCommand.addAll(filters);

        // Construir var_stream_map (sin espacio inicial)
        StringBuilder streamMap = new StringBuilder();
        for (int i = 0; i < resolutionPairs.size(); i++) {
            int w = resolutionPairs.get(i).getWidth();
            int h = resolutionPairs.get(i).getHeight();
            int fpsn = resolutionPairs.get(i).getFps();
            if (streamMap.length() > 0)
                streamMap.append(" ");
            streamMap.append("v:").append(i).append(",a:").append(i).append(",name:")
                    .append(w).append("x").append(h).append("@").append(fpsn);
        }
        ffmpegCommand.addAll(List.of("-var_stream_map", streamMap.toString()));

        // Opciones de salida HLS (maestro)
        ffmpegCommand.addAll(List.of("-master_pl_name", "master.m3u8",
                "-f", "hls",
                "-hls_time", "2",
                "-hls_playlist_type", hlsPlaylistType,
                "-hls_flags", hlsFlags,
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", "%v/data%05d.ts",
                "%v/stream.m3u8"));

        // Si quieres además guardar un original.mp4 cuando live
        if (live) {
            // Nota: usar -map y -c:v copy produce un segundo output; en este caso
            // las opciones se aplican a ese output concreto.
            ffmpegCommand
                    .addAll(List.of("-map", "0:v?", "-map", "0:a?", "-c:v", "copy", "-c:a", "aac", "original.mp4"));
        }

        logger.info("Comando FFmpeg: {}", String.join(" ", ffmpegCommand));
        return ffmpegCommand;
    }

    /**
     * Función para enviar el SDP a FFmpeg
     * 
     * @param process   Proceso de FFmpeg
     * @param sdpStream Flujo de entrada del SDP
     * @param sdpSent   Booleano para indicar si se ha enviado el SDP
     * @throws IOException
     */
    protected void sendSDP(Process process, InputStream sdpStream, Boolean sdpSent) throws IOException {
        try {
            logger.info("➡️ Enviando SDP a FFmpeg...");
            OutputStream os = process.getOutputStream();

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = sdpStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            os.close(); // cerramos stdin, el SDP ya está completo

            sdpSent = true;
            logger.info("✅ SDP enviado y stdin cerrado");
        } catch (IOException e) {
            logger.error("Error al enviar SDP a FFmpeg: {}", e.getMessage());
        }
    }

    /**
     * Función para obtener la resolución del video con ffprobe
     * 
     * @param inputFilePath String: dirección del video original
     * @return String[] con la resolución del video
     * @throws IOException
     * @throws RuntimeException
     * @throws InterruptedException
     */
    protected String[] ffprobe(String inputFilePath) throws IOException, RuntimeException, InterruptedException {
        String width = null;
        String height = null;
        String fps = null;
        logger.info("Obteniendo la resolución del video con ffprobe");
        ProcessBuilder processBuilder = new ProcessBuilder("ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height,r_frame_rate",
                "-of", "csv=p=0", inputFilePath);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {
            logger.info("FFProbe: {}", line);
            String[] parts = line.split(",");
            width = parts[0];
            height = parts[1];

            // Para calcular los fps, "r_frame_rate" devuelve un formato como"30000/1001"
            String[] frameRateParts = parts[2].split("/");
            if (frameRateParts.length == 2) {
                // Redondear el fps a entero
                fps = String.valueOf((int) Math.round(Double.parseDouble(frameRateParts[0]) /
                        Double.parseDouble(frameRateParts[1])));
            }
        }

        process.waitFor();
        if (width == null || height == null || fps == null) {
            logger.error("La resolución es null, reintentando con ffprobe");
            this.ffprobe(inputFilePath);
        } else if (width.equals("0") || height.equals("0") || fps.equals("0")) {
            logger.error("La resolución es 0");
            throw new RuntimeException("No se pudo obtener la resolución del streaming");
        }

        logger.info("Resolución: {}x{}@{}", width, height, fps);
        return new String[] { width, height, fps };
    }

    /**
     * Función para calcular los perfiles de resolución
     * 
     * @param totalPixels Número total de píxeles
     * @param inputFps    FPS del video
     * @return Lista de ResolutionProfile
     */
    protected List<ResolutionProfile> calculateResolutionPairs(int totalPixels, int inputFps) {
        return Arrays.stream(ResolutionProfile.values())
                .filter(r -> r.getWidth() * r.getHeight() <= totalPixels &&
                        (r.getFps() == 30 || r.getFps() <= inputFps))
                // Ordena de mayor a menor resolución
                .sorted((a, b) -> Integer.compare(
                        b.getWidth() * b.getHeight(),
                        a.getWidth() * a.getHeight()))
                // Elimina duplicados si los hubiera
                .distinct().toList();
    }

    /**
     * Función para crear el filtro de CPU
     * 
     * @param resolutionPairs Lista de ResolutionProfile
     * @return String con el filtro de CPU
     */
    protected List<String> createCPUFilter(List<ResolutionProfile> resolutionPairs) {
        // defensa básica
        if (resolutionPairs == null || resolutionPairs.isEmpty()) {
            return Collections.emptyList();
        }

        int n = resolutionPairs.size();
        List<String> filters = new ArrayList<>();

        // 1) Construir el filtergraph: split + scale por salida
        StringBuilder graph = new StringBuilder();
        // split desde la entrada
        graph.append("[0:v]split=").append(n);
        for (int i = 0; i < n; i++) {
            graph.append("[v").append(i + 1).append("]");
        }
        // ahora las escalas (una por each vX -> vXout)
        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = resolutionPairs.get(i);
            graph.append(";");
            graph.append("[v").append(i + 1).append("]");
            graph.append("scale=w=").append(rp.getWidth())
                    .append(":h=").append(rp.getHeight())
                    .append("[v").append(i + 1).append("out]");
        }
        // añadir el grafo como primer elemento de la lista de filtros
        filters.add(graph.toString());

        // 2) Opciones por cada stream de vídeo (map + codec + rates etc.)
        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = resolutionPairs.get(i);
            int fpsn = rp.getFps();

            filters.addAll(Arrays.asList(
                    "-map", "[v" + (i + 1) + "out]",
                    "-c:v:" + i, "libx264", // encoder CPU
                    "-profile:v:" + i, rp.getProfile(),
                    "-level:v:" + i, rp.getLevel(),
                    "-b:v:" + i, rp.getBitrate(),
                    "-maxrate:v:" + i, rp.getMaxrate(),
                    "-bufsize:v:" + i, rp.getBufsize(),
                    "-g", String.valueOf(fpsn),
                    "-keyint_min", String.valueOf(fpsn)));
        }

        // 3) Mapear audio por cada stream (mismo audio 0:a:0 en cada salida)
        for (int i = 0; i < n; i++) {
            filters.addAll(Arrays.asList(
                    "-map", "0:a:0", // map primer audio del input
                    "-c:a:" + i, "aac",
                    "-b:a:" + i, resolutionPairs.get(i).getAudioBitrate()));
            if (i == 0) {
                // Forzar estereo si lo quieres solo una vez (aplica antes de outputs)
                filters.addAll(Arrays.asList("-ac", "2"));
            }
        }

        return filters;
    }

    /**
     * Función para crear el filtro de GPU de Intel
     * 
     * @param resolutionPairs Lista de ResolutionProfile
     * @return Lista de String con el filtro de GPU
     */
    protected List<String> createIntelGPUFilter(List<ResolutionProfile> resolutionPairs) {
        List<String> filters = new ArrayList<>();

        String filtro = "[0:v]format=nv12,hwupload,split=" + resolutionPairs.size();
        for (int i = 0; i < resolutionPairs.size(); i++) {
            filtro += "[v" + (i + 1) + "]";
        }

        for (int i = 0; i < resolutionPairs.size(); i++) {
            filtro += "; [v" + (i + 1) + "]scale_vaapi=w=" + resolutionPairs.get(i).getWidth() + ":h="
                    + resolutionPairs.get(i).getHeight() + "[v" + (i + 1) + "out]";
        }
        filters.add(filtro);

        for (int i = 0; i < resolutionPairs.size(); i++) {
            int fpsn = resolutionPairs.get(i).getFps();
            filters.addAll(Arrays.asList(
                    "-map", "[v" + (i + 1) + "out]",
                    "-c:v:" + i, "h264_vaapi", // o libx264 si no se usa VAAPI
                    // "-qp:v:" + i, "4", // Constante de calidad
                    "-profile:v:" + i, resolutionPairs.get(i).getProfile(),
                    "-level:v:" + i, resolutionPairs.get(i).getLevel(),
                    "-b:v:" + i, resolutionPairs.get(i).getBitrate(),
                    "-maxrate:v:" + i, resolutionPairs.get(i).getMaxrate(),
                    "-bufsize:v:" + i, resolutionPairs.get(i).getBufsize(),
                    "-g", String.valueOf(fpsn), // Conversión explícita de fps a String
                    "-keyint_min", String.valueOf(fpsn)));
        }

        for (int i = 0; i < resolutionPairs.size(); i++) {
            filters.addAll(Arrays.asList("-map", "a:0", "-c:a:" + i, "aac", "-b:a:" + i,
                    resolutionPairs.get(i).getAudioBitrate()));
            if (i == 0) {
                filters.addAll(Arrays.asList("-ac", "2"));
            }
        }

        return filters;
    }

    protected List<String> createNvidiaGPUFilter(List<ResolutionProfile> resolutionPairs) {
        if (resolutionPairs == null || resolutionPairs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> filters = new ArrayList<>();
        int n = resolutionPairs.size();

        // --- Filtro base con split y escalado por hardware
        StringBuilder filtro = new StringBuilder();
        filtro.append("[0:v]format=nv12,hwupload_cuda,split=").append(n);
        for (int i = 0; i < n; i++) {
            filtro.append("[v").append(i + 1).append("]");
        }

        // Escalado por resolución (cada salida independiente)
        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = resolutionPairs.get(i);
            filtro.append(";[v").append(i + 1).append("]")
                    .append("scale_cuda=w=").append(rp.getWidth())
                    .append(":h=").append(rp.getHeight())
                    .append("[v").append(i + 1).append("out]");
        }

        filters.add(filtro.toString());

        // --- Configuración de video (una salida por resolución)
        for (int i = 0; i < n; i++) {
            ResolutionProfile rp = resolutionPairs.get(i);
            int fpsn = rp.getFps();

            filters.addAll(Arrays.asList(
                    "-map", "[v" + (i + 1) + "out]",
                    "-c:v:" + i, "h264_nvenc",
                    "-preset:v:" + i, "p4", // rango p1–p7 (más alto = más calidad)
                    "-profile:v:" + i, rp.getProfile(),
                    "-level:v:" + i, rp.getLevel(),
                    "-b:v:" + i, rp.getBitrate(),
                    "-maxrate:v:" + i, rp.getMaxrate(),
                    "-bufsize:v:" + i, rp.getBufsize(),
                    "-g", String.valueOf(fpsn),
                    "-keyint_min", String.valueOf(fpsn)));
        }

        // --- Configuración de audio (una pista por salida)
        for (int i = 0; i < n; i++) {
            filters.addAll(Arrays.asList(
                    "-map", "0:a:0",
                    "-c:a:" + i, "aac",
                    "-b:a:" + i, resolutionPairs.get(i).getAudioBitrate()));
            if (i == 0) {
                filters.addAll(Arrays.asList("-ac", "2"));
            }
        }

        return filters;
    }

}
