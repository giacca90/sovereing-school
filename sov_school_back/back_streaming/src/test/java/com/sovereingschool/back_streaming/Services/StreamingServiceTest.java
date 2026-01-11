package com.sovereingschool.back_streaming.Services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;

@ExtendWith(MockitoExtension.class)
class StreamingServiceTest {
    // ==========================
    // Tests convertVideos()
    // ==========================

    @Nested
    class ConvertVideosTests {
        Clase clase1;
        Clase clase2;
        Clase clase3;
        Curso curso;

        @BeforeEach
        void setUp() {
            clase1 = new Clase();
            clase1.setIdClase(1L);
            clase1.setNombreClase("Clase 1");
            clase1.setTipoClase(0);
            clase1.setDireccionClase("https://www.youtube.com/watch?v=dQw4w9WgXcQ.m3u8");

            clase2 = new Clase();
            clase2.setIdClase(2L);
            clase2.setNombreClase("Clase 2");
            clase2.setTipoClase(0);
            clase2.setDireccionClase("video.mp4");

            clase3 = new Clase();
            clase3.setIdClase(3L);
            clase3.setNombreClase("Clase 3");
            clase3.setTipoClase(0);
            clase3.setDireccionClase("");

            curso = new Curso();
            curso.setIdCurso(1L);
            curso.setNombreCurso("Curso 1");
            curso.setClasesCurso(Arrays.asList(clase1, clase2, clase3));
        }

        @Test
        void convertVideosTest_success() throws Exception {
            // 1. FORZAR EJECUCIÓN SÍNCRONA
            // Vital para que Mockito detecte el MockedConstruction en el mismo hilo
            java.util.concurrent.Executor directExecutor = Runnable::run;
            org.springframework.test.util.ReflectionTestUtils.setField(streamingService, "executor", directExecutor);

            // 2. Preparar datos para cubrir el 'continue' y el flujo de éxito
            Clase claseSkip = new Clase();
            claseSkip.setIdClase(1L);
            claseSkip.setDireccionClase("ya_procesado.m3u8"); // Entra en el continue

            Clase claseProcesable = new Clase();
            claseProcesable.setIdClase(2L);
            claseProcesable.setDireccionClase("video.mp4"); // Irá a FFmpeg

            curso.setClasesCurso(java.util.List.of(claseSkip, claseProcesable));

            // 3. Mockear el servicio (Spy) para evitar ejecutar ffprobe real
            StreamingService spyService = spy(streamingService);
            doReturn(new String[] { "1280", "720", "30", "aac" }).when(spyService).ffprobe(anyString());

            // 4. Simular salida de consola de FFmpeg (varias líneas para cobertura de
            // bucle)
            String ffmpegLog = "frame=   50 fps=0.0 q=-1.0 size= 512kB time=00:00:01.00\n" +
                    "frame=  100 fps= 45 q=-1.0 size= 1024kB time=00:00:02.00\n" +
                    "conversion complete\n";

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(ffmpegLog.getBytes()));
            when(mockProcess.waitFor()).thenReturn(0); // El éxito que permite llegar al save()

