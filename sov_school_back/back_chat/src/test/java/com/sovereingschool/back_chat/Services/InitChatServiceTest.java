package com.sovereingschool.back_chat.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sovereingschool.back_chat.Repositories.CursoChatRepository;
import com.sovereingschool.back_chat.Repositories.MensajeChatRepository;
import com.sovereingschool.back_chat.Repositories.UsuarioChatRepository;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InitChatServiceTest {

    // ==========================
    // Tests initChat()
    // ==========================
    @Nested
    class InitChatTests {
    }

    // ==========================
    // Tests getMensajesDTO()
    // ==========================
    @Nested
    class GetMensajesDTOTests {
    }

    @Mock
    private UsuarioChatRepository usuarioChatRepo;
    @Mock
    private MensajeChatRepository mensajeChatRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private CursoRepository cursoRepo;
    @Mock
    private CursoChatRepository cursoChatRepo;
    @Mock
    private ClaseRepository claseRepo;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private ReactiveMongoTemplate reactiveMongoTemplate;

    private InitChatService initChatService;

    @BeforeEach
    void setUp() {
        initChatService = new InitChatService(
                cursoChatRepo,
                claseRepo,
                usuarioChatRepo,
                mensajeChatRepo,
                usuarioRepo,
                cursoRepo,
                simpMessagingTemplate,
                reactiveMongoTemplate);
    }
}
