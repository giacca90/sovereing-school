package com.sovereingschool.back_streaming.Services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_streaming.Models.ResolutionProfile;

@Service
@Transactional
public class StreamingService {
    private final Map<String, Process> ffmpegProcesses = new ConcurrentHashMap<>();

    @Autowired
    private ClaseRepository claseRepo;

    @Value("${variable.RTMP}")
    private String RTMP;

    @Value("${variable.VIDEOS_DIR}")
    private String uploadDir;

    /**
     * Función que devuelve un puerto libre en el rango 8010-8059
     */
    public int[] getFreePorts() throws IOException {
        int videoPort = 0;
        int audioPort = 0;

        for (int port = 8010; port <= 8058; port++) { // 8058 porque necesitamos 2 puertos consecutivos
            try (
                    ServerSocket tcp1 = new ServerSocket(port);
                    DatagramSocket udp1 = new DatagramSocket(port);
                    ServerSocket tcp2 = new ServerSocket(port + 1);
                    DatagramSocket udp2 = new DatagramSocket(port + 1)) {
                tcp1.setReuseAddress(true);
                udp1.setReuseAddress(true);
                tcp2.setReuseAddress(true);
                udp2.setReuseAddress(true);

                // Si no lanza excepción, ambos puertos están libres
                videoPort = port;
                audioPort = port + 1;
                return new int[] { videoPort, audioPort };
            } catch (IOException ignored) {
                // Alguno de los puertos está ocupado, seguimos buscando
            }
        }
        throw new IOException("No hay puertos libres disponibles entre 8010 y 8059.");
    }

    /**
     * Función para convertir los videos de un curso
     * 
     * @param curso
     * @throws IOException
     * @throws InterruptedException
     */
    @Async
    // TODO: Modificar para trabajar solo con clases concretas
    public void convertVideos(Curso curso) throws IOException, InterruptedException {
        if (curso.getClases_curso() == null || curso.getClases_curso().isEmpty())
            throw new RuntimeException("Curso sin clases");
        for (Clase clase : curso.getClases_curso()) {
            Path baseUploadDir = Paths.get(uploadDir);
            Path base = Paths.get(baseUploadDir.toString(), curso.getId_curso().toString(),
                    clase.getId_clase().toString());

            // Verificar que la dirección de la clase no esté vacía y que sea diferente dela
            // base
            if (!clase.getDireccion_clase().isEmpty() && !clase.getDireccion_clase().contains(base.toString())
                    && !clase.getDireccion_clase().endsWith(".m3u8") && clase.getDireccion_clase().contains(".")) {

                // Extraer el directorio y el nombre del archivo de entrada
                Path inputPath = Paths.get(clase.getDireccion_clase());
                File baseFile = base.toFile();
                File inputFile = inputPath.toFile();
                File destino = new File(baseFile, inputPath.getFileName().toString());

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
                    processBuilder.directory(baseFile);
                    processBuilder.redirectErrorStream(true);

                    Process process = processBuilder.start();

                    // Leer la salida del proceso
                    try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.err.println("FFmpeg: " + line);
                        }
                    } catch (IOException e) {
                        System.err.println("Error leyendo salida de FFmpeg: " + e.getMessage());
                        process.destroy();
                        throw new RuntimeException("Error leyendo salida de FFmpeg: " + e.getMessage());
                    }

                    int exitCode = process.waitFor();
                    System.out.println("Salida del proceso de FFmpeg: " + exitCode);
                    if (exitCode != 0) {
                        throw new IOException("El proceso de FFmpeg falló con el código de salida " + exitCode);
                    }

                    clase.setDireccion_clase(base.toString() + "/master.m3u8");
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
        Long idCurso = clase.getCurso_clase().getId_curso();
        Long idClase = clase.getId_clase();
        Path baseUploadDir = Paths.get(uploadDir);
        Path outputDir = baseUploadDir.resolve(idCurso.toString()).resolve(idClase.toString());
        claseRepo.updateClase(idClase, clase.getNombre_clase(), clase.getTipo_clase(),
                outputDir.toString() + "/master.m3u8",
                clase.getPosicion_clase());
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
            System.err.println("Tipo de entrada no soportado");
            return;
        }
        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(outputDir.toFile());
        Process process = processBuilder.start();
        // Guardar el proceso en el mapa
        ffmpegProcesses.put(streamId.substring(streamId.lastIndexOf("_") + 1), process);

        // Hilo para escribir en el stdin de FFmpeg (solo para InputStream)
        if (inputStream instanceof InputStream) {
            System.out.println("Iniciando hilo de escritura en stdin de FFmpeg");
            new Thread(() -> {
                try {
                    InputStream IS = (InputStream) inputStream; // Input desde el proceso Go
                    OutputStream OS = process.getOutputStream(); // Salida hacia ffmpeg

                    byte[] buffer = new byte[40960];
                    int bytesRead;

                    while ((bytesRead = IS.read(buffer)) != -1) {
                        System.out.println("Enviando paquete a ffmpeg: " + bytesRead + " bytes");
                        OS.write(buffer, 0, bytesRead);
                        OS.flush();
                    }

                    // NO cerramos OS si quieres mantener ffmpeg activo

                } catch (IOException e) {
                    System.err.println("Error al escribir en el stdin de FFmpeg: " + e.getMessage());
                }
            }).start();
        }

