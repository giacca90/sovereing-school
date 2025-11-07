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

import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_streaming.Models.ResolutionProfile;

@Service
@Transactional
public class StreamingService {
    private final Map<String, Process> ffmpegProcesses = new ConcurrentHashMap<>();

    private ClaseRepository claseRepo;

    private Logger logger = LoggerFactory.getLogger(StreamingService.class);

    @Value("${variable.rtmp}")
    private String rtmp;

    @Value("${variable.VIDEOS_DIR}")
    private String uploadDir;

    public StreamingService(ClaseRepository claseRepo) {
        this.claseRepo = claseRepo;
    }

    /**
     * Función para convertir los videos de un curso
     * 
     * @param curso
     * @throws IOException
     * @throws InterruptedException
     * @throws NotFoundException
     */
    @Async
    // TODO: Modificar para trabajar solo con clases concretas
    public void convertVideos(Curso curso) throws IOException, InterruptedException, NotFoundException {
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
            if (!clase.getDireccionClase().isEmpty()
                    && !clase.getDireccionClase().contains(destinationPath.toString())
                    && !clase.getDireccionClase().endsWith(".m3u8")
                    && clase.getDireccionClase().contains(".")) {

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
                ffmpegCommand = this.creaComandoFFmpeg(destino.getAbsolutePath(), false, null, null);
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
                            System.err.println("FFmpeg: " + line);
                        }
                    } catch (IOException e) {
                        logger.error("Error leyendo salida de FFmpeg: {}", e.getMessage());
                        process.destroy();
                        throw new RuntimeException("Error leyendo salida de FFmpeg: " + e.getMessage());
                    }

                    int exitCode = process.waitFor();
                    logger.info("Salida del proceso de FFmpeg: {}", exitCode);
                    if (exitCode != 0) {
                        throw new IOException("El proceso de FFmpeg falló con el código de salida " + exitCode);
                    }

