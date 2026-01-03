package com.sovereingschool.back_chat.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.sovereingschool.back_common.Exceptions.InternalServerException;
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
        Usuario usuario;
        UsuarioChat usuarioChat;
        Curso curso1;
        Curso curso2;
        Clase clase1curso1;
        Clase clase2curso1;
        Clase clase1curso2;
        Clase clase2curso2;

        CursoChat cursoChat1;
        CursoChat cursoChat2;

        ClaseChat claseChat1;
        ClaseChat claseChat2;
        ClaseChat claseChat3;
        ClaseChat claseChat4;

        @BeforeEach
        void setUp() {
            // Definición de Clases
            clase1curso1 = new Clase();
            clase1curso1.setIdClase(1L);
            clase1curso1.setNombreClase("clase1curso1");

            clase2curso1 = new Clase();
            clase2curso1.setIdClase(2L);
            clase2curso1.setNombreClase("clase2curso1");

            clase1curso2 = new Clase();
            clase1curso2.setIdClase(3L);
            clase1curso2.setNombreClase("clase1curso2");

            clase2curso2 = new Clase();
            clase2curso2.setIdClase(4L);
            clase2curso2.setNombreClase("clase2curso2");

            // Cursos con listas mutables
            curso1 = new Curso();
            curso1.setIdCurso(1L);
            curso1.setNombreCurso("curso1");
            curso1.setClasesCurso(new ArrayList<>(Arrays.asList(clase1curso1, clase2curso1)));

            curso2 = new Curso();
            curso2.setIdCurso(2L);
            curso2.setNombreCurso("curso2");
            curso2.setClasesCurso(new ArrayList<>(Arrays.asList(clase1curso2, clase2curso2)));

            // Usuario
            usuario = new Usuario();
            usuario.setIdUsuario(1L);
            usuario.setNombreUsuario("nombreUsuario");
            usuario.setCursosUsuario(new ArrayList<>(Arrays.asList(curso1, curso2)));

            usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(1L);
            usuarioChat.setCursos(new ArrayList<>());

            // CursoChat - Asegurando el uso de New ArrayList para evitar
            // UnsupportedOperationException
            cursoChat1 = new CursoChat();
            cursoChat1.setIdCurso(curso1.getIdCurso());
            cursoChat1.setClases(new ArrayList<>(Arrays.asList(claseChat1, claseChat2)));
            cursoChat1.setMensajes(new ArrayList<>(Arrays.asList("idMensaje1", "idMensaje2")));

            cursoChat2 = new CursoChat();
            cursoChat2.setIdCurso(curso2.getIdCurso());
            // Error corregido aquí: faltaba el operador diamante <> y asegurar mutabilidad
            cursoChat2.setClases(new ArrayList<>(Arrays.asList(claseChat3, claseChat4)));
            cursoChat2.setMensajes(new ArrayList<>(Arrays.asList("idMensaje3", "idMensaje4")));
        }

        @Test
        void creaUsuarioChatTest_success() throws InternalServerException {
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);
            when(cursoChatRepo.findByIdCurso(1L)).thenReturn(Optional.of(cursoChat1));
            when(cursoChatRepo.findByIdCurso(2L)).thenReturn(Optional.of(cursoChat2));

            cursoChatService.creaUsuarioChat(usuario);

            verify(usuarioChatRepo, times(2)).save(any(UsuarioChat.class));
        }

        @Test
        void creaUsuarioChatTest_error() {
            when(usuarioChatRepo.save(any(UsuarioChat.class))).thenReturn(usuarioChat);
            when(cursoChatRepo.findByIdCurso(1L)).thenReturn(Optional.empty());

            assertThrows(InternalServerException.class, () -> cursoChatService.creaUsuarioChat(usuario));
        }
    }

    // ==========================
    // Tests creaCursoChat()
    // ==========================
    @Nested
    class CreaCursoChatTests {
        Curso curso;

        Clase clase1;
        Clase clase2;

        ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
            clase1 = new Clase();
            clase1.setIdClase(1L);
            clase1.setNombreClase("clase1");

            clase2 = new Clase();
            clase2.setIdClase(2L);
            clase2.setNombreClase("clase2");

            curso = new Curso();
            curso.setIdCurso(1L);
            curso.setNombreCurso("curso1");
            curso.setClasesCurso(new ArrayList<>(Arrays.asList(clase1, clase2)));
        }

        @Test
        void creaCursoChatTest_success() throws InternalServerException, JsonProcessingException {
            String jsonMessage = objectMapper.writeValueAsString(curso);
            cursoChatService.creaCursoChat(jsonMessage);
            verify(cursoChatRepo).save(any(CursoChat.class));
        }

        @Test
        void creaCursoChatTest_error() throws JsonProcessingException {
            String jsonMessage = objectMapper.writeValueAsString("Texto inválido");
            assertThrows(InternalServerException.class, () -> cursoChatService.creaCursoChat(jsonMessage));
        }
    }

    // ==========================
    // Tests creaClaseChat()
    // ==========================
    @Nested
    class CreaClaseChatTests {
        private Curso curso;
        private Clase clase;
        private CursoChat cursoChat;
        private ClaseChat claseChat;

        @BeforeEach
        void setUp() {
            curso = new Curso();
            curso.setIdCurso(1L);
            curso.setNombreCurso("curso1");
            curso.setClasesCurso(new ArrayList<>());

            clase = new Clase();
            clase.setIdClase(1L);
            clase.setNombreClase("clase1");
            clase.setCursoClase(curso); // Esto ahora sí se incluirá en el JSON

            cursoChat = new CursoChat();
            cursoChat.setIdCurso(curso.getIdCurso());
            cursoChat.setClases(new ArrayList<>());

            claseChat = new ClaseChat();
            claseChat.setIdClase(clase.getIdClase());
            claseChat.setIdCurso(curso.getIdCurso());
        }

        @Test
        void creaClaseChatTest_success() throws InternalServerException {
            // Al generar el JSON, se incluirá el campo "cursoClase" gracias a la
            // configuración del builder
            String jsonMessage = "{"
                    + "\"idClase\":1,"
                    + "\"nombreClase\":\"clase1\","
                    + "\"cursoClase\":{\"idCurso\":1}"
                    + "}";
            // Mocks
            when(cursoChatRepo.findByIdCurso(1L)).thenReturn(Optional.of(cursoChat));

            // Ejecución
            cursoChatService.creaClaseChat(jsonMessage);

            // Verificaciones
            verify(cursoChatRepo).save(any(CursoChat.class));
        }

        @Test
        void creaClaseChatTest_error_cursoChatNoEncontrado() {
            // Al generar el JSON, se incluirá el campo "cursoClase" gracias a la
            // configuración del builder
            String jsonMessage = "{"
                    + "\"idClase\":1,"
                    + "\"nombreClase\":\"clase1\","
                    + "\"cursoClase\":{\"idCurso\":1}"
                    + "}";

            // Mocks
            when(cursoChatRepo.findByIdCurso(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.creaClaseChat(jsonMessage));
        }
    }

    // ==========================
    // Tests mensajeLeido()
    // ==========================
    @Nested
    class MensajeLeidoTests {
        String messageSimulado = "\"1\",\"idMensajePrueba\",\"Test\",\"Hola\",\"2023-06-01\"";
        UsuarioChat usuarioChat;

        @BeforeEach
        void setUp() {
            usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(1L);
            usuarioChat.setMensajes(new ArrayList<>());
        }

        @Test
        void mensajeLeidoTest_success() {
            // Mocks
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioChat));

            // Ejecución - Ahora enviamos "1" en lugar de "{"id":"1"..."
            cursoChatService.mensajeLeido(messageSimulado);

            // Verificaciones - No se llama a save porque no se guarda nada
            verifyNoInteractions(mensajeChatRepo);

        }

        @Test
        void mensajeLeidoTest_error_noMensaje() {
            // Mocks
            String idStringValido = "1";

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.mensajeLeido(idStringValido));
        }
    }

    // ==========================
    // Tests borrarClaseChat()
    // ==========================
    @Nested
    class BorrarClaseChatTests {
        Long cursoId = 1L;
        Long claseId = 2L;

        CursoChat cursoChat;
        ClaseChat claseChat;

        @BeforeEach
        void setUp() {
            cursoChat = new CursoChat();
            cursoChat.setIdCurso(cursoId);
            cursoChat.setClases(new ArrayList<>());

            claseChat = new ClaseChat();
            claseChat.setIdClase(claseId);
            claseChat.setIdCurso(cursoId);

            cursoChat.getClases().add(claseChat);
        }

        @Test
        void borrarClaseChatTest_success() {
            // Mocks
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChat);

            // Ejecución
            cursoChatService.borrarClaseChat(cursoId, claseId);

            // Verificaciones
            verify(cursoChatRepo).save(cursoChat);
        }

        @Test
        void borrarClaseChatTest_error_noCurso() {
            // Mocks
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.borrarClaseChat(cursoId, claseId));
        }
    }

    // ==========================
    // Tests borrarCursoChat()
    // ==========================
    @Nested
    class BorrarCursoChatTests {
        Long cursoId = 1L;

        CursoChat cursoChat;

        @BeforeEach
        void setUp() {
            cursoChat = new CursoChat();
            cursoChat.setIdCurso(cursoId);
            cursoChat.setClases(new ArrayList<>());
        }

        @Test
        void borrarCursoChatTest_success() {
            // Mocks
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.of(cursoChat));

            // Ejecución
            cursoChatService.borrarCursoChat(cursoId);

            // Verificaciones
            verify(cursoChatRepo).delete(cursoChat);
        }

        @Test
        void borrarCursoChatTest_error_noCurso() {
            // Mocks
            when(cursoChatRepo.findByIdCurso(cursoId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.borrarCursoChat(cursoId));
        }
    }

    // ==========================
    // Tests borrarUsuarioChat()
    // ==========================
    @Nested
    class BorrarUsuarioChatTests {
        Long usuarioId = 1L;

        UsuarioChat usuarioChat;

        @BeforeEach
        void setUp() {
            usuarioChat = new UsuarioChat();
            usuarioChat.setIdUsuario(usuarioId);
            usuarioChat.setMensajes(new ArrayList<>());
        }

        @Test
        void borrarUsuarioChatTest_success() {
            // Mocks
            when(usuarioChatRepo.findByIdUsuario(usuarioId)).thenReturn(Optional.of(usuarioChat));

            // Ejecución
            cursoChatService.borrarUsuarioChat(usuarioId);

            // Verificaciones
            verify(usuarioChatRepo).delete(usuarioChat);
        }

        @Test
        void borrarUsuarioChatTest_error_noUsuario() {
            // Mocks
            when(usuarioChatRepo.findByIdUsuario(usuarioId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cursoChatService.borrarUsuarioChat(usuarioId));
        }
    }

    // ==========================
    // Tests refreshTokenInOpenWebsocket()
    // ==========================
    @Nested
    class RefreshTokenInOpenWebsocketTests {
        private String sessionId = "test-session-123";
        private String token = "valid.jwt.token";

        @Test
        void refreshTokenInOpenWebsocket_success() {
            // 1. Setup de la autenticación simulada
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken mockAuth = new UsernamePasswordAuthenticationToken("user@test.com",
                    token, authorities);

            when(jwtUtil.createAuthenticationFromToken(token)).thenReturn(mockAuth);

            // 2. Ejecución
            cursoChatService.refreshTokenInOpenWebsocket(sessionId, token);

            // 3. Verificaciones
            // Comprobar que el contexto global de seguridad se actualizó
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(currentAuth);
            assertEquals("user@test.com", currentAuth.getName());
            assertEquals(token, currentAuth.getCredentials());

            // Verificar interacción con el utilitario
            verify(jwtUtil).createAuthenticationFromToken(token);
        }
    }

    // ==========================
    // Tests init()
    // ==========================
    @Nested
    class InitTests {
        Usuario usuario1;
        Usuario profesor1;
        Curso curso1;
        Clase clase1;
        UsuarioChat usuarioChat1;

        @BeforeEach
        void setUp() {
            // Usuario Alumno
            usuario1 = new Usuario();
            usuario1.setIdUsuario(1L);

            // Profesor
            profesor1 = new Usuario();
            profesor1.setIdUsuario(99L);

            // Clase
            clase1 = new Clase();
            clase1.setIdClase(10L);

            // Curso con su clase y profesor
            curso1 = new Curso();
            curso1.setIdCurso(500L);
            curso1.setClasesCurso(new ArrayList<>(List.of(clase1)));
            curso1.setProfesoresCurso(new ArrayList<>(List.of(profesor1)));

            // UsuarioChat pre-existente para el profesor (necesario para la lógica del
            // curso)
            usuarioChat1 = new UsuarioChat();
            usuarioChat1.setIdUsuario(99L);
            usuarioChat1.setCursos(new ArrayList<>());
        }

        @Test
        void init_Success_CreatesNewData() throws InternalServerException {
            // --- CONFIGURACIÓN DE MOCKS ---
            // 1. Usuarios: usuario1 no existe en Chat, profesor1 sí
            when(usuarioRepo.findAll()).thenReturn(List.of(usuario1, profesor1));
            when(usuarioChatRepo.findByIdUsuario(1L)).thenReturn(Optional.empty()); // Crea este
            when(usuarioChatRepo.findByIdUsuario(99L)).thenReturn(Optional.of(usuarioChat1)); // Salta este

            // 2. Cursos: curso1 no existe en Chat
            when(cursoRepo.findAll()).thenReturn(List.of(curso1));
            when(cursoChatRepo.findByIdCurso(500L)).thenReturn(Optional.empty());

            // --- EJECUCIÓN ---
            cursoChatService.init();

            // --- VERIFICACIONES ---
            // Verificar que se guardó el UsuarioChat que faltaba
            verify(usuarioChatRepo, atLeastOnce()).save(any(UsuarioChat.class));

            // Verificar que se guardó el CursoChat
            verify(cursoChatRepo).save(any(CursoChat.class));

            // Verificar que el profesor fue actualizado con el ID del nuevo curso
            verify(usuarioChatRepo, atLeastOnce()).save(usuarioChat1);
        }

        @Test
        void init_Skips_WhenDataAlreadyExists() throws InternalServerException {
            // Simular que todo ya existe
            when(usuarioRepo.findAll()).thenReturn(List.of(usuario1));
            when(usuarioChatRepo.findByIdUsuario(anyLong())).thenReturn(Optional.of(new UsuarioChat()));

            when(cursoRepo.findAll()).thenReturn(List.of(curso1));
            when(cursoChatRepo.findByIdCurso(anyLong())).thenReturn(Optional.of(new CursoChat()));

            cursoChatService.init();

            // No debería llamar a save para nuevos registros
            verify(usuarioChatRepo, never()).save(any(UsuarioChat.class));
            verify(cursoChatRepo, never()).save(any(CursoChat.class));
        }

        @Test
        void init_ThrowsException_WhenProfessorChatNotFound() {
            // Forzar error: el curso tiene un profesor pero no existe su UsuarioChat
            when(usuarioRepo.findAll()).thenReturn(new ArrayList<>()); // Saltamos primera parte
            when(cursoRepo.findAll()).thenReturn(List.of(curso1));
            when(cursoChatRepo.findByIdCurso(anyLong())).thenReturn(Optional.empty());

            // El profesor 99 no tiene chat
            when(usuarioChatRepo.findByIdUsuario(99L)).thenReturn(Optional.empty());

            // Debe lanzar InternalServerException envolviendo la EntityNotFoundException
            assertThrows(InternalServerException.class, () -> cursoChatService.init());
        }
    }

    // ==========================
    // Tests actualizarCursoChat()
    // ==========================
    @Nested
    class ActualizarCursoChatTests {
        private Curso curso;
        private Usuario profesor;
        private Clase clase1;
        private CursoChat cursoChatExistente;
        private UsuarioChat profChat;

        @BeforeEach
        void setUp() {
            profesor = new Usuario();
            profesor.setIdUsuario(99L);

            clase1 = new Clase();
            clase1.setIdClase(10L);

            curso = new Curso();
            curso.setIdCurso(500L);
            curso.setProfesoresCurso(new ArrayList<>(List.of(profesor)));
            curso.setClasesCurso(new ArrayList<>(List.of(clase1)));

            // Simulación de chats existentes
            profChat = new UsuarioChat();
            profChat.setIdUsuario(99L);
            profChat.setCursos(new ArrayList<>());

            cursoChatExistente = new CursoChat();
            cursoChatExistente.setId("id-curso-chat-777");
            cursoChatExistente.setIdCurso(500L);
            cursoChatExistente.setClases(new ArrayList<>()); // Empieza sin clases
        }

        @Test
        void actualizarCursoChat_CuandoNoExiste_DebeCrearYAsignarAProfesores() throws InternalServerException {
            // Configuramos mocks para que no encuentre el curso pero sí al profesor
            when(cursoChatRepo.findByIdCurso(500L)).thenReturn(Optional.empty());
            when(usuarioChatRepo.findByIdUsuario(99L)).thenReturn(Optional.of(profChat));

            // El save del cursoChat devuelve el objeto con ID (importante para el prof)
            when(cursoChatRepo.save(any(CursoChat.class))).thenAnswer(i -> {
                CursoChat c = i.getArgument(0);
                c.setId("id-curso-chat-777");
                return c;
            });

            // Ejecución
            cursoChatService.actualizarCursoChat(curso);

            // Verificaciones
            verify(cursoChatRepo, atLeastOnce()).save(any(CursoChat.class));
            verify(usuarioChatRepo).save(profChat);
        }

        @Test
        void actualizarCursoChat_CuandoExiste_DebeAnadirSoloNuevasClases() throws InternalServerException {
            // El curso ya existe
            when(cursoChatRepo.findByIdCurso(500L)).thenReturn(Optional.of(cursoChatExistente));

            // Ejecución
            cursoChatService.actualizarCursoChat(curso);

            // Verificaciones
            verify(cursoChatRepo).save(cursoChatExistente);
            assertEquals(1, cursoChatExistente.getClases().size());
            assertEquals(10L, cursoChatExistente.getClases().get(0).getIdClase());

            // No debería interactuar con profesores si el curso ya existía
            verify(usuarioChatRepo, never()).findByIdUsuario(anyLong());
        }

        @Test
        void actualizarCursoChat_CuandoFallaProfe_DebeLanzarExcepcion() {
            // Curso nuevo, pero profesor no encontrado
            when(cursoChatRepo.findByIdCurso(500L)).thenReturn(Optional.empty());
            when(usuarioChatRepo.findByIdUsuario(99L)).thenReturn(Optional.empty());

            // El save inicial del cursoChat (antes de fallar con el profe)
            when(cursoChatRepo.save(any(CursoChat.class))).thenReturn(cursoChatExistente);

            assertThrows(InternalServerException.class, () -> {
                cursoChatService.actualizarCursoChat(curso);
            });
        }

        @Test
        void actualizarCursoChat_ConClasesNulas_NoDebeFallar() throws InternalServerException {
            curso.setClasesCurso(null); // Caso borde
            when(cursoChatRepo.findByIdCurso(500L)).thenReturn(Optional.of(cursoChatExistente));

            cursoChatService.actualizarCursoChat(curso);

            verify(cursoChatRepo).save(cursoChatExistente);
            assertTrue(cursoChatExistente.getClases().isEmpty());
        }
    }

    // ==========================
    // Tests getAllCursosChat()
    // ==========================
    @Nested
    class GetAllCursosChatTests {
        private CursoChat cursoChat;
        private Curso curso;
        private ClaseChat claseChat;
        private MensajeChat mensajeChat;

        @BeforeEach
        void setUp() {
            // Datos de Clase
            claseChat = new ClaseChat(10L, 500L, new ArrayList<>());

            // Datos de CursoChat
            cursoChat = new CursoChat();
            cursoChat.setIdCurso(500L);
            cursoChat.setClases(new ArrayList<>(List.of(claseChat)));
            cursoChat.setMensajes(new ArrayList<>(List.of("msg_1")));

            // Datos de Curso (Entidad relacional)
            curso = new Curso();
            curso.setIdCurso(500L);
            curso.setNombreCurso("Curso de Prueba");
            curso.setImagenCurso("imagen.png");

            // Datos de Mensaje
            mensajeChat = new MensajeChat();
            mensajeChat.setId("msg_1");
        }

        @Test
        void getAllCursosChat_Success() throws InternalServerException {
            // 1. Mock de la lista principal
            when(cursoChatRepo.findAll()).thenReturn(List.of(cursoChat));

            // 2. Mock del nombre de la clase (para evitar el error en .get())
            when(claseRepo.findNombreClaseById(10L)).thenReturn(Optional.of("Nombre Clase Test"));

            // 3. Mock de mensajes
            when(mensajeChatRepo.findAllById(anyList())).thenReturn(List.of(mensajeChat));

            // Simulamos la conversión de mensajes que hace initChatService
            MensajeChatDTO msgDto = new MensajeChatDTO(
                    "msg_1",
                    500L,
                    10L,
                    1L,
                    "Nombre Curso",
                    "Nombre Clase",
                    "Nombre Usuario",
                    "Foto Usuario",
                    "Foto Usuario",
                    null,
                    null,
                    "Mensaje",
                    new Date());
            when(initChatService.getMensajesDTO(anyList())).thenReturn(List.of(msgDto));

            // 4. Mock del curso
            when(cursoRepo.findById(500L)).thenReturn(Optional.of(curso));

            // EJECUCIÓN
            List<CursoChatDTO> resultado = cursoChatService.getAllCursosChat();

            // VERIFICACIONES
            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            assertEquals("Curso de Prueba", resultado.get(0).nombreCurso());
            assertEquals("Nombre Clase Test", resultado.get(0).clases().get(0).nombreClase());
            verify(cursoChatRepo).findAll();
        }

        @Test
        void getAllCursosChat_ReturnsNull_WhenNoCursos() throws InternalServerException {
            when(cursoChatRepo.findAll()).thenReturn(new ArrayList<>());

            List<CursoChatDTO> resultado = cursoChatService.getAllCursosChat();

            assertNull(resultado);
        }

        @Test
        void getAllCursosChat_ThrowsException_WhenCursoNotFound() {
            when(cursoChatRepo.findAll()).thenReturn(List.of(cursoChat));
            when(claseRepo.findNombreClaseById(anyLong())).thenReturn(Optional.of("Nombre"));

            // El curso chat existe pero la entidad Curso no (integridad referencial
            // fallida)
            when(cursoRepo.findById(500L)).thenReturn(Optional.empty());

            assertThrows(InternalServerException.class, () -> {
                cursoChatService.getAllCursosChat();
            });
        }
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
