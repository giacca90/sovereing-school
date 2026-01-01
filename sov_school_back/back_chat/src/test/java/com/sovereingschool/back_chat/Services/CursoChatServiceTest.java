package com.sovereingschool.back_chat.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_chat.DTOs.CursoChatDTO;
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
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CursoChatServiceTest {
    // ==========================
    // Tests getCursoChat()
    // ==========================
    @Nested
    class GetCursoChatTests {
        Long cursoId = 1L;
        String nombreCurso = "Curso 1";
        String nombreClase1 = "Clase 1";
        String nombreClase2 = "Clase 2";
        String imagenCurso = "https://example.com/curso1.png";
        String idMex1clase1 = "idMex1clase1";
        String idMex2clase1 = "idMex2clase1";
        String idMex1clase2 = "idMex1clase2";
        String idMex2clase2 = "idMex2clase2";
        String idMex1curso = "idMex1curso";
        String idMex2curso = "idMex2curso";
        String textoMex1clase1 = "Mensaje 1 Clase 1";
        String textoMex2clase1 = "Mensaje 2 Clase 1";
        String textoMex1clase2 = "Mensaje 1 Clase 2";
        String textoMex2clase2 = "Mensaje 2 Clase 2";
        String textoMex1curso = "Mensaje 1 Curso";
        String textoMex2curso = "Mensaje 2 Curso";

        ClaseChat claseChat1 = new ClaseChat();
        ClaseChat claseChat2 = new ClaseChat();

        CursoChat cursoChat = new CursoChat();
        Clase clase1 = new Clase();
        Clase clase2 = new Clase();
        Curso curso = new Curso();
        MensajeChat mex1clase1 = new MensajeChat();
        MensajeChat mex2clase1 = new MensajeChat();
        MensajeChat mex1clase2 = new MensajeChat();
        MensajeChat mex2clase2 = new MensajeChat();
        MensajeChat mex1curso = new MensajeChat();
        MensajeChat mex2curso = new MensajeChat();

        @BeforeEach
        void setUp() {

            claseChat1.setIdClase(1L);
            claseChat1.setIdCurso(cursoId);
            claseChat1.setMensajes(List.of(idMex1clase1, idMex2clase1));

            claseChat2.setIdClase(2L);
            claseChat2.setIdCurso(cursoId);
            claseChat2.setMensajes(List.of(idMex1clase2, idMex2clase2));

            cursoChat.setIdCurso(cursoId);
            cursoChat.setClases(List.of(claseChat1, claseChat2));
            cursoChat.setMensajes(List.of(idMex1curso, idMex2curso));

            clase1.setIdClase(1L);
            clase1.setNombreClase(nombreClase1);

            clase2.setIdClase(2L);
            clase2.setNombreClase(nombreClase2);

            curso.setIdCurso(cursoId);
            curso.setNombreCurso(nombreCurso);
            curso.setImagenCurso(imagenCurso);
            curso.setClasesCurso(List.of(clase1, clase2));

            mex1clase1.setId(idMex1clase1);
            mex1clase1.setIdCurso(cursoId);
            mex1clase1.setIdClase(1L);
            mex1clase1.setIdUsuario(1L);
            mex1clase1.setFecha(new Date());
            mex1clase1.setMensaje(textoMex1clase1);

            mex2clase1.setId(idMex2clase1);
            mex2clase1.setIdCurso(cursoId);
            mex2clase1.setIdClase(1L);
            mex2clase1.setIdUsuario(1L);
            mex2clase1.setFecha(new Date());
            mex2clase1.setMensaje(textoMex2clase1);

            mex1clase2.setId(idMex1clase2);
            mex1clase2.setIdCurso(cursoId);
            mex1clase2.setIdClase(2L);
            mex1clase2.setIdUsuario(1L);
            mex1clase2.setFecha(new Date());
            mex1clase2.setMensaje(textoMex1clase2);

            mex2clase2.setId(idMex2clase2);
            mex2clase2.setIdCurso(cursoId);
            mex2clase2.setIdClase(2L);
            mex2clase2.setIdUsuario(1L);
            mex2clase2.setFecha(new Date());
            mex2clase2.setMensaje(textoMex2clase2);

            mex1curso.setId(idMex1curso);
            mex1curso.setIdCurso(cursoId);
            mex1curso.setIdClase(null);
            mex1curso.setIdUsuario(1L);
            mex1curso.setFecha(new Date());
            mex1curso.setMensaje(textoMex1curso);

            mex2curso.setId(idMex2curso);
            mex2curso.setIdCurso(cursoId);
            mex2curso.setIdClase(null);
            mex2curso.setIdUsuario(1L);
            mex2curso.setFecha(new Date());
            mex2curso.setMensaje(textoMex2curso);

        }

        @Test
        void getCursoChatTest_success() {
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));
            when(mensajeChatRepo.findAllById(anyList()))
                    .thenReturn(List.of(mex1clase1, mex2clase1, mex1clase2, mex2clase2, mex1curso, mex2curso));

            CursoChatDTO cursoChatDTO = cursoChatService.getCursoChat(cursoId);
            assertEquals(cursoId, cursoChatDTO.idCurso());
            assertEquals(nombreCurso, cursoChatDTO.nombreCurso());
            assertEquals(nombreClase1, cursoChatDTO.clases().get(0).nombreClase());
            assertEquals(nombreClase2, cursoChatDTO.clases().get(1).nombreClase());
        }

        @Test
        void getCursoChatTest_noFound() {
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.getCursoChat(cursoId));
        }

        @Test
        void getCursoChatTest_noFound2() {
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.getCursoChat(cursoId));
        }
    }

    // ==========================
    // Tests guardaMensaje()
    // ==========================
    @Nested
    class GuardaMensajeTests {
        MensajeChatDTO mensajeDTO;
        MensajeChatDTO respuestaDTO;
        MensajeChat mensaje;

        CursoChat cursoChat;
        UsuarioChat usuarioChat;
        UsuarioChat profChat;

        Curso curso;
        Usuario profe;

        Long cursoId = 1L;

        ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
            // 1. DTOs (Datos de entrada)
            respuestaDTO = new MensajeChatDTO(
                    "idRespuesta", cursoId, 1L, 2L, "nombreCurso", "nombreClase",
                    "nombreUsuario", "fotoCurso", "fotoUsuario", null, null, "original", new Date());

            mensajeDTO = new MensajeChatDTO(
                    "idMensaje", cursoId, 1L, 1L, "nombreCurso", "nombreClase",
                    "nombreUsuario", "fotoCurso", "fotoUsuario", respuestaDTO, 30, "mensaje", new Date());

            // 2. Mensaje que devuelve el save
            mensaje = new MensajeChat();
            mensaje.setId("idMensaje");
            mensaje.setIdCurso(cursoId);
            mensaje.setIdClase(1L);
            mensaje.setIdUsuario(1L);
            mensaje.setFecha(new Date());
            mensaje.setMensaje("mensaje");
            mensaje.setRespuesta("idRespuesta");

            // 3. Configuración de ClaseChat (Importante para el filter)
            ClaseChat claseChat = new ClaseChat();
            claseChat.setIdClase(1L); // <--- Debe coincidir con mensajeDTO.idClase()
            claseChat.setMensajes(new ArrayList<>()); // Mutable

            // 4. CursoChat
            cursoChat = new CursoChat();
            cursoChat.setId("idCursoMongo");
            cursoChat.setIdCurso(cursoId);
            cursoChat.setMensajes(new ArrayList<>(List.of("idAnterior"))); // Mutable
            cursoChat.setClases(new ArrayList<>(List.of(claseChat))); // Mutable

            // 5. Usuario que envía el mensaje
            usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(1L);
            usuarioChat.setCursos(new ArrayList<>(List.of())); // Mutable
            usuarioChat.setMensajes(new ArrayList<>()); // Mutable

            profChat = new UsuarioChat();
            profChat.setIdUsuario(2L);
            profChat.setMensajes(new ArrayList<>()); // Mutable

            // 6. Configuración para Notificar Profesores
            profe = new Usuario();
            profe.setIdUsuario(2L);
            profe.setNombreUsuario("profe");

            curso = new Curso();
            curso.setIdCurso(cursoId);
            curso.setProfesoresCurso(List.of(profe));
        }

        @Test
        void guardaMensajeTest_success_claseMessage() throws JsonProcessingException {
            // 1. Preparación
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mocks Obligatorios (Siempre se ejecutan)
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChat);
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);

            // Parte de Profesores
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));
            when(usuarioChatRepo.findByIdUsuario(2L)).thenReturn(Optional.of(profChat));

            // Parte de Respuesta
            MensajeChat mensajeOriginal = new MensajeChat();
            mensajeOriginal.setId("idRespuesta");
            mensajeOriginal.setIdUsuario(2L);
            when(mensajeChatRepo.findById("idRespuesta")).thenReturn(Optional.of(mensajeOriginal));

            // 4. Ejecución
            cursoChatService.guardaMensaje(jsonMessage);

            // 5. Verificaciones
            verify(mensajeChatRepo).save(any(MensajeChat.class));
            verify(cursoChatRepo).save(any(CursoChat.class));
            verify(usuarioChatRepo, atLeastOnce()).save(any(UsuarioChat.class));
        }

        @Test
        void guardaMensajeTest_success_cursoMessage() throws JsonProcessingException {
            // 1. Preparación
            mensajeDTO = new MensajeChatDTO(
                    "idMensaje", cursoId, null, 1L, "nombreCurso", "nombreClase",
                    "nombreUsuario", "fotoCurso", "fotoUsuario", respuestaDTO, 30, "mensaje", new Date());

            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mocks Obligatorios (Siempre se ejecutan)
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChat);
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);

            // 3. Mocks Condicionales (Usamos lenient() para evitar el
            // UnnecessaryStubbingException)
            // Esto es útil si el flujo de profesores o respuesta varía

            // Parte de Profesores
            lenient().when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));
            lenient().when(usuarioChatRepo.findByIdUsuario(2L)).thenReturn(Optional.of(profChat));

            // Parte de Respuesta
            MensajeChat mensajeOriginal = new MensajeChat();
            mensajeOriginal.setId("idRespuesta");
            mensajeOriginal.setIdUsuario(2L);
            lenient().when(mensajeChatRepo.findById("idRespuesta")).thenReturn(Optional.of(mensajeOriginal));

            // 4. Ejecución
            cursoChatService.guardaMensaje(jsonMessage);

            // 5. Verificaciones
            verify(mensajeChatRepo).save(any(MensajeChat.class));
            verify(cursoChatRepo).save(any(CursoChat.class));
            verify(usuarioChatRepo, atLeastOnce()).save(any(UsuarioChat.class));
        }

        @Test
        void guardaMensajeTest_errorParseJson() {
            // 1. Definimos un input que no es JSON (un String plano)
            String mensajeInvalido = "esto no es un json";

            // 2. Verificamos que se lance IllegalArgumentException
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                cursoChatService.guardaMensaje(mensajeInvalido);
            });

            // 3. Verificamos que el mensaje de la excepción contenga el texto que pusiste
            // en tu código
            assertTrue(exception.getMessage().contains("Formato JSON inválido"));

            // 4. Opcional: Verificar que nunca se llamó a los repositorios
            verifyNoInteractions(mensajeChatRepo, cursoChatRepo, usuarioChatRepo);
        }

        @Test
        void guardaMensajeTest_errorCursoChatNoEncontrado() throws JsonProcessingException {
            // 1. Preparamos un JSON válido para que pase el primer filtro (el parseo)
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mockeamos el primer repositorio (save) para que no falle ahí
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);

            // 3. EL MOCK CLAVE: Hacemos que el curso NO exista en la base de datos
            when(cursoChatRepo.findByIdCurso(mensajeDTO.idCurso())).thenReturn(Optional.empty());

            // 4. Verificamos que se lance la excepción EntityNotFoundException
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                cursoChatService.guardaMensaje(jsonMessage);
            });

            // 5. Validamos el mensaje de error
            assertEquals("Error en obtener el curso del chat", exception.getMessage());

            // 6. Verificación de seguridad: Se intentó buscar pero no se guardó nada
            // después
            verify(cursoChatRepo, times(1)).findByIdCurso(mensajeDTO.idCurso());
            verify(cursoChatRepo, never()).save(any(CursoChat.class));
            verifyNoInteractions(cursoRepo); // El flujo se corta antes de llegar a notificar profesores
        }

        @Test
        void guardaMensajeTest_errorUsuarioChatNoEncontrado() throws JsonProcessingException {
            // 1. Preparamos un JSON válido para que pase el primer filtro (el parseo)
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mockeamos el primer repositorio (save) para que no falle ahí
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));

            // 3. EL MOCK CLAVE: Hacemos que el usuario NO exista en la base de datos
            when(usuarioChatRepo.findByIdUsuario(mensajeDTO.idUsuario())).thenReturn(Optional.empty());

            // 4. Verificamos que se lance la excepción EntityNotFoundException
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                cursoChatService.guardaMensaje(jsonMessage);
            });

            // 5. Validamos el mensaje de error
            assertEquals("Error en obtener el usuario del chat", exception.getMessage());

            // 6. Verificación de seguridad: Se intentó buscar pero no se guardó nada
            // después
            verify(usuarioChatRepo, times(1)).findByIdUsuario(mensajeDTO.idUsuario());
            verify(usuarioChatRepo, never()).save(any(UsuarioChat.class));
        }

        @Test
        void guardaMensajeTest_errorCursoNoEncontrado() throws JsonProcessingException {
            // 1. Preparamos un JSON válido para que pase el primer filtro (el parseo)
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mockeamos el primer repositorio (save) para que no falle ahí
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));

            when(cursoRepo.findById(cursoId)).thenReturn(Optional.empty());
            // 3. Verificamos que se lance la excepción EntityNotFoundException
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                cursoChatService.guardaMensaje(jsonMessage);
            });

            // 4. Validamos el mensaje de error
            assertEquals("Curso no encontrado con id: " + mensajeDTO.idCurso(), exception.getMessage());

            // 5. Verificación de seguridad: Se intentó buscar pero no se guardó nada
            // después
            verify(cursoChatRepo, times(1)).findByIdCurso(cursoId);
        }

        @Test
        void guardaMensajeTest_errorProfesorNoEncontrado() throws JsonProcessingException {

            // 1. Preparación
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mocks Obligatorios (Siempre se ejecutan)
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChat);
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);

            // Parte de Profesores
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));
            when(usuarioChatRepo.findByIdUsuario(2L)).thenReturn(Optional.empty());

            // 4. Ejecución
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                cursoChatService.guardaMensaje(jsonMessage);
            });

            // 5. Verificaciones
            // 4. Validamos el mensaje de error
            assertEquals("Error en obtener el profesor del curso", exception.getMessage());

        }

        @Test
        void guardaMensajeTest_success_errorRespuestaNoEncontrada() throws JsonProcessingException {
            // 1. Preparación
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mocks Obligatorios (Siempre se ejecutan)
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChat);
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);

            // Parte de Profesores
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));
            when(usuarioChatRepo.findByIdUsuario(2L)).thenReturn(Optional.of(profChat));

            // Parte de Respuesta
            when(mensajeChatRepo.findById("idRespuesta")).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                cursoChatService.guardaMensaje(jsonMessage);
            });

            // 5. Verificaciones
            // 4. Validamos el mensaje de error
            assertEquals("No se encontró el mensaje con id idRespuesta", exception.getMessage());

        }

        @Test
        void guardaMensajeTest_success_errorUsuarioRespuestaNoEncontrado() throws JsonProcessingException {
            // 1. Preparación
            String jsonMessage = objectMapper.writeValueAsString(mensajeDTO);

            // 2. Mocks Obligatorios (Siempre se ejecutan)
            when(mensajeChatRepo.save(any(MensajeChat.class))).thenReturn(mensaje);
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChat);
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);

            // Parte de Profesores
            when(cursoRepo.findById(cursoId)).thenReturn(Optional.of(curso));
            when(usuarioChatRepo.findByIdUsuario(2L)).thenReturn(Optional.of(profChat));

            // Parte de Respuesta
            MensajeChat mensajeOriginal = new MensajeChat();
            mensajeOriginal.setId("idRespuesta");
            mensajeOriginal.setIdUsuario(3L);
            when(mensajeChatRepo.findById("idRespuesta")).thenReturn(Optional.of(mensajeOriginal));
            when(usuarioChatRepo.findByIdUsuario(3L)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                cursoChatService.guardaMensaje(jsonMessage);
            });

            // 5. Verificaciones
            // 4. Validamos el mensaje de error
            assertEquals("Error en obtener el usuario del chat en la respuesta", exception.getMessage());

        }

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