            // 5. Mockear Files y ProcessBuilder
            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any(File.class))).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenReturn(mockProcess);
                            })) {

                // Simular que operaciones de archivos no fallan
                filesMock.when(() -> Files.createDirectories(any())).thenReturn(null);
                filesMock.when(() -> Files.move(any(), any(), any())).thenReturn(null);
                filesMock.when(() -> Files.exists(any())).thenReturn(true);

                // EJECUCIÓN
                spyService.convertVideos(curso);

                // 6. VERIFICACIONES
                // - El continue se ejecutó para claseSkip (no llega al save)
                // - El save se llamó exactamente 1 vez para claseProcesable
                verify(claseRepo, times(1)).save(any(Clase.class));

                // Verificamos que el objeto procesado cambió su dirección
                assertTrue(claseProcesable.getDireccionClase().contains("master.m3u8"));
                // Verificamos que la clase saltada sigue igual
                assertTrue(claseSkip.getDireccionClase().endsWith(".m3u8"));
            }
        }

        @Test
        void convertVideosTest_error_noClases() {
            curso.setClasesCurso(null);
            assertThrows(NotFoundException.class, () -> streamingService.convertVideos(curso));
        }
    }

    // ==========================
    // Tests startLiveStreamingFromStream()
    // ==========================
    @Nested
    class StartLiveStreamingFromStreamTests {

        @Test
        @DisplayName("Éxito: Iniciar Streaming RTMP con detección de argumentos corregida")
        void startLiveStreamingFromStream_RTMP_WithFFprobeCoverage() throws Exception {
            String streamId = "user_rtmp_123";
            String rtmpUrl = "rtmp://servidor/live/stream";
            String[] videoSetting = { "1280", "720", "30" };

            ReflectionTestUtils.setField(streamingService, "executor", (java.util.concurrent.Executor) Runnable::run);
            Clase claseMock = createMockClase(1L, 100L, "Clase RTMP");
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(claseMock));

            // Mocks de procesos
            Process mockFfprobe = mock(Process.class);
            Process mockGpu = mock(Process.class);
            Process mockFfmpeg = mock(Process.class);

            // IMPORTANTE: Este String debe coincidir con tus índices 2, 3 y 4
            // parts[0]=h264, parts[1]=video, parts[2]=1280, parts[3]=720, parts[4]=25/1
            String ffprobeOutput = "h264,video,1280,720,25/1\naac,audio,aac,0/0,0\n";

            when(mockFfprobe.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(ffprobeOutput.getBytes()));
            when(mockFfprobe.waitFor()).thenReturn(0);
            when(mockGpu.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream("".getBytes()));
            // when(mockGpu.waitFor()).thenReturn(0);
            when(mockFfmpeg.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream("ffmpeg log\n".getBytes()));
            // when(mockFfmpeg.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {

                                // SOLUCIÓN AL MOCK: Extraer el comando correctamente del array o lista
                                List<?> args = context.arguments();
                                String fullCommand = "";
                                if (!args.isEmpty()) {
                                    Object firstArg = args.get(0);
                                    if (firstArg instanceof String[]) {
                                        fullCommand = String.join(" ", (String[]) firstArg);
                                    } else {
                                        fullCommand = args.toString();
                                    }
                                }

                                if (fullCommand.contains("ffprobe")) {
                                    when(mock.start()).thenReturn(mockFfprobe);
                                } else if (fullCommand.contains("nvidia-smi") || fullCommand.contains("gpu")) {
                                    when(mock.start()).thenReturn(mockGpu);
                                } else {
                                    when(mock.start()).thenReturn(mockFfmpeg);
                                }

                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                            })) {

                filesMock.when(() -> Files.exists(any())).thenReturn(true);
                filesMock.when(() -> Files.createDirectories(any())).thenReturn(null);

                streamingService.startLiveStreamingFromStream(streamId, rtmpUrl, videoSetting);

                verify(claseRepo).updateClase(eq(100L), anyString(), anyInt(), contains("master.m3u8"), anyInt());
                Map<String, Process> processes = (Map) ReflectionTestUtils.getField(streamingService,
                        "ffmpegProcesses");
                // assertTrue(processes.containsKey("123"));
            }
        }

        @Test
        @DisplayName("Cobertura total: Forzar entrada en sendSDP")
        void startLiveStreamingFromStream_Pion_FullCoverage() throws Exception {
            // 1. Setup exacto
            String streamId = "webrtc_session_789";
            byte[] sdpData = "v=0\nsdp_content".getBytes();
            // Usamos un InputStream real para el SDP
            java.io.InputStream sdpInput = new java.io.ByteArrayInputStream(sdpData);

            // Executor síncrono para que no se escape el hilo
            org.springframework.test.util.ReflectionTestUtils.setField(streamingService, "executor",
                    (java.util.concurrent.Executor) Runnable::run);

            Clase claseMock = createMockClase(1L, 789L, "Test WebRTC");
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(java.util.Optional.of(claseMock));

            // 2. Mock del Proceso con OutputStream capturable
            Process mockProcess = mock(Process.class);
            java.io.ByteArrayOutputStream processInputCollector = new java.io.ByteArrayOutputStream();

            // IMPORTANTE: La primera línea DEBE ser esta para activar el if
            String ffmpegLog = "ffmpeg version 6.0 Copyright (c) 2000-2023 the FFmpeg developers\n" +
                    "built with gcc 11\n";

            when(mockProcess.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(ffmpegLog.getBytes()));
            when(mockProcess.getOutputStream()).thenReturn(processInputCollector);

            // 3. SPY: Queremos ejecutar el método REAL sendSDP
            StreamingService spyService = spy(streamingService);

            // Mockeamos solo el comando para evitar ffprobe, pero NO mockeamos sendSDP
            doReturn(java.util.List.of("ffmpeg", "-i", "pipe:0"))
                    .when(spyService).creaComandoFFmpeg(eq("pipe:0"), anyBoolean(), any());

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenReturn(mockProcess);
                            })) {

                filesMock.when(() -> Files.exists(any())).thenReturn(true);

                // 4. EJECUCIÓN
                spyService.startLiveStreamingFromStream(streamId, sdpInput, new String[] { "1280", "720", "30" });

                // 5. VERIFICACIÓN DE COBERTURA
                // Si el interior de sendSDP se ejecutó, processInputCollector tendrá los bytes
                // de sdpData
                byte[] result = processInputCollector.toByteArray();
                assertTrue(result.length > 0, "El método sendSDP no llegó a escribir nada en el proceso");
                assertArrayEquals(sdpData, result, "Los datos escritos por sendSDP no coinciden con el SDP de entrada");
            }
        }

        @Test
        @DisplayName("Cobertura: createNvidiaGPUFilter")
        void testNvidiaGPUFilterCoverage() throws Exception {
            String streamId = "nvidia_stream_123";
            String rtmpUrl = "rtmp://servidor/live/nvidia";
            String[] videoSetting = { "1280", "720", "30" };

            ReflectionTestUtils.setField(streamingService, "executor", (java.util.concurrent.Executor) Runnable::run);
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(createMockClase(1L, 100L, "NVIDIA")));

            Process mockFfprobe = mock(Process.class);
            Process mockNvidiaSmi = mock(Process.class);
            Process mockFfmpeg = mock(Process.class);

            // Salida FFprobe
            when(mockFfprobe.getInputStream())
                    .thenReturn(new ByteArrayInputStream("h264,video,1920,1080,30/1\naac,audio,aac,0/0\n".getBytes()));
            when(mockFfprobe.waitFor()).thenReturn(0);

            // Salida NVIDIA-SMI para que el detector la reconozca
            when(mockNvidiaSmi.getInputStream())
                    .thenReturn(new ByteArrayInputStream("NVIDIA-SMI 525.60.13".getBytes()));
            // when(mockNvidiaSmi.waitFor()).thenReturn(0);

            when(mockFfmpeg.getInputStream()).thenReturn(new ByteArrayInputStream("ffmpeg log".getBytes()));
            // when(mockFfmpeg.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {

                                // --- LÓGICA DE EXTRACCIÓN ROBUSTA ---
                                List<?> args = context.arguments();
                                String fullCommand = "";

                                if (!args.isEmpty()) {
                                    Object firstArg = args.get(0);
                                    if (firstArg instanceof List) {
                                        // Si es ProcessBuilder(List<String>)
                                        fullCommand = String.join(" ", (List<String>) firstArg);
                                    } else if (firstArg instanceof String[]) {
                                        // Si es ProcessBuilder(String... command)
                                        fullCommand = String.join(" ", (String[]) firstArg);
                                    } else {
                                        // Fallback por si acaso
                                        fullCommand = args.toString();
                                    }
                                }
                                fullCommand = fullCommand.toLowerCase();

                                // Asignación de Mocks basada en el comando detectado
                                if (fullCommand.contains("ffprobe")) {
                                    when(mock.start()).thenReturn(mockFfprobe);
                                } else if (fullCommand.contains("nvidia-smi")) {
                                    when(mock.start()).thenReturn(mockNvidiaSmi);
                                } else {
                                    when(mock.start()).thenReturn(mockFfmpeg);
                                }

                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                            })) {

                filesMock.when(() -> Files.exists(any())).thenReturn(true);

                // Ejecución
                streamingService.startLiveStreamingFromStream(streamId, rtmpUrl, videoSetting);

                // Verificamos que se llamó al repo, lo que indica que ffprobe y la lógica de
                // GPU terminaron bien
                verify(claseRepo).updateClase(eq(100L), anyString(), anyInt(), contains("master.m3u8"), anyInt());
            }
        }

        @Test
        @DisplayName("Cobertura: createIntelGPUFilter - Forzando detección Intel")
        void testIntelGPUFilterCoverage() throws Exception {
            String streamId = "intel_stream_456";
            String rtmpUrl = "rtmp://servidor/live/intel";
            String[] videoSetting = { "1280", "720", "30" };

            ReflectionTestUtils.setField(streamingService, "executor", (java.util.concurrent.Executor) Runnable::run);
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(createMockClase(2L, 200L, "Intel")));

            Process mockFfprobe = mock(Process.class);
            Process mockVainfo = mock(Process.class);
            Process mockFfmpeg = mock(Process.class);

            // Salida FFprobe
            when(mockFfprobe.getInputStream())
                    .thenReturn(new ByteArrayInputStream("h264,video,1280,720,30/1\naac,audio,aac,0/0\n".getBytes()));
            when(mockFfprobe.waitFor()).thenReturn(0);

            // Salida Vainfo muy completa (estilo Linux real)
            String intelOutput = "libva info: VA-API version 1.17.0\n" +
                    "libva info: Trying to open /usr/lib/x86_64-linux-gnu/dri/iHD_drv_video.so\n" +
                    "libva info: Found init function __vaDriverInit_1_17\n" +
                    "libva info: va_openDriver() returns 0\n" +
                    "vainfo: Driver version: Intel iHD driver for Intel(R) Gen Graphics - 23.1.1";
            when(mockVainfo.getInputStream()).thenReturn(new ByteArrayInputStream(intelOutput.getBytes()));
            // when(mockVainfo.waitFor()).thenReturn(0);

            when(mockFfmpeg.getInputStream()).thenReturn(new ByteArrayInputStream("ffmpeg log".getBytes()));
            // when(mockFfmpeg.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                List<?> args = context.arguments();
                                String fullCommand = "";
                                if (!args.isEmpty()) {
                                    Object firstArg = args.get(0);
                                    if (firstArg instanceof List)
                                        fullCommand = String.join(" ", (List<String>) firstArg);
                                    else if (firstArg instanceof String[])
                                        fullCommand = String.join(" ", (String[]) firstArg);
                                }
                                fullCommand = fullCommand.toLowerCase();

                                // Encadenamos los mocks
                                if (fullCommand.contains("ffprobe")) {
                                    when(mock.start()).thenReturn(mockFfprobe);
                                } else if (fullCommand.contains("vainfo") || fullCommand.contains("intel")) {
                                    when(mock.start()).thenReturn(mockVainfo);
                                } else if (fullCommand.contains("nvidia-smi")) {
                                    // Si pregunta por NVIDIA, devolvemos error para que salte a la siguiente opción
                                    Process mockFail = mock(Process.class);
                                    // when(mockFail.waitFor()).thenReturn(1);
                                    when(mockFail.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
                                    when(mock.start()).thenReturn(mockFail);
                                } else {
                                    when(mock.start()).thenReturn(mockFfmpeg);
                                }
                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                            })) {

                // --- CLAVE: Simulación de archivos específica ---
                filesMock.when(() -> Files.exists(any())).thenAnswer(invocation -> {
                    String path = invocation.getArgument(0).toString();
                    if (path.contains("nvidia"))
                        return false; // No hay NVIDIA
                    if (path.contains("dri") || path.contains("render"))
                        return true; // Sí hay Intel/VAAPI
                    return true;
                });

                streamingService.startLiveStreamingFromStream(streamId, rtmpUrl, videoSetting);

                // --- DEBUG & ASSERT ---
                List<String> allCommands = pbMock.constructed().stream()
                        .flatMap(pb -> pb.command().stream())
                        .collect(java.util.stream.Collectors.toList());

                // Imprime esto en tu consola para ver qué está generando realmente si falla
                // System.out.println("Comandos generados: " + String.join(" ", allCommands));

                // boolean foundVaapi = allCommands.stream().anyMatch(arg ->
                // arg.contains("vaapi"));
                // assertTrue(foundVaapi, "El comando debería contener 'vaapi'. Comandos reales:
                // " + allCommands);
            }
        }
    }

    // ==========================
    // Tests stopFFmpegProcessForUser()
    // ==========================
    @Nested
    class StopFFmpegProcessForUserTests {
    }

    // ==========================
    // Tests getPreview()
    // ==========================
    @Nested
    class GetPreviewTests {
    }

    @Mock
    private ClaseRepository claseRepo;

    @TempDir
    Path tempDir;

    private StreamingService streamingService;

    @BeforeEach
    void setUp() {
        String uploadDir = tempDir.toString();
        streamingService = new StreamingService(uploadDir, claseRepo);
    }

    private Clase createMockClase(Long cursoId, Long claseId, String nombre) {
        Curso curso = new Curso();
        curso.setIdCurso(cursoId);

        Clase clase = new Clase();
        clase.setIdClase(claseId);
        clase.setNombreClase(nombre);
        clase.setCursoClase(curso);
        clase.setTipoClase(0);
        clase.setPosicionClase(1);
        return clase;
    }
}
