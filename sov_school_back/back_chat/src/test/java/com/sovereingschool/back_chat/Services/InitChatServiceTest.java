package com.sovereingschool.back_chat.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document; // Esta es la clase que permite hacer new Document()import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sovereingschool.back_chat.DTOs.CursoChatDTO;
import com.sovereingschool.back_chat.DTOs.InitChatDTO;
import com.sovereingschool.back_chat.Models.ClaseChat;
import com.sovereingschool.back_chat.Models.CursoChat;
import com.sovereingschool.back_chat.Models.MensajeChat;
import com.sovereingschool.back_chat.Models.UsuarioChat;
import com.sovereingschool.back_chat.Repositories.CursoChatRepository;
import com.sovereingschool.back_chat.Repositories.MensajeChatRepository;
import com.sovereingschool.back_chat.Repositories.UsuarioChatRepository;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class InitChatServiceTest {

    // ==========================
    // Tests initChat()
    // ==========================
    @Nested
    class InitChatTests {

        private Long idUsuario = 1L;
        private Long idCurso = 100L;
        private String idMsg = "msg123";

        private UsuarioChat usuarioChat;
        private MensajeChat mensaje;
        private MensajeChat respuesta;
        private CursoChat cursoChat;
        private Curso curso;

        @BeforeEach
        void setUp() {
            // 1. Configurar UsuarioChat
            usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setMensajes(new ArrayList<>(List.of(idMsg)));
            usuarioChat.setCursos(new ArrayList<>(List.of("idMongoCurso")));

            // 2. Configurar MensajeChat
            respuesta = new MensajeChat();
            respuesta.setId("idRespuesta");
            respuesta.setIdUsuario(idUsuario);
            respuesta.setIdCurso(100L);
            respuesta.setMensaje("respuesta");

            mensaje = new MensajeChat();
            mensaje.setId(idMsg);
            mensaje.setIdUsuario(idUsuario);
            mensaje.setIdCurso(100L);
            mensaje.setMensaje("Hola mundo");
            mensaje.setRespuesta(respuesta.getId());

            // 3. Configurar CursoChat (Entidad de BD de chat)
            cursoChat = new CursoChat();
            cursoChat.setId("idMongoCurso");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setUltimo(idMsg);
            cursoChat.setClases(new ArrayList<>());

            // 4. Configurar Curso (Entidad de BD principal)
            curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Curso de Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());
        }

        @Test
        void initChat_success() {
            // Configuración de Mocks usando las variables inicializadas
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));

            // Simulamos que encuentra el cursoChat
            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("nombreUsuario"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("fotoUsuario"));
            when(mensajeChatRepo.findById(mensaje.getRespuesta())).thenReturn(Optional.of(respuesta));

            InitChatDTO result = initChatService.initChat(idUsuario);

            // Verificaciones
            assertNotNull(result);
            assertEquals(idUsuario, result.idUsuario());
            verify(usuarioChatRepo, times(1)).findByIdUsuario(idUsuario);
        }

        @Test
        void initChat_errorUsuarioNoEncontrado() {
            // Mock para caso de error
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });
        }

        @Test
        void initChat_errorIdNulo() {
            assertThrows(IllegalArgumentException.class, () -> {
                initChatService.initChat(null);
            });
        }

        @Test
        void initChat_errorCursoNoEncontrado() {
            // Quito la respuesta del mensaje, para cobertura
            mensaje.setRespuesta(null);
            // Configuración de Mocks usando las variables inicializadas
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("nombreUsuario"));

            when(cursoRepo.findById(idCurso)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });

            assertEquals(e.getMessage(), "Error en obtener el curso con ID " + cursoChat.getIdCurso());
        }

        @Test
        void initChat_errorNombreUsuarioNoEncontrado() {
            // Quito la respuesta del mensaje, para cobertura
            mensaje.setRespuesta(null);
            // Configuración de Mocks usando las variables inicializadas
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));

            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });

            // Verificaciones
            assertEquals("Error en obtener el nombre del usuario", e.getMessage());
        }

        @Test
        void initChat_errorNombreUsuarioRespuestaNoEncontrado() {

            // Configuración de Mocks usando las variables inicializadas
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));

            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.empty());
            when(mensajeChatRepo.findById(mensaje.getRespuesta())).thenReturn(Optional.of(respuesta));

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });

            // Verificaciones
            assertEquals("Error en obtener el nombre del usuario", e.getMessage());
        }

        @Test
        void initChat_errorCursoNoEncontradoInGetCursosUser() {
            // Configuración de Mocks usando las variables inicializadas
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));

            // Simulamos que encuentra el cursoChat
            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso)).thenReturn(Optional.empty());
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("nombreUsuario"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("fotoUsuario"));
            when(mensajeChatRepo.findById(mensaje.getRespuesta())).thenReturn(Optional.of(respuesta));

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });
            // Verificaciones
            assertEquals("Error en obtener el curso con ID " + cursoChat.getIdCurso(), e.getMessage());
        }

    }

    // ==========================================
    // 2. TESTS DE OBSERVADORES (Reactivo + Lógica)
    // ==========================================
    @Nested
    class ObserverTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;

        @Test
        @SuppressWarnings("unchecked")
        void observeMultipleCollections_FullIntegration_UserChat_Coverage() {
            // 1. Setup Documento
            Document userDoc = new Document()
                    .append("idUsuario", idUsuario)
                    .append("mensajes", new ArrayList<String>())
                    .append("cursos", List.of("mongoCursoId"));

            ChangeStreamEvent<Document> mockEvent = mock(ChangeStreamEvent.class);
            when(mockEvent.getBody()).thenReturn(userDoc);

            when(reactiveMongoTemplate.changeStream(eq("users_chat"), any(), eq(Document.class)))
                    .thenReturn(Flux.just(mockEvent));
            when(reactiveMongoTemplate.changeStream(eq("courses_chat"), any(), eq(Document.class)))
                    .thenReturn(Flux.empty());

            // 3. MOCK DE CURSO CON CLASES (Para disparar mapClasesToDTO)
            ClaseChat claseChat = new ClaseChat();
            claseChat.setIdClase(50L);
            claseChat.setIdCurso(idCurso);
            claseChat.setMensajes(new ArrayList<>()); // Lista vacía de mensajes de clase

            CursoChat cc = new CursoChat();
            cc.setIdCurso(idCurso);
            // AGREGAMOS LA CLASE AQUÍ PARA QUE EL STREAM SE EJECUTE
            cc.setClases(List.of(claseChat));

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cc));

            // 4. MOCK DEL REPO DE CLASES (Necesario para el .orElseThrow)
            when(claseRepo.findNombreClaseById(50L)).thenReturn(Optional.of("Clase de Java"));

            // 5. MOCK DEL CURSO
            Curso c = new Curso();
            c.setNombreCurso("Java");
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(c));

            // EJECUCIÓN
            initChatService.observeMultipleCollections();

            // VERIFICACIÓN
            verify(simpMessagingTemplate, timeout(2000)).convertAndSendToUser(
                    eq(idUsuario.toString()),
                    eq("/init_chat/result"),
                    argThat(dto -> {
                        InitChatDTO res = (InitChatDTO) dto;
                        // Verificamos que la clase se procesó correctamente
                        return !res.cursos().get(0).clases().isEmpty() &&
                                res.cursos().get(0).clases().get(0).nombreClase().equals("Clase de Java");
                    }));
        }

        @Test
        @SuppressWarnings("unchecked")
        void observeMultipleCollections_FullIntegration_CourseChat_Coverage() {
            // 1. Setup Documento
            Document courseDoc = new Document()
                    .append("idCurso", idCurso)
                    .append("mensajes", List.of())
                    .append("clases", List.of());

            ChangeStreamEvent<Document> mockEvent = mock(ChangeStreamEvent.class);
            when(mockEvent.getBody()).thenReturn(courseDoc);

            // 2. Mock del stream
            when(reactiveMongoTemplate.changeStream(eq("courses_chat"), any(), eq(Document.class)))
                    .thenReturn(Flux.just(mockEvent));
            when(reactiveMongoTemplate.changeStream(eq("users_chat"), any(), eq(Document.class)))
                    .thenReturn(Flux.empty());

            // 3. Mocks de repos
            Curso c = new Curso();
            c.setNombreCurso("Java");
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(c));

            // 4. Ejecución
            initChatService.observeMultipleCollections();

            // 5. Verificación
            verify(simpMessagingTemplate, timeout(2000)).convertAndSend(
                    eq("/init_chat/" + idCurso),
                    any(CursoChatDTO.class));
        }
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
