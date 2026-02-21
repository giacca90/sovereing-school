package com.sovereingschool.back_chat.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sovereingschool.back_chat.DTOs.ClaseChatDTO;
import com.sovereingschool.back_chat.DTOs.CursoChatDTO;
import com.sovereingschool.back_chat.DTOs.InitChatDTO;
import com.sovereingschool.back_chat.DTOs.MensajeChatDTO;
import com.sovereingschool.back_chat.Models.ClaseChat;
import com.sovereingschool.back_chat.Models.CursoChat;
import com.sovereingschool.back_chat.Models.MensajeChat;
import com.sovereingschool.back_chat.Models.UsuarioChat;
import com.sovereingschool.back_chat.Repositories.CursoChatRepository;
import com.sovereingschool.back_chat.Repositories.MensajeChatRepository;
import com.sovereingschool.back_chat.Repositories.UsuarioChatRepository;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class InitChatServiceTest {

    // ==========================================
    // 1. TESTS DE initChat()
    // ==========================================
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

            // 2. Configurar MensajeChat respuesta
            respuesta = new MensajeChat();
            respuesta.setId("idRespuesta");
            respuesta.setIdUsuario(idUsuario);
            respuesta.setIdCurso(100L);
            respuesta.setMensaje("respuesta");
            respuesta.setMomento(1);
            respuesta.setFecha(new Date());

            // 3. Configurar MensajeChat pregunta
            mensaje = new MensajeChat();
            mensaje.setId(idMsg);
            mensaje.setIdUsuario(idUsuario);
            mensaje.setIdCurso(100L);
            mensaje.setIdClase(200L);
            mensaje.setMensaje("Hola mundo");
            mensaje.setRespuesta(respuesta.getId());
            mensaje.setMomento(1);
            mensaje.setFecha(new Date());

            // 4. Configurar CursoChat
            cursoChat = new CursoChat();
            cursoChat.setId("idMongoCurso");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setUltimo(idMsg);
            cursoChat.setClases(new ArrayList<>());

            // 5. Configurar Curso
            curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Curso de Java");
            curso.setImagenCurso("foto.png");
            Clase clase = new Clase();
            clase.setIdClase(200L);
            clase.setNombreClase("Introducción");
            curso.setClasesCurso(List.of(clase));
        }

        @Test
        void initChat_success() {
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));
            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("nombreUsuario"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("fotoUsuario.jpg"));
            when(mensajeChatRepo.findById(mensaje.getRespuesta())).thenReturn(Optional.of(respuesta));

            InitChatDTO result = initChatService.initChat(idUsuario);

            assertNotNull(result);
            assertEquals(idUsuario, result.idUsuario());
            assertEquals(1, result.mensajes().size());
            verify(usuarioChatRepo, times(1)).findByIdUsuario(idUsuario);
        }

        @Test
        void initChat_errorUsuarioNoEncontrado() {
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
        void initChat_sinMensajes() {
            usuarioChat.setMensajes(new ArrayList<>());
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));
            when(mensajeChatRepo.findById(cursoChat.getUltimo()))
                    .thenReturn(Optional.empty());

            InitChatDTO result = initChatService.initChat(idUsuario);

            assertNotNull(result);
            assertTrue(result.mensajes().isEmpty());
        }

        @Test
        void initChat_sinCursos() {
            usuarioChat.setCursos(new ArrayList<>());
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("nombreUsuario"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("fotoUsuario.jpg"));
            when(mensajeChatRepo.findById(mensaje.getRespuesta())).thenReturn(Optional.of(respuesta));
            when(cursoRepo.findById(mensaje.getIdCurso())).thenReturn(Optional.of(curso));

            InitChatDTO result = initChatService.initChat(idUsuario);

            assertNotNull(result);
            assertTrue(result.cursos().isEmpty());
        }

        @Test
        void initChat_errorNombreUsuarioNoEncontrado() {
            mensaje.setRespuesta(null);
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });

            assertEquals("Error en obtener el nombre del usuario", e.getMessage());
        }

        @Test
        void initChat_errorCursoNoEncontrado() {
            mensaje.setRespuesta(null);
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("nombreUsuario"));
            when(cursoRepo.findById(mensaje.getIdCurso())).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });

            assertEquals("Error en obtener el curso con ID " + mensaje.getIdCurso(), e.getMessage());
        }

        @Test
        void initChat_errorRespuestaNoEncontrada() {
            when(usuarioChatRepo.findByIdUsuario(idUsuario)).thenReturn(Optional.of(usuarioChat));
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensaje));
            when(mensajeChatRepo.findById(mensaje.getRespuesta())).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.initChat(idUsuario);
            });

            assertEquals("Error en obtener la respuesta del mensaje", e.getMessage());
        }
    }

    // ==========================================
    // 2. TESTS DE getMensajesDTO()
    // ==========================================
    @Nested
    class GetMensajesDTOTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;
        private Long idClase = 200L;

        private MensajeChat mensaje;
        private MensajeChat respuesta;
        private Curso curso;

        @BeforeEach
        void setUp() {
            respuesta = new MensajeChat();
            respuesta.setId("idRespuesta");
            respuesta.setIdUsuario(idUsuario);
            respuesta.setIdCurso(idCurso);
            respuesta.setMensaje("Respuesta");
            respuesta.setMomento(1);
            respuesta.setFecha(new Date());

            mensaje = new MensajeChat();
            mensaje.setId("msg1");
            mensaje.setIdUsuario(idUsuario);
            mensaje.setIdCurso(idCurso);
            mensaje.setIdClase(idClase);
            mensaje.setMensaje("Pregunta");
            mensaje.setRespuesta("idRespuesta");
            mensaje.setMomento(1);
            mensaje.setFecha(new Date());

            Clase clase = new Clase();
            clase.setIdClase(idClase);
            clase.setNombreClase("Introducción");

            curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(List.of(clase));
        }

        @Test
        void getMensajesDTO_conRespuesta() {
            when(mensajeChatRepo.findById("idRespuesta")).thenReturn(Optional.of(respuesta));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg,otra.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<MensajeChatDTO> result = initChatService.getMensajesDTO(List.of(mensaje));

            assertNotNull(result);
            assertEquals(1, result.size());
            assertNotNull(result.get(0).respuesta());
            assertEquals("Juan", result.get(0).nombreUsuario());
        }

        @Test
        void getMensajesDTO_sinRespuesta() {
            mensaje.setRespuesta(null);
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<MensajeChatDTO> result = initChatService.getMensajesDTO(List.of(mensaje));

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(null, result.get(0).respuesta());
        }

        @Test
        void getMensajesDTO_claseNoEncontradaEnCurso() {
            mensaje.setIdClase(999L);
            mensaje.setRespuesta(null);
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<MensajeChatDTO> result = initChatService.getMensajesDTO(List.of(mensaje));

            assertNotNull(result);
            assertEquals(null, result.get(0).nombreClase());
        }

        @Test
        void getMensajesDTO_vacio() {
            List<MensajeChatDTO> result = initChatService.getMensajesDTO(new ArrayList<>());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getMensajesDTO_errorNombreUsuarioRespuesta() {
            when(mensajeChatRepo.findById("idRespuesta")).thenReturn(Optional.of(respuesta));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.getMensajesDTO(List.of(mensaje));
            });
            assertEquals("Error en obtener el nombre del usuario", e.getMessage());
        }
    }

    // ==========================================
    // 3. TESTS DE getMessageUser()
    // ==========================================
    @Nested
    class GetMessageUserTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;

        @Test
        void getMessageUser_conMensajes() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setMensajes(List.of("msg1", "msg2"));

            MensajeChat msg = new MensajeChat();
            msg.setId("msg1");
            msg.setIdUsuario(idUsuario);
            msg.setIdCurso(idCurso);
            msg.setMensaje("Hola");
            msg.setRespuesta(null);
            msg.setMomento(1);
            msg.setFecha(new Date());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(msg));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<MensajeChatDTO> result = initChatService.getMessageUser(usuarioChat);

            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        void getMessageUser_sinMensajes() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setMensajes(new ArrayList<>());

            List<MensajeChatDTO> result = initChatService.getMessageUser(usuarioChat);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getMessageUser_mensajesNulo() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setMensajes(null);

            List<MensajeChatDTO> result = initChatService.getMessageUser(usuarioChat);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getMessageUser_mensajesVaciosAlConsultar() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setMensajes(List.of("msg1"));

            when(mensajeChatRepo.findAllById(anyList())).thenReturn(new ArrayList<>());

            List<MensajeChatDTO> result = initChatService.getMessageUser(usuarioChat);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================
    // 4. TESTS DE getCursosUser()
    // ==========================================
    @Nested
    class GetCursosUserTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;

        @Test
        void getCursosUser_conCursos() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setCursos(List.of("mongoCursoId"));

            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setUltimo("msg1");
            cursoChat.setClases(new ArrayList<>());

            MensajeChat mensaje = new MensajeChat();
            mensaje.setId("msg1");
            mensaje.setIdUsuario(idUsuario);
            mensaje.setIdCurso(idCurso);
            mensaje.setMensaje("Último");
            mensaje.setRespuesta(null);
            mensaje.setMomento(1);
            mensaje.setFecha(new Date());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));
            when(mensajeChatRepo.findById("msg1")).thenReturn(Optional.of(mensaje));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));

            List<CursoChatDTO> result = initChatService.getCursosUser(usuarioChat);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(idCurso, result.get(0).idCurso());
        }

        @Test
        void getCursosUser_conCursosPeroCursoNoEncontrado() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setCursos(List.of("mongoCursoId"));

            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setUltimo(null);
            cursoChat.setClases(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.getCursosUser(usuarioChat);
            });
            assertEquals("Error en obtener el curso con ID " + idCurso, e.getMessage());
        }

        @Test
        void getCursosUser_sinCursos() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setCursos(new ArrayList<>());

            List<CursoChatDTO> result = initChatService.getCursosUser(usuarioChat);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getCursosUser_cursosNulo() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setCursos(null);

            List<CursoChatDTO> result = initChatService.getCursosUser(usuarioChat);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getCursosUser_sinUltimoMensaje() {
            UsuarioChat usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(idUsuario);
            usuarioChat.setCursos(List.of("mongoCursoId"));

            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setUltimo(null);
            cursoChat.setClases(new ArrayList<>());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<CursoChatDTO> result = initChatService.getCursosUser(usuarioChat);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("No hay mensajes en este curso", result.get(0).mensajes().get(0).mensaje());
        }
    }

    // ==========================================
    // 5. TESTS DE generateNoMessage()
    // ==========================================
    @Nested
    class GenerateNoMessageTests {
        private Long idCurso = 100L;

        @Test
        void generateNoMessage_success() {
            CursoChat cursoChat = new CursoChat();
            cursoChat.setIdCurso(idCurso);

            Curso curso = new Curso();
            curso.setNombreCurso("Java Avanzado");
            curso.setImagenCurso("imagen.png");

            MensajeChatDTO result = initChatService.generateNoMessage(cursoChat, curso);

            assertNotNull(result);
            assertEquals(idCurso, result.idCurso());
            assertEquals("Java Avanzado", result.nombreCurso());
            assertEquals("No hay mensajes en este curso", result.mensaje());
            assertEquals("imagen.png", result.fotoCurso());
        }
    }

    // ==========================================
    // 6. TESTS DE fetchMensajesFromIds()
    // ==========================================
    @Nested
    class FetchMensajesFromIdsTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;

        @Test
        void fetchMensajesFromIds_conMensajes() {
            MensajeChat msg = new MensajeChat();
            msg.setId("msg1");
            msg.setIdUsuario(idUsuario);
            msg.setIdCurso(idCurso);
            msg.setMensaje("contenido");
            msg.setRespuesta(null);
            msg.setMomento(1);
            msg.setFecha(new Date());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(msg));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<MensajeChatDTO> result = initChatService.fetchMensajesFromIds(List.of("msg1"));

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        void fetchMensajesFromIds_vacio() {
            List<MensajeChatDTO> result = initChatService.fetchMensajesFromIds(new ArrayList<>());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void fetchMensajesFromIds_nulo() {
            List<MensajeChatDTO> result = initChatService.fetchMensajesFromIds(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void fetchMensajesFromIds_noEncontrados() {
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(new ArrayList<>());

            List<MensajeChatDTO> result = initChatService.fetchMensajesFromIds(List.of("msg1", "msg2"));

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================
    // 7. TESTS DE mapClasesToDTO()
    // ==========================================
    @Nested
    class MapClasesToDTOTests {
        private Long idClase = 50L;
        private Long idCurso = 100L;
        private Long idUsuario = 1L;

        @Test
        void mapClasesToDTO_conClases() {
            ClaseChat claseChat = new ClaseChat();
            claseChat.setIdClase(idClase);
            claseChat.setIdCurso(idCurso);
            claseChat.setMensajes(List.of("msg1"));

            MensajeChat msg = new MensajeChat();
            msg.setId("msg1");
            msg.setIdUsuario(idUsuario);
            msg.setIdCurso(idCurso);
            msg.setIdClase(idClase);
            msg.setMensaje("contenido");
            msg.setRespuesta(null);
            msg.setMomento(1);
            msg.setFecha(new Date());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(msg));
            when(claseRepo.findNombreClaseById(idClase)).thenReturn(Optional.of("Introducción"));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<ClaseChatDTO> result = initChatService.mapClasesToDTO(List.of(claseChat));

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(idClase, result.get(0).idClase());
            assertEquals("Introducción", result.get(0).nombreClase());
        }

        @Test
        void mapClasesToDTO_vacio() {
            List<ClaseChatDTO> result = initChatService.mapClasesToDTO(new ArrayList<>());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void mapClasesToDTO_nulo() {
            List<ClaseChatDTO> result = initChatService.mapClasesToDTO(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void mapClasesToDTO_claseNoEncontrada() {
            ClaseChat claseChat = new ClaseChat();
            claseChat.setIdClase(idClase);
            claseChat.setIdCurso(idCurso);
            claseChat.setMensajes(new ArrayList<>());

            when(claseRepo.findNombreClaseById(idClase)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.mapClasesToDTO(List.of(claseChat));
            });
            assertEquals("Error en obtener el nombre de la clase", e.getMessage());
        }
    }

    // ==========================================
    // 8. TESTS DE fetchCursosUsuario()
    // ==========================================
    @Nested
    class FetchCursosUsuarioTests {
        private Long idCurso = 100L;
        private Long idClase = 50L;

        @Test
        void fetchCursosUsuario_conCursos() {
            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setClases(new ArrayList<>());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<CursoChatDTO> result = initChatService.fetchCursosUsuario(List.of("mongoCursoId"));

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(idCurso, result.get(0).idCurso());
        }

        @Test
        void fetchCursosUsuario_vacio() {
            List<CursoChatDTO> result = initChatService.fetchCursosUsuario(new ArrayList<>());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void fetchCursosUsuario_nulo() {
            List<CursoChatDTO> result = initChatService.fetchCursosUsuario(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void fetchCursosUsuario_cursoNoEncontrado() {
            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setClases(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.fetchCursosUsuario(List.of("mongoCursoId"));
            });
            assertEquals("Error en obtener el curso con ID " + idCurso, e.getMessage());
        }

        @Test
        void fetchCursosUsuario_conClases() {
            ClaseChat claseChat = new ClaseChat();
            claseChat.setIdClase(idClase);
            claseChat.setIdCurso(idCurso);
            claseChat.setMensajes(new ArrayList<>());

            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setClases(List.of(claseChat));

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));
            when(claseRepo.findNombreClaseById(idClase)).thenReturn(Optional.of("Introducción"));

            List<CursoChatDTO> result = initChatService.fetchCursosUsuario(List.of("mongoCursoId"));

            assertNotNull(result);
            assertEquals(1, result.size());
            assertFalse(result.get(0).clases().isEmpty());
        }
    }

    // ==========================================
    // 9. TESTS DE fetchClasesChat()
    // ==========================================
    @Nested
    class FetchClasesChatTests {
        private Long idClase = 50L;
        private Long idCurso = 100L;
        private Long idUsuario = 1L;

        @Test
        void fetchClasesChat_conClases() {
            Document doc = new Document()
                    .append("idClase", idClase)
                    .append("idCurso", idCurso)
                    .append("mensajes", List.of("msg1"));

            MensajeChat msg = new MensajeChat();
            msg.setId("msg1");
            msg.setIdUsuario(idUsuario);
            msg.setIdCurso(idCurso);
            msg.setIdClase(idClase);
            msg.setMensaje("contenido");
            msg.setRespuesta(null);
            msg.setMomento(1);
            msg.setFecha(new Date());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(msg));
            when(claseRepo.findNombreClaseById(idClase)).thenReturn(Optional.of("Introducción"));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            List<ClaseChatDTO> result = initChatService.fetchClasesChat(List.of(doc));

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(idClase, result.get(0).idClase());
        }

        @Test
        void fetchClasesChat_vacio() {
            List<ClaseChatDTO> result = initChatService.fetchClasesChat(new ArrayList<>());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void fetchClasesChat_nulo() {
            List<ClaseChatDTO> result = initChatService.fetchClasesChat(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void fetchClasesChat_claseNoEncontrada() {
            Document doc = new Document()
                    .append("idClase", idClase)
                    .append("idCurso", idCurso)
                    .append("mensajes", new ArrayList<>());

            when(claseRepo.findNombreClaseById(idClase)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.fetchClasesChat(List.of(doc));
            });
            assertEquals("Error en obtener el nombre de la clase", e.getMessage());
        }
    }

    // ==========================================
    // 10. TESTS DE notifyCoursesChat()
    // ==========================================
    @Nested
    class NotifyCoursesChatTests {
        private Long idCurso = 100L;
        private Long idClase = 50L;
        private Long idUsuario = 1L;

        @Test
        void notifyCoursesChat_success() {
            Document doc = new Document()
                    .append("idCurso", idCurso)
                    .append("mensajes", List.of())
                    .append("clases", new ArrayList<Document>());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");

            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            initChatService.notifyCoursesChat(doc);

            verify(simpMessagingTemplate, times(1)).convertAndSend(
                    eq("/init_chat/" + idCurso),
                    any(CursoChatDTO.class));
        }

        @Test
        void notifyCoursesChat_conClases() {
            Document claseDoc = new Document()
                    .append("idClase", idClase)
                    .append("idCurso", idCurso)
                    .append("mensajes", new ArrayList<>());

            Document doc = new Document()
                    .append("idCurso", idCurso)
                    .append("mensajes", List.of())
                    .append("clases", List.of(claseDoc));

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");

            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));
            when(claseRepo.findNombreClaseById(idClase)).thenReturn(Optional.of("Introducción"));

            initChatService.notifyCoursesChat(doc);

            verify(simpMessagingTemplate, times(1)).convertAndSend(
                    eq("/init_chat/" + idCurso),
                    any(CursoChatDTO.class));
        }

        @Test
        void notifyCoursesChat_cursoNoEncontrado() {
            Document doc = new Document()
                    .append("idCurso", idCurso)
                    .append("mensajes", List.of())
                    .append("clases", new ArrayList<>());

            when(cursoRepo.findById(idCurso)).thenReturn(Optional.empty());

            EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> {
                initChatService.notifyCoursesChat(doc);
            });
            assertEquals("Error en obtener el curso con ID " + idCurso, e.getMessage());
        }
    }

    // ==========================================
    // 11. TESTS DE notifyUsersChat()
    // ==========================================
    @Nested
    class NotifyUsersChatTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;

        @Test
        void notifyUsersChat_success() {
            Document doc = new Document()
                    .append("idUsuario", idUsuario)
                    .append("mensajes", new ArrayList<>())
                    .append("cursos", new ArrayList<>());

            initChatService.notifyUsersChat(doc);

            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(idUsuario.toString()),
                    eq("/init_chat/result"),
                    any(InitChatDTO.class));
        }

        @Test
        void notifyUsersChat_conMensajes() {
            Document doc = new Document()
                    .append("idUsuario", idUsuario)
                    .append("mensajes", List.of("msg1"))
                    .append("cursos", new ArrayList<>());

            MensajeChat msg = new MensajeChat();
            msg.setId("msg1");
            msg.setIdUsuario(idUsuario);
            msg.setIdCurso(idCurso);
            msg.setMensaje("contenido");
            msg.setRespuesta(null);
            msg.setMomento(1);
            msg.setFecha(new Date());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(msg));
            when(usuarioRepo.findNombreUsuarioForId(idUsuario)).thenReturn(Optional.of("Juan"));
            when(usuarioRepo.findFotosUsuarioForId(idUsuario)).thenReturn(List.of("foto.jpg"));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            initChatService.notifyUsersChat(doc);

            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(idUsuario.toString()),
                    eq("/init_chat/result"),
                    any());
        }

        @Test
        void notifyUsersChat_conCursos() {
            Document doc = new Document()
                    .append("idUsuario", idUsuario)
                    .append("mensajes", new ArrayList<>())
                    .append("cursos", List.of("mongoCursoId"));

            CursoChat cursoChat = new CursoChat();
            cursoChat.setId("mongoCursoId");
            cursoChat.setIdCurso(idCurso);
            cursoChat.setClases(new ArrayList<>());

            Curso curso = new Curso();
            curso.setIdCurso(idCurso);
            curso.setNombreCurso("Java");
            curso.setImagenCurso("foto.png");
            curso.setClasesCurso(new ArrayList<>());

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cursoChat));
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(curso));

            initChatService.notifyUsersChat(doc);

            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(idUsuario.toString()),
                    eq("/init_chat/result"),
                    any());
        }
    }

    // ==========================================
    // 12. TESTS DE observeMultipleCollections()
    // ==========================================
    @Nested
    class ObserverTests {
        private Long idUsuario = 1L;
        private Long idCurso = 100L;

        @Test
        @SuppressWarnings("unchecked")
        void observeMultipleCollections_FullIntegration_UserChat_Coverage() {
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

            ClaseChat claseChat = new ClaseChat();
            claseChat.setIdClase(50L);
            claseChat.setIdCurso(idCurso);
            claseChat.setMensajes(new ArrayList<>());

            CursoChat cc = new CursoChat();
            cc.setIdCurso(idCurso);
            cc.setClases(List.of(claseChat));

            when(cursoChatRepo.findAllById(anyList())).thenReturn(List.of(cc));
            when(claseRepo.findNombreClaseById(50L)).thenReturn(Optional.of("Clase de Java"));

            Curso c = new Curso();
            c.setNombreCurso("Java");
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(c));

            initChatService.observeMultipleCollections();

            verify(simpMessagingTemplate, timeout(2000)).convertAndSendToUser(
                    eq(idUsuario.toString()),
                    eq("/init_chat/result"),
                    any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void observeMultipleCollections_FullIntegration_CourseChat_Coverage() {
            Document courseDoc = new Document()
                    .append("idCurso", idCurso)
                    .append("mensajes", List.of())
                    .append("clases", List.of());

            ChangeStreamEvent<Document> mockEvent = mock(ChangeStreamEvent.class);
            when(mockEvent.getBody()).thenReturn(courseDoc);

            when(reactiveMongoTemplate.changeStream(eq("courses_chat"), any(), eq(Document.class)))
                    .thenReturn(Flux.just(mockEvent));
            when(reactiveMongoTemplate.changeStream(eq("users_chat"), any(), eq(Document.class)))
                    .thenReturn(Flux.empty());

            Curso c = new Curso();
            c.setNombreCurso("Java");
            when(cursoRepo.findById(idCurso)).thenReturn(Optional.of(c));

            initChatService.observeMultipleCollections();

            verify(simpMessagingTemplate, timeout(2000)).convertAndSend(
                    eq("/init_chat/" + idCurso),
                    any(CursoChatDTO.class));
        }
    }

    // ==========================================
    // MOCKS
    // ==========================================
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