                    clase.setDireccionClase(destinationPath.toString() + "/master.m3u8");
                    this.claseRepo.save(clase);
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
     * @throws Exception
     */
    public void startLiveStreamingFromStream(String streamId, Object inputStream, String[] videoSetting)
            throws IOException, InterruptedException, RuntimeException, IllegalArgumentException {
        Optional<Clase> claseOpt = claseRepo.findByDireccionClase(streamId);
        if (!claseOpt.isPresent())
            throw new RuntimeException("No se encuentra la clase con la dirección " + streamId);
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
        if (inputStream instanceof String) {
            ffmpegCommand = this.creaComandoFFmpeg((String) inputStream, true, streamId, videoSetting);
        } else if (inputStream instanceof InputStream) {
            ffmpegCommand = this.creaComandoFFmpeg("pipe:0", true, streamId, videoSetting);
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
                    System.err.println("FFmpeg: " + line);

                    // Enviar SDP a FFmpeg (solo para WebRTC)
                    if (!sdpSent && inputStream instanceof InputStream && line.contains("ffmpeg version")) {
                        try {
                            logger.info("➡️ Enviando SDP a FFmpeg...");
                            OutputStream os = process.getOutputStream();
                            InputStream sdpStream = (InputStream) inputStream;

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
                }
            } catch (IOException e) {
                logger.error("Error leyendo salida de FFmpeg: {}", e.getMessage());
                throw new RuntimeException("Error leyendo salida de FFmpeg: " + e.getMessage());
            }
        });
        logReader.start();
        // logReader.join(); // Esperar a que se terminen de leer los logs
    }

    public void stopFFmpegProcessForUser(String streamId) throws IOException, RuntimeException {
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
            throw new RuntimeException("El proceso fue interrumpido: " + e.getMessage(), e);
        }
    }

    public Path getPreview(String idPreview) {
        Path baseUploadDir = Paths.get(uploadDir);
        Path previewDir = baseUploadDir.resolve("previews");
        Path m3u8 = previewDir.resolve(idPreview + ".m3u8");
        // Espera a que se genere el preview
        while (!Files.exists(m3u8)) {
            // Espera medio segundo y reintenta
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("Error al esperar a que se genere el preview: {}", e.getMessage());
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error al esperar a que se genere el preview: " + e.getMessage());
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
    private List<String> creaComandoFFmpeg(String inputFilePath, Boolean live, String streamId, String[] videoSetting)
            throws IOException, InterruptedException, RuntimeException {
        String hlsPlaylistType = live ? "event" : "vod";
        String hlsFlags = live ? "independent_segments+append_list+program_date_time" : "independent_segments";
        // String preset = live ? "veryfast" : "fast"; // No se usa en VAAPI
        String width = null;
        String height = null;
        String fps = null;
        if (videoSetting != null) {
            width = videoSetting[0];
            height = videoSetting[1];
            fps = videoSetting[2];
        }
        // Obtener la resolución del video
        if (width == null || height == null || fps == null) {
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
            if (width.equals("0") || height.equals("0") || fps.equals("0")) {
                logger.error("La resolución es 0");
                throw new RuntimeException("No se pudo obtener la resolución del streaming");
            }
            if (width == null || height == null || fps == null) {
                logger.error("La resolución es null, reintentando con ffprobe");
                this.creaComandoFFmpeg(inputFilePath, live, streamId, videoSetting);
            }
            logger.info("Resolución: {}x{}@{}", width, height, fps);
        }

        int inputWidth = Integer.parseInt(width);
        int inputHeight = Integer.parseInt(height);
        int inputFps = Integer.parseInt(fps);
        int totalPixels = inputWidth * inputHeight;

        // Filtra todos los perfiles cuya área sea ≤ la del input
        // y cuyo fps sea 30 (fallback) o menor o igual al input
        List<ResolutionProfile> resolutionPairs = Arrays.stream(ResolutionProfile.values())
                .filter(r -> r.getWidth() * r.getHeight() <= totalPixels &&
                        (r.getFps() == 30 || r.getFps() <= inputFps))
                // Ordena de mayor a menor resolución
                .sorted((a, b) -> Integer.compare(
                        b.getWidth() * b.getHeight(),
                        a.getWidth() * a.getHeight()))
                // Elimina duplicados si los hubiera
                .distinct().toList();

        // Debug
        /*
         * resolutionPairs.forEach(r -> System.out.println("Perfil: " +
         * r.getWidth() + "x" + r.getHeight() +
         * "@" + r.getFps() + "fps → br=" + r.getBitrate() + "kbps"));
         */
        // Crear los filtros
        List<String> filters = new ArrayList<>();

        String filtro = "[0:v]format=nv12,hwupload,split=" + resolutionPairs.size();
        for (int i = 0; i < resolutionPairs.size(); i++) {
            filtro += "[v" + (i + 1) + "]";
        }
        /*
         * versión sin VAAPI
         * 
         * for (int i = 0; i < resolutionPairs.size(); i++) {
         * if (i == 0) {
         * filtro += " [v1]copy[v1out]";
         * } else {
         * filtro += "; [v" + (i + 1) + "]scale=w=" +
         * resolutionPairs.get(i).split(",")[0] + ":h="
         * + resolutionPairs.get(i).split(",")[1] + "[v" + (i + 1) + "out]";
         * }
         * }
         */
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

        // Crea el comando FFmpeg
        List<String> ffmpegCommand = new ArrayList<>();
        ffmpegCommand = new ArrayList<>(List.of(
                "ffmpeg", "-loglevel", "info",
                "-vaapi_device", "/dev/dri/renderD128" // Dispositivo VAAPI
        // "-hwaccel", "vaapi", "-hwaccel_output_format", "vaapi" // Acceleration para
        // VAAPI
        ));

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

        ffmpegCommand.addAll(List.of(
                "-filter_complex"));
        ffmpegCommand.addAll(filters);
        ffmpegCommand.addAll(List.of("-var_stream_map"));
        String streamMap = "";
        for (int i = 0; i < resolutionPairs.size(); i++) {
            int Width = resolutionPairs.get(i).getWidth();
            int Height = resolutionPairs.get(i).getHeight();
            int fpsn = resolutionPairs.get(i).getFps();
            streamMap += " v:" + i + ",a:" + i + ",name:" + Width + "x" + Height + "@" + fpsn;
        }
        ffmpegCommand.add(streamMap);

        ffmpegCommand.addAll(List.of("-master_pl_name", "master.m3u8",
                "-f", "hls",
                "-hls_time", "2",
                "-hls_playlist_type", hlsPlaylistType,
                "-hls_flags", hlsFlags,
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", "%v/data%05d.ts",
                "%v/stream.m3u8"));

        if (live) {
            ffmpegCommand.addAll(List.of("-map", "0:v?", "-map", "0:a?", "-c:v", "copy",
                    "-c:a", "aac", "original.mp4"));
        }

        logger.info("Comando FFmpeg: {}", String.join(" ", ffmpegCommand));
        return ffmpegCommand;
    }
}
