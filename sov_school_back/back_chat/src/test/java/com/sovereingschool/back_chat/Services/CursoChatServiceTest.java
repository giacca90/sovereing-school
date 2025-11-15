package com.sovereingschool.back_chat.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sovereingschool.back_chat.Repositories.CursoChatRepository;
import com.sovereingschool.back_chat.Repositories.MensajeChatRepository;
import com.sovereingschool.back_chat.Repositories.UsuarioChatRepository;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class CursoChatServiceTest {
    // ==========================
    // Tests getCursoChat()
    // ==========================
    @Nested
    class GetCursoChatTests {
    }

    // ==========================
    // Tests guardaMensaje()
    // ==========================
    @Nested
    class GuardaMensajeTests {
    }

    // ==========================
    // Tests creaUsuarioChat()
    // ==========================
    @Nested
    class CreaUsuarioChatTests {
    }

    // ==========================
    // Tests creaCursoChat()
    // ==========================
    @Nested
    class CreaCursoChatTests {
    }

    // ==========================
    // Tests creaClaseChat()
    // ==========================
    @Nested
    class CreaClaseChatTests {
    }

    // ==========================
    // Tests mensajeLeido()
    // ==========================
    @Nested
    class MensajeLeidoTests {
    }

    // ==========================
    // Tests borrarClaseChat()
    // ==========================
    @Nested
    class BorrarClaseChatTests {
    }

    // ==========================
    // Tests borrarCursoChat()
    // ==========================
    @Nested
    class BorrarCursoChatTests {
    }

    // ==========================
    // Tests borrarUsuarioChat()
    // ==========================
    @Nested
    class BorrarUsuarioChatTests {
    }

    // ==========================
    // Tests refreshTokenInOpenWebsocket()
    // ==========================
    @Nested
    class RefreshTokenInOpenWebsocketTests {
    }

    // ==========================
    // Tests init()
    // ==========================
    @Nested
    class InitTests {
    }

    // ==========================
    // Tests actualizarCursoChat()
    // ==========================
    @Nested
    class ActualizarCursoChatTests {
    }

    // ==========================
    // Tests getAllCursosChat()
    // ==========================
    @Nested
    class GetAllCursosChatTests {
    }

    @Mock
    private CursoChatRepository cursoChatRepo;
    @Mock
    private InitChatService initChatService;
    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private MensajeChatRepository mensajeChatRepo;
    @Mock
    private UsuarioChatRepository usuarioChatRepo;
    @Mock
    private JwtUtil jwtUtil;

    private CursoChatService cursoChatService;

    @BeforeEach
    void setUp() {
        cursoChatService = new CursoChatService(
                cursoChatRepo,
                initChatService,
                cursoRepo,
                claseRepo,
                usuarioRepo,
                mensajeChatRepo,
                usuarioChatRepo,
                jwtUtil);
    }
}
