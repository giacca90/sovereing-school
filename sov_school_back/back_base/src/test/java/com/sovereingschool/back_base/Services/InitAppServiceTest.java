package com.sovereingschool.back_base.Services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class InitAppServiceTest {

    // ==========================
    // Tests getProfesores()
    // ==========================
    @Nested
    class GetProfesoresTests {
    }

    // ==========================
    // Tests getInit()
    // ==========================
    @Nested
    class GetInitTests {
    }

    // ==========================
    // Tests getInitToken()
    // ==========================
    @Nested
    class GetInitTokenTests {
    }

    // ==========================
    // Tests refreshSSR()
    // ==========================
    @Nested
    class RefreshSSRTests {
    }

    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private WebClientConfig webClientConfig;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    private InitAppService initAppService;
    private final String frontDocker = "http://mocked-front-url";

    @BeforeEach
    void setUp() throws SSLException, URISyntaxException {
        lenient().when(webClientConfig.createSecureWebClient(anyString())).thenReturn(webClient);
        initAppService = new InitAppService(
                frontDocker,
                cursoRepo,
                claseRepo,
                usuarioRepo,
                jwtUtil,
                webClientConfig);
    }
}