        // Hilo para leer los logs de FFmpeg
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread logReader = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("FFmpeg: " + line);
                }
            } catch (IOException e) {
                System.err.println("Error leyendo salida de FFmpeg: " + e.getMessage());
                throw new RuntimeException("Error leyendo salida de FFmpeg: " +
                        e.getMessage());
            }
        });
        logReader.start();
        // logReader.join(); // Esperar a que se terminen de leer los logs
    }

    public void stopFFmpegProcessForUser(String streamId) throws IOException, RuntimeException {
        String sessionId = streamId.substring(streamId.lastIndexOf('_') + 1);
        Process process = ffmpegProcesses.remove(sessionId);
        if (process != null && process.isAlive()) {
            try {
                OutputStream os = process.getOutputStream();
                os.write('q'); // Señal de terminación
                os.flush();
                os.close();

                boolean finished = process.waitFor(5, TimeUnit.SECONDS); // Esperar 5 segundos
                if (finished) {
                    // El proceso terminó correctamente
                    int exitCode = process.exitValue();
                    if (exitCode == 0) {
                    } else {
                        System.err.println("FFmpeg preview terminó con un error. Código de salida: " + exitCode);
                        throw new RuntimeException(
                                "FFmpeg preview terminó con un error. Código de salida: " + exitCode);
                    }
                } else {
                    // Si no terminó en 1 segundo, forzar la terminación
                    System.err.println(
                            "El proceso FFmpeg preview no respondió en el tiempo esperado. Terminando de forma forzada...");
                    process.destroy(); // Intentar una terminación limpia
                    if (process.isAlive()) {
                        process.destroyForcibly(); // Forzar si sigue vivo
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("El proceso fue interrumpido: " + e.getMessage());
                Thread.currentThread().interrupt();
                throw new RuntimeException("El proceso fue interrumpido: " + e.getMessage());
            }
        }
    }

    public Path getPreview(String id_preview) {
        Path baseUploadDir = Paths.get(uploadDir);
        Path previewDir = baseUploadDir.resolve("previews");
        Path m3u8 = previewDir.resolve(id_preview + ".m3u8");
        // Espera a que se genere el preview
        while (!Files.exists(m3u8)) {
            // Espera medio segundo y reintenta
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("Error al esperar a que se genere el preview: " + e.getMessage());
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
        String hls_playlist_type = live ? "event" : "vod";
        String hls_flags = live ? "independent_segments+append_list+program_date_time" : "independent_segments";
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
            System.out.println("Obteniendo la resolución del video con ffprobe");
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
                System.out.println("FFProbe: " + line);
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
                break;
            }

            process.waitFor();
            if (width == "0" || height == "0" || fps == "0") {
                System.err.println("La resolución es 0");
                throw new RuntimeException("No se pudo obtener la resolución del streaming");
            }
            if (width == null || height == null || fps == null) {
                System.err.println("La resolución es null, reintentando con ffprobe");
                this.creaComandoFFmpeg(inputFilePath, live, streamId, videoSetting);
            }
            System.out.println("Resolución: " + width + "x" + height + "@" + fps);
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
                .distinct()
                .collect(Collectors.toList());

        // Debug
        resolutionPairs.forEach(r -> System.out.println("Perfil: " +
                r.getWidth() + "x" + r.getHeight() +
                "@" + r.getFps() + "fps → br=" + r.getBitrate() + "kbps"));

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
            int Width = resolutionPairs.get(i).getWidth();
            int Height = resolutionPairs.get(i).getHeight();
            int fpsn = resolutionPairs.get(i).getFps();
            filters.addAll(Arrays.asList(
                    "-map", "[v" + (i + 1) + "out]",
                    "-c:v:" + i, "h264_vaapi", // o libx264 si no se usa VAAPI
                    "-qp:v:" + i, "4", // Constante de calidad
                    "-profile:v:" + i, resolutionPairs.get(i).getProfile(),
                    "-level:v:" + i, resolutionPairs.get(i).getLevel(),
                    "-b:v:" + i, resolutionPairs.get(i).getBitrate(),
                    "-maxrate:v:" + i, resolutionPairs.get(i).getMaxrate(),
                    "-bufsize:v:" + i, resolutionPairs.get(i).getBufsize(),
                    // "-preset", preset, // No sirve para VAAPI
                    "-g", String.valueOf(fpsn), // Conversión explícita de fps a String
                    // "-sc_threshold", "0", // No sirve para VAAPI
                    "-keyint_min", String.valueOf(fpsn),
                    "-hls_segment_filename", "%v/data%02d.ts"));
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

        if (live) {
            ffmpegCommand.add("-re");
        }

        ffmpegCommand.addAll(List.of(
                "-i", inputFilePath,
                "-f", "hls",
                "-hls_time", "5",
                "-hls_playlist_type", hls_playlist_type,
                "-hls_flags", hls_flags,
                "-hls_segment_type", "mpegts",
                "-hls_base_url", "%v/",
                "-filter_complex"));
        ffmpegCommand.addAll(filters);
        ffmpegCommand.addAll(List.of("-master_pl_name", "master.m3u8", "-var_stream_map"));
        String streamMap = "";
        for (int i = 0; i < resolutionPairs.size(); i++) {
            int Width = resolutionPairs.get(i).getWidth();
            int Height = resolutionPairs.get(i).getHeight();
            int fpsn = resolutionPairs.get(i).getFps();
            streamMap += " v:" + i + ",a:" + i + ",name:" + Width + "x" + Height + "@" + fpsn;
        }
        ffmpegCommand.add(streamMap);
        ffmpegCommand.addAll(List.of("%v.m3u8"));

        if (live) {
            ffmpegCommand.addAll(List.of("-map", "0:v?", "-map", "0:a?", "-c:v", "copy",
                    "-c:a", "aac", "original.mp4"));
        }

        System.out.println("Comando FFmpeg: " + String.join(" ", ffmpegCommand));
        return ffmpegCommand;
    }
}
