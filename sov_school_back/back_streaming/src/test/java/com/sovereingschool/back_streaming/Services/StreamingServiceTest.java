package com.sovereingschool.back_streaming.Services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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

}
