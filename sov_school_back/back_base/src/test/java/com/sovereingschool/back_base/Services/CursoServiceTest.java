package com.sovereingschool.back_base.Services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.net.URISyntaxException;
import java.nio.file.Path;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    // ==========================
    // Tests createCurso()
    // ==========================
    @Nested
    class CreateCursoTests {
    }

    // ==========================
    // Tests getCurso()
    // ==========================
    @Nested
    class GetCursoTests {
    }

    // ==========================
    // Tests getNombreCurso()
    // ==========================
    @Nested
    class GetNombreCursoTests {
    }

    // ==========================
    // Tests getProfesoresCurso()
    // ==========================
    @Nested
    class GetProfesoresCursoTests {
    }

    // ==========================
    // Tests getFechaCreacionCurso()
    // ==========================
    @Nested
    class GetFechaCreacionCursoTests {
    }

    // ==========================
    // Tests getClasesDelCurso()
    // ==========================
    @Nested
    class GetClasesDelCursoTests {
    }

    // ==========================
    // Tests getPlanesDelCurso()
    // ==========================
    @Nested
    class GetPlanesDelCursoTests {
    }

    // ==========================
    // Tests getPrecioCurso()
    // ==========================
    @Nested
    class GetPrecioCursoTests {
    }

    // ==========================
    // Tests updateCurso()
    // ==========================
    @Nested
    class UpdateCursoTests {
    }

    // ==========================
    // Tests deleteCurso()
    // ==========================
    @Nested
    class DeleteCursoTests {
    }

    // ==========================
    // Tests getAll()
    // ==========================
    @Nested
    class GetAllTests {
    }

    // ==========================
    // Tests deleteClase()
    // ==========================
    @Nested
    class DeleteClaseTests {
    }

    // ==========================
    // Tests subeVideo()
    // ==========================
    @Nested
    class SubeVideoTests {
    }

    @Mock
    private WebClientConfig webClientConfig;

    // Usamos RETURNS_DEEP_STUBS para evitar tener que mockear cada etapa
    // manualmente
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private InitAppService initAppService;

    private CursoService cursoService;
    private final String backStreamURL = "http://mocked-stream-url";
    private final String backChatURL = "http://mocked-chat-url";

    @TempDir
    Path tempDir; // carpeta temporal que se borra automáticamente

    @BeforeEach
    void setUp() throws SSLException, URISyntaxException {
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);
        cursoService = new CursoService(
                tempDir.toString(),
                backStreamURL,
                backChatURL,
                cursoRepo,
                claseRepo,
                initAppService,
                webClientConfig);
    }
}