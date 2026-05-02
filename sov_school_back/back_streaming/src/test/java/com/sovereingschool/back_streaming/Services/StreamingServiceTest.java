package com.sovereingschool.back_streaming.Services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_streaming.Models.ResolutionProfile;
import com.sovereingschool.back_streaming.Repositories.UsuarioCursosRepository;

/**
 * Pruebas unitarias para {@link StreamingService}.
 */
@ExtendWith(MockitoExtension.class)
class StreamingServiceTest {

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

        /**
         * Prueba la conversión exitosa de videos.
         */
        @Test
        void convertVideos_ShouldConvertSuccessfully() throws Exception {
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

        /**
         * Prueba el error cuando no hay clases para convertir.
         */
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

        /**
         * Prueba la creación del comando FFmpeg.
         */
        @Test
        @DisplayName("Cobertura: creaComandoFFmpeg - flujos variados")
        void testCreaComandoFFmpegCoverage() throws Exception {
            // Caso: live=false (vod), sin settings, path sin pipe
            StreamingService spyService = spy(streamingService);
            doReturn(new String[] { "1280", "720", "30", "aac" }).when(spyService).ffprobe(anyString());

            List<String> command = spyService.creaComandoFFmpeg("video.mp4", false, null);

            assertTrue(command.contains("vod"));
            assertTrue(command.contains("independent_segments"));
            // En VOD (live=false), no se añade original.mp4 al final
            assertFalse(command.contains("original.mp4"));

            // Caso: live=true, settings proporcionados
            List<String> commandLive = spyService.creaComandoFFmpeg("rtmp://test", true,
                    new String[] { "1920", "1080", "60" });
            assertTrue(commandLive.contains("event"));
            assertTrue(commandLive.contains("original.mp4"));
        }

        @Test
        @DisplayName("Éxito: buildStreamMap sin audio")
        void buildStreamMap_noAudio() {
            ResolutionProfile p1 = ResolutionProfile.RES_720P_30;
            ResolutionProfile p2 = ResolutionProfile.RES_360P_30;
            List<ResolutionProfile> profiles = List.of(p1, p2);

            String result = streamingService.buildStreamMap(profiles, false);

            assertTrue(result.contains("v:0,name:1280x720@30"));
            assertTrue(result.contains("v:1,name:640x360@30"));
            assertFalse(result.contains("a:0"));
            assertFalse(result.contains("a:1"));
        }

        @Test
        @DisplayName("Cobertura: creaComandoFFmpeg con pipe y settings nulos")
        void testCreaComandoFFmpegPipeDefaultSettings() throws Exception {
            StreamingService spyService = spy(streamingService);

            // Caso 1: videoSetting es nulo
            List<String> command1 = spyService.creaComandoFFmpeg("pipe:0", true, null);
            // Debería usar 1280x720@30 por defecto
            System.out.println("Command: " + command1);
            assertTrue(command1.toString().contains("1280x720@30"));

            // Caso 2: videoSetting tiene longitud insuficiente
            List<String> command2 = spyService.creaComandoFFmpeg("pipe:0", true, new String[] { "800" });
            assertTrue(command2.toString().contains("1280x720@30"));

            // Caso 3: videoSetting[0] es nulo
            List<String> command3 = spyService.creaComandoFFmpeg("pipe:0", true, new String[] { null, "720", "30" });
            assertTrue(command3.toString().contains("1280x720@30"));
        }

        /**
         * Prueba el inicio exitoso del streaming RTMP.
         */
        @Test
        @DisplayName("Éxito: Iniciar Streaming RTMP con detección de argumentos corregida")
        void startLiveStreamingFromStream_RTMP_WithFFprobeCoverage() throws Exception {
            String streamId = "user_rtmp_123";
            String rtmpUrl = "rtmp://servidor/live/stream";
            String[] videoSetting = { "1280", "720", "30" };

            ReflectionTestUtils.setField(streamingService, "executor", (java.util.concurrent.Executor) Runnable::run);
            Clase claseMock = createMockClase(1L, 100L, "Clase RTMP");
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(claseMock));

            StreamingService spyService = spy(streamingService);
            doReturn(new String[] { "1280", "720", "25", "aac" }).when(spyService).ffprobe(anyString());

            // Mocks de procesos
            Process mockFfmpeg = mock(Process.class);
            when(mockFfmpeg.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream("ffmpeg log\n".getBytes()));

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.start()).thenReturn(mockFfmpeg);
                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                            })) {

                filesMock.when(() -> Files.exists(any())).thenReturn(true);
                filesMock.when(() -> Files.createDirectories(any())).thenReturn(null);

                spyService.startLiveStreamingFromStream(streamId, rtmpUrl, videoSetting);

                verify(claseRepo).updateClase(eq(100L), anyString(), anyInt(), contains("master.m3u8"), anyInt());
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

        /**
         * Prueba la cobertura del filtro de GPU NVIDIA.
         */
        @Test
        @DisplayName("Cobertura: createNvidiaGPUFilter")
        void testNvidiaGPUFilterCoverage() throws Exception {
            String streamId = "nvidia_stream_123";
            String rtmpUrl = "rtmp://servidor/live/nvidia";
            String[] videoSetting = { "1280", "720", "30" };

            ReflectionTestUtils.setField(streamingService, "executor", (java.util.concurrent.Executor) Runnable::run);
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(createMockClase(1L, 100L, "NVIDIA")));

            StreamingService spyService = spy(streamingService);
            doReturn(new String[] { "1920", "1080", "30", "aac" }).when(spyService).ffprobe(anyString());

            Process mockNvidiaSmi = mock(Process.class);
            Process mockFfmpeg = mock(Process.class);

            // Salida NVIDIA-SMI para que el detector la reconozca
            when(mockNvidiaSmi.getInputStream())
                    .thenReturn(new ByteArrayInputStream("NVIDIA-SMI 525.60.13".getBytes()));

            when(mockFfmpeg.getInputStream()).thenReturn(new ByteArrayInputStream("ffmpeg log".getBytes()));

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

                                if (fullCommand.contains("nvidia-smi")) {
                                    when(mock.start()).thenReturn(mockNvidiaSmi);
                                } else {
                                    when(mock.start()).thenReturn(mockFfmpeg);
                                }

                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                            })) {

                filesMock.when(() -> Files.exists(any())).thenReturn(true);

                // Ejecución
                spyService.startLiveStreamingFromStream(streamId, rtmpUrl, videoSetting);

                verify(claseRepo).updateClase(eq(100L), anyString(), anyInt(), contains("master.m3u8"), anyInt());
            }
        }

        /**
         * Prueba la cobertura del filtro de GPU Intel.
         */
        @Test
        @DisplayName("Cobertura: createIntelGPUFilter - Forzando detección Intel")
        void testIntelGPUFilterCoverage() throws Exception {
            String streamId = "intel_stream_456";
            String rtmpUrl = "rtmp://servidor/live/intel";
            String[] videoSetting = { "1280", "720", "30" };

            ReflectionTestUtils.setField(streamingService, "executor", (java.util.concurrent.Executor) Runnable::run);
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(createMockClase(2L, 200L, "Intel")));

            StreamingService spyService = spy(streamingService);
            doReturn(new String[] { "1280", "720", "30", "aac" }).when(spyService).ffprobe(anyString());

            Process mockVainfo = mock(Process.class);
            Process mockFfmpeg = mock(Process.class);

            // Salida Vainfo muy completa (estilo Linux real)
            String intelOutput = "libva info: VA-API version 1.17.0\n" +
                    "libva info: Trying to open /usr/lib/x86_64-linux-gnu/dri/iHD_drv_video.so\n" +
                    "libva info: Found init function __vaDriverInit_1_17\n" +
                    "libva info: va_openDriver() returns 0\n" +
                    "vainfo: Driver version: Intel iHD driver for Intel(R) Gen Graphics - 23.1.1";
            when(mockVainfo.getInputStream()).thenReturn(new ByteArrayInputStream(intelOutput.getBytes()));

            when(mockFfmpeg.getInputStream()).thenReturn(new ByteArrayInputStream("ffmpeg log".getBytes()));

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

                                if (fullCommand.contains("vainfo") || fullCommand.contains("intel")) {
                                    when(mock.start()).thenReturn(mockVainfo);
                                } else if (fullCommand.contains("nvidia-smi")) {
                                    Process mockFail = mock(Process.class);
                                    when(mockFail.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
                                    when(mock.start()).thenReturn(mockFail);
                                } else {
                                    when(mock.start()).thenReturn(mockFfmpeg);
                                }
                                when(mock.directory(any())).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                            })) {

                filesMock.when(() -> Files.exists(any())).thenAnswer(invocation -> {
                    String path = invocation.getArgument(0).toString();
                    if (path.contains("nvidia"))
                        return false;
                    if (path.contains("dri") || path.contains("render"))
                        return true;
                    return true;
                });

                spyService.startLiveStreamingFromStream(streamId, rtmpUrl, videoSetting);

                verify(claseRepo).updateClase(eq(200L), anyString(), anyInt(), contains("master.m3u8"), anyInt());
            }
        }
    }

    // ==========================
    // Tests stopFFmpegProcessForUser()
    // ==========================
    @Nested
    class StopFFmpegProcessForUserTests {
        /**
         * Prueba la detención exitosa del proceso FFmpeg.
         */
        @Test
        @DisplayName("Éxito: Detener proceso FFmpeg existente")
        void stopFFmpegProcess_success() throws Exception {
            String sessionId = "123";
            Process mockProcess = mock(Process.class);
            Map<String, Process> processes = (Map<String, Process>) ReflectionTestUtils.getField(streamingService,
                    "ffmpegProcesses");
            processes.put(sessionId, mockProcess);

            when(mockProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

            streamingService.stopFFmpegProcessForUser("user_stream_123");

            verify(mockProcess).destroy();
            assertTrue(processes.isEmpty());
        }

        /**
         * Prueba la detención forzada del proceso FFmpeg si no responde.
         */
        @Test
        @DisplayName("Error: stopFFmpegProcessForUser interrumpido")
        void stopFFmpegProcess_interrupted() throws Exception {
            String sessionId = "123";
            Process mockProcess = mock(Process.class);
            Map<String, Process> processes = (Map<String, Process>) ReflectionTestUtils.getField(streamingService,
                    "ffmpegProcesses");
            processes.put(sessionId, mockProcess);

            when(mockProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS))
                    .thenThrow(new InterruptedException("Interrupted"));

            assertThrows(InternalServerException.class,
                    () -> streamingService.stopFFmpegProcessForUser("user_stream_123"));
            assertTrue(Thread.interrupted());
        }
    }

    // ==========================
    // Tests getPreview()
    // ==========================
    @Nested
    class GetPreviewTests {
        /**
         * Prueba la obtención exitosa de la previsualización.
         */
        @Test
        @DisplayName("Error: getPreview interrumpido")
        void getPreview_interrupted() throws Exception {
            String idPreview = "preview1";
            try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
                filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);

                // Simular interrupción del hilo
                Thread.currentThread().interrupt();

                assertThrows(InternalServerException.class, () -> streamingService.getPreview(idPreview));
                assertTrue(Thread.interrupted());
            }
        }
    }

    // ==========================
    // Tests convertVideos()
    // ==========================

    @Nested
    class FfprobeTests {

        @Test
        @DisplayName("Error: ffprobe lanza IOException al leer")
        void ffprobe_readIOException() throws Exception {
            String inputPath = "video.mp4";
            Process mockProcess = mock(Process.class);
            InputStream mockIn = mock(InputStream.class);
            when(mockIn.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Read error"));
            when(mockProcess.getInputStream()).thenReturn(mockIn);

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                    (mock, context) -> {
                        when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                        when(mock.start()).thenReturn(mockProcess);
                    })) {
                assertThrows(InternalServerException.class, () -> streamingService.ffprobe(inputPath));
            }
        }

        @Test
        @DisplayName("Error: ffprobe interrumpido")
        void ffprobe_interrupted() throws Exception {
            String inputPath = "video.mp4";
            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream())
                    .thenReturn(new ByteArrayInputStream("1280,720,30/1,video,h264\n".getBytes()));
            when(mockProcess.waitFor()).thenThrow(new InterruptedException("Interrupted"));

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                    (mock, context) -> {
                        when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                        when(mock.start()).thenReturn(mockProcess);
                    })) {
                assertThrows(InternalServerException.class, () -> streamingService.ffprobe(inputPath));
                assertTrue(Thread.interrupted());
            }
        }

        @Test
        @DisplayName("Cobertura: ffprobe con FPS malformados")
        void ffprobe_numberFormatException() throws Exception {
            String inputPath = "video.mp4";
            Process mockProcess = mock(Process.class);
            // frameRate malformado (no tiene /) y otro que causa den=0
            String output = "1280,720,invalid,video,h264\n" +
                    "1280,720,30/0,video,h264\n" +
                    "1280,720,60/2,video,h264\n";
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes()));

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                    (mock, context) -> {
                        when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                        when(mock.start()).thenReturn(mockProcess);
                    })) {
                String[] result = streamingService.ffprobe(inputPath);
                // Debería haber tomado el 60/2 = 30
                assertEquals("30", result[2]);
            }
        }

        @Test
        @DisplayName("Error: ffprobe no encuentra stream de video")
        void ffprobe_noVideoFound() throws Exception {
            String inputPath = "video.mp4";
            Process mockProcess = mock(Process.class);
            // Solo audio
            String output = ",,,audio,aac\n";
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes()));

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                    (mock, context) -> {
                        when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                        when(mock.start()).thenReturn(mockProcess);
                    })) {
                assertThrows(InternalServerException.class, () -> streamingService.ffprobe(inputPath));
            }
        }

        @Test
        @DisplayName("Error: ffprobe lanza IOException al iniciar")
        void ffprobe_startIOException() throws Exception {
            String inputPath = "some/path/video.mp4";
            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenThrow(new IOException("Cannot start process"));
            })) {
                InternalServerException exception = assertThrows(InternalServerException.class,
                        () -> streamingService.ffprobe(inputPath));
                assertTrue(exception.getMessage().contains("Error al ejecutar ffprobe"));
            }
        }

        @Test
        @DisplayName("Error: ffprobe no devuelve resolución completa")
        void ffprobe_incompleteResolution() throws Exception {
            Process mockProcess = mock(Process.class);
            // Salida que garantiza que parts.length sea al menos 5 para evitar
            // ArrayIndexOutOfBounds
            // pero que deje width, height o fps como null para entrar en el if del error

            when(mockProcess.getInputStream())
                    .thenReturn(new ByteArrayInputStream("video,codec,null,null,null\n".getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenReturn(mockProcess);
            })) {
                InternalServerException exception = assertThrows(InternalServerException.class,

                        () -> streamingService.ffprobe("some/path/video.mp4"));
                assertTrue(exception.getMessage().contains("No se pudo obtener la resolución del streaming"));
            }
        }

        @Test
        @DisplayName("Error: IOException al leer la salida de ffprobe")
        void ffprobe_readOutputError() throws Exception {
            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new java.io.InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Error reading stream");
                }
            });

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenReturn(mockProcess);
            })) {
                InternalServerException exception = assertThrows(InternalServerException.class,

                        () -> streamingService.ffprobe("some/path/video.mp4"));
                assertTrue(exception.getMessage().contains("Error al leer la salida de ffprobe"));
            }
        }

        @Test
        @DisplayName("Error: InterruptedException durante waitFor de ffprobe")
        void ffprobe_interrupted_waitFor() throws Exception {
            String inputPath = "some/path/video.mp4";

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream())
                    .thenReturn(new ByteArrayInputStream("1280,720,30/1,video,h264\n,,,audio,aac\n".getBytes()));
            when(mockProcess.waitFor()).thenThrow(new InterruptedException("FFprobe interrupted"));

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenReturn(mockProcess);
            })) {
                InternalServerException exception = assertThrows(InternalServerException.class,
                        () -> streamingService.ffprobe(inputPath));
                assertTrue(exception.getMessage().contains("FFprobe se interrumpió"));
                assertTrue(Thread.currentThread().isInterrupted());
            }
        }

        @Test
        @DisplayName("Éxito: ffprobe con audio codec null")
        void ffprobe_success_noAudioCodec() throws Exception {
            String inputPath = "some/path/video.mp4";

            Process mockProcess = mock(Process.class);
            // Simula salida sin línea de audio
            String ffprobeOutput = "1280,720,30/1,video,h264\n";
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(ffprobeOutput.getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenReturn(mockProcess);
            })) {
                String[] result = streamingService.ffprobe(inputPath);
                assertArrayEquals(new String[] { "1280", "720", "30", null }, result); // El último elemento debe ser
                                                                                       // null
            }
        }

        @Test
        @DisplayName("Éxito: ffprobe con fps calculado correctamente")
        void ffprobe_success_fpsCalculated() throws Exception {
            String inputPath = "some/path/video.mp4";

            Process mockProcess = mock(Process.class);
            // Simula un framerate que necesita redondeo
            String ffprobeOutput = "1280,720,60000/1001,video,h264\n"; // ~59.94 fps, debería redondear a 60
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(ffprobeOutput.getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenReturn(mockProcess);
            })) {
                String[] result = streamingService.ffprobe(inputPath);
                assertArrayEquals(new String[] { "1280", "720", "60", null }, result);
            }
        }
    }

    // ==========================
    // Tests processSingleClase()
    // ==========================
    @Nested
    class ProcessSingleClaseTests {
        Clase clase;
        Curso curso;
        Path baseUploadDir;
        Path destinationPath;

        @BeforeEach
        void setUp() {
            clase = new Clase();
            clase.setIdClase(1L);
            clase.setNombreClase("Clase Test");
            clase.setDireccionClase("original/path/video.mp4");
            clase.setTipoClase(0);
            clase.setPosicionClase(1);

            curso = new Curso();
            curso.setIdCurso(10L);
            curso.setNombreCurso("Curso Test");
            curso.setClasesCurso(List.of(clase));

            baseUploadDir = Paths.get(tempDir.toString());
            destinationPath = baseUploadDir.resolve(curso.getIdCurso().toString())
                    .resolve(clase.getIdClase().toString());

            clase.setCursoClase(curso);
        }

        @Test
        @DisplayName("Error: Falla al mover el archivo de video")
        void processSingleClase_moveFileError() throws Exception {
            // Simular que Files.move lanza una excepción
            try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class)))
                        .thenThrow(new IOException("Error simulado al mover archivo"));

                InternalServerException exception = assertThrows(InternalServerException.class,
                        () -> streamingService.processSingleClase(curso, clase, baseUploadDir, destinationPath));
                assertTrue(exception.getMessage().contains("Error en mover el video de la clase"));
            }
        }

        @Test
        @DisplayName("Error: IOException al iniciar el proceso FFmpeg")
        void processSingleClase_ffmpegStartIOException() throws Exception {
            StreamingService spyService = spy(streamingService);
            doReturn(List.of("ffmpeg", "-i", "input.mp4")).when(spyService).creaComandoFFmpeg(anyString(), anyBoolean(),
                    any());

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any(File.class))).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenThrow(new IOException("Error simulado al iniciar FFmpeg"));
                            })) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class)))
                        .thenReturn(destinationPath.resolve("video.mp4"));

                InternalServerException exception = assertThrows(InternalServerException.class,
                        () -> spyService.processSingleClase(curso, clase, baseUploadDir, destinationPath));
                assertTrue(exception.getMessage().contains("Error al convertir la clase"));
            }
        }

        @Test
        @DisplayName("Error: IOException al leer la salida de FFmpeg")
        void processSingleClase_ffmpegReadOutputIOException() throws Exception {
            StreamingService spyService = spy(streamingService);
            doReturn(List.of("ffmpeg", "-i", "input.mp4")).when(spyService).creaComandoFFmpeg(anyString(), anyBoolean(),
                    any());

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Error simulado al leer la salida");
                }
            });

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any(File.class))).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenReturn(mockProcess);
                            })) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class)))
                        .thenReturn(destinationPath.resolve("video.mp4"));

                // No se lanza excepción, simplemente se loguea y retorna
                spyService.processSingleClase(curso, clase, baseUploadDir, destinationPath);

                // No verificamos excepciones, solo que el save no se llamó y no lanzó nada
                verify(claseRepo, times(0)).save(any(Clase.class));
            }
        }

        @Test
        @DisplayName("Error: FFmpeg termina con código de salida distinto de 0")
        void processSingleClase_ffmpegExitCodeError() throws Exception {
            StreamingService spyService = spy(streamingService);
            doReturn(List.of("ffmpeg", "-i", "input.mp4")).when(spyService).creaComandoFFmpeg(anyString(), anyBoolean(),
                    any());

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream())
                    .thenReturn(new ByteArrayInputStream("FFmpeg output with error\n".getBytes()));
            when(mockProcess.waitFor()).thenReturn(1); // Simula error de FFmpeg

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any(File.class))).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenReturn(mockProcess);
                            })) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class)))
                        .thenReturn(destinationPath.resolve("video.mp4"));

                // No se lanza excepción, simplemente se loguea y retorna
                spyService.processSingleClase(curso, clase, baseUploadDir, destinationPath);

                // Verificamos que el save no se llamó
                verify(claseRepo, times(0)).save(any(Clase.class));
            }
        }

        @Test
        @DisplayName("Error: InterruptedException durante waitFor de FFmpeg")
        void processSingleClase_ffmpegInterrupted() throws Exception {
            StreamingService spyService = spy(streamingService);
            doReturn(List.of("ffmpeg", "-i", "input.mp4")).when(spyService).creaComandoFFmpeg(anyString(), anyBoolean(),
                    any());

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("FFmpeg output\n".getBytes()));
            when(mockProcess.waitFor()).thenThrow(new InterruptedException("FFmpeg interrupted"));

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any(File.class))).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenReturn(mockProcess);
                            })) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class)))
                        .thenReturn(destinationPath.resolve("video.mp4"));

                InternalServerException exception = assertThrows(InternalServerException.class,
                        () -> spyService.processSingleClase(curso, clase, baseUploadDir, destinationPath));
                assertTrue(exception.getMessage().contains("Error al convertir la clase"));
                assertTrue(Thread.currentThread().isInterrupted());
            }
        }

        @Test
        @DisplayName("Éxito: processSingleClase completa correctamente")
        void processSingleClase_success() throws Exception {
            StreamingService spyService = spy(streamingService);
            doReturn(List.of("ffmpeg", "-i", "input.mp4")).when(spyService).creaComandoFFmpeg(anyString(), anyBoolean(),
                    any());

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("FFmpeg output\n".getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            try (MockedStatic<Files> filesMock = mockStatic(Files.class);
                    MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                            (mock, context) -> {
                                when(mock.directory(any(File.class))).thenReturn(mock);
                                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                                when(mock.start()).thenReturn(mockProcess);
                            })) {
                filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
                filesMock.when(() -> Files.move(any(Path.class), any(Path.class), any(StandardCopyOption.class)))
                        .thenReturn(destinationPath.resolve("video.mp4"));

                spyService.processSingleClase(curso, clase, baseUploadDir, destinationPath);

                verify(claseRepo, times(1)).save(clase);
                assertTrue(clase.getDireccionClase().contains("master.m3u8"));
            }
        }
    }

    @Nested
    class AdditionalCoverageTests {

        @Test
        @DisplayName("Cobertura: createNvidiaGPUFilter con lista vacía")
        void testCreateNvidiaGPUFilterEmptyProfiles() {
            List<com.sovereingschool.back_streaming.Models.ResolutionProfile> profiles = java.util.Collections
                    .emptyList();
            List<String> result = streamingService.createNvidiaGPUFilter(profiles);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Cobertura: createIntelGPUFilter con perfiles")
        void testCreateIntelGPUFilter() {

            List<com.sovereingschool.back_streaming.Models.ResolutionProfile> profiles = List
                    .of(com.sovereingschool.back_streaming.Models.ResolutionProfile.RES_720P_30);
            List<String> result = streamingService.createIntelGPUFilter(profiles);
            assertFalse(result.isEmpty());
            assertTrue(result.toString().contains("h264_vaapi"));
        }

        @Test
        @DisplayName("Cobertura: createNvidiaGPUFilter con perfiles")
        void testCreateNvidiaGPUFilter() {

            List<com.sovereingschool.back_streaming.Models.ResolutionProfile> profiles = List
                    .of(com.sovereingschool.back_streaming.Models.ResolutionProfile.RES_720P_30);
            List<String> result = streamingService.createNvidiaGPUFilter(profiles);
            assertFalse(result.isEmpty());
            assertTrue(result.toString().contains("h264_nvenc"));
        }

        @Test
        @DisplayName("Cobertura: lambda de startLiveStreamingFromStream")
        void testLambdaExecutorCoverage() throws Exception {
            String streamId = "stream_999";
            Clase claseMock = createMockClase(1L, 999L, "Test Lambda");
            when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(claseMock));

            StreamingService spyService = spy(streamingService);
            doReturn(new String[] { "1920", "1080", "30", "aac" }).when(spyService).ffprobe(anyString());

            // Simular proceso FFmpeg
            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(
                    "ffmpeg log".getBytes()));

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                when(mock.directory(any())).thenReturn(mock);
                when(mock.redirectErrorStream(anyBoolean())).thenReturn(mock);
                when(mock.start()).thenReturn(mockProcess);
            })) {
                ReflectionTestUtils.setField(spyService, "executor",
                        (java.util.concurrent.Executor) Runnable::run);

                spyService.startLiveStreamingFromStream(streamId, "rtmp://test",
                        new String[] { "1280", "720", "30" });
            }
        }

    }

    @Mock
    private UsuarioCursosRepository usuarioCursosRepository;

    @Mock
    private ClaseRepository claseRepo;

    @InjectMocks
    private StreamingService streamingService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Error: orElseThrow en startLiveStreamingFromStream")
    void testStartLiveStreamingFromStream_NotFound() {
        when(claseRepo.findByDireccionClase("non-existent")).thenReturn(Optional.empty());
        assertThrows(com.sovereingschool.back_common.Exceptions.RepositoryException.class,
                () -> streamingService.startLiveStreamingFromStream("non-existent", "test", null));
    }

    @Test
    @DisplayName("Cobertura: startLiveStreamingFromStream con entrada no soportada")
    void startLiveStreamingFromStream_UnsupportedInput() throws Exception {
        String streamId = "test_123";
        Clase claseMock = createMockClase(1L, 100L, "Test");
        when(claseRepo.findByDireccionClase(streamId)).thenReturn(Optional.of(claseMock));

        streamingService.startLiveStreamingFromStream(streamId, 123, null);

        verify(claseRepo, times(1)).findByDireccionClase(streamId);
    }

    @Test
    @DisplayName("Cobertura: configureInputSource con varias ramas")
    void testConfigureInputSourceCoverage() {
        List<String> command = new java.util.ArrayList<>();
        // Caso: live=true, path=rtmp://...
        streamingService.configureInputSource(command, "rtmp://test", true, null);
        assertTrue(command.contains("-re"));

        // Caso: pipe
        List<String> commandPipe = new java.util.ArrayList<>();
        streamingService.configureInputSource(commandPipe, "pipe:0", true, new String[] { "1280", "720", "30" });
        assertTrue(commandPipe.contains("-f"));
        assertTrue(commandPipe.contains("sdp"));
    }

    @Test
    @DisplayName("Cobertura: applyHardwareAcceleration todas las ramas")
    void testApplyHardwareAccelerationCoverage() {
        List<String> command = new java.util.ArrayList<>();
        streamingService.applyHardwareAcceleration(command,
                com.sovereingschool.back_streaming.Utils.GPUDetector.VideoAcceleration.VAAPI);
        assertTrue(command.contains("/dev/dri/renderD128"));

        List<String> commandNvidia = new java.util.ArrayList<>();
        streamingService.applyHardwareAcceleration(commandNvidia,
                com.sovereingschool.back_streaming.Utils.GPUDetector.VideoAcceleration.NVIDIA);
        assertTrue(commandNvidia.contains("cuda"));
    }

    @BeforeEach
    void setUp() {
        String uploadDir = tempDir.toString();
        // streamingService ya está inyectado por @InjectMocks,
        // pero necesitamos configurar el directorio de subida manualmente si es
        // necesario
        ReflectionTestUtils.setField(streamingService, "uploadDir", uploadDir);
    }

    @Test
    @DisplayName("Éxito: Registrar progreso correctamente")
    void registrarProgreso_success() {
        Long idUsuario = 1L;
        Long idCurso = 10L;
        Long idClase = 100L;
        int segmentIndex = 5;

        streamingService.registrarProgreso(idUsuario, idCurso, idClase, segmentIndex);

        verify(usuarioCursosRepository).updateProgress(idUsuario, idCurso, idClase, segmentIndex);
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
