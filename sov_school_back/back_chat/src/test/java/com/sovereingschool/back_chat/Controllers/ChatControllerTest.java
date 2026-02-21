package com.sovereingschool.back_chat.Controllers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_chat.DTOs.CursoChatDTO;
import com.sovereingschool.back_chat.DTOs.InitChatDTO;
import com.sovereingschool.back_chat.Services.CursoChatService;
import com.sovereingschool.back_chat.Services.InitChatService;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Usuario;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para ChatController")
class ChatControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private InitChatService initChatService;

    @Mock
    private CursoChatService cursoChatService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        objectMapper = new ObjectMapper();
    }

    /* ======================= Tests para WebSocket ======================= */

    @Test
    @DisplayName("handleInitChat - debe inicializar el chat exitosamente")
    void testHandleInitChat_Success() {
        // Arrange
        Long idUsuario = 1L;
        InitChatDTO expectedResult = new InitChatDTO(idUsuario, new ArrayList<>(), new ArrayList<>());

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(idUsuario);
        when(initChatService.initChat(idUsuario)).thenReturn(expectedResult);

        // Act
        Object result = chatController.handleInitChat();

        // Assert
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(initChatService, times(1)).initChat(idUsuario);
    }

    @Test
    @DisplayName("handleInitChat - debe manejar usuario no autenticado")
    void testHandleInitChat_NotAuthenticated() {
        // Arrange
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        Object result = chatController.handleInitChat();

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().contains("Token inválido"));
        verify(initChatService, never()).initChat(anyLong());
    }

    @Test
    @DisplayName("handleInitChat - debe lanzar EntityNotFoundException")
    void testHandleInitChat_EntityNotFound() {
        // Arrange
        Long idUsuario = 1L;
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(idUsuario);
        when(initChatService.initChat(idUsuario)).thenThrow(
                new EntityNotFoundException("Usuario no encontrado"));

        // Act
        Object result = chatController.handleInitChat();

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().contains("Usuario no encontrado"));
    }

    @Test
    @DisplayName("handleInitChat - debe lanzar IllegalArgumentException")
    void testHandleInitChat_IllegalArgument() {
        // Arrange
        Long idUsuario = 1L;
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(idUsuario);
        when(initChatService.initChat(idUsuario)).thenThrow(
                new IllegalArgumentException("Argumento inválido"));

        // Act
        Object result = chatController.handleInitChat();

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().contains("Argumento inválido"));
    }

    @Test
    @DisplayName("handleInitChat - debe manejar excepciones generales")
    void testHandleInitChat_GeneralException() {
        // Arrange
        Long idUsuario = 1L;
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(idUsuario);
        when(initChatService.initChat(idUsuario)).thenThrow(
                new RuntimeException("Error inesperado"));

        // Act
        Object result = chatController.handleInitChat();

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().contains("Error en obtener en init del chat"));
    }

    @Test
    @DisplayName("getCursoChat - debe obtener el chat del curso exitosamente")
    void testGetCursoChat_Success() {
        // Arrange
        Long idCurso = 1L;
        CursoChatDTO cursoChatDTO = new CursoChatDTO(idCurso, null, null, "Curso Test", null);
        when(cursoChatService.getCursoChat(idCurso)).thenReturn(cursoChatDTO);

        // Act
        chatController.getCursoChat(idCurso.toString());

        // Assert
        verify(cursoChatService, times(1)).getCursoChat(idCurso);
        verify(messagingTemplate, times(1)).convertAndSend("/init_chat/" + idCurso,
                cursoChatDTO);
    }

    @Test
    @DisplayName("getCursoChat - debe manejar EntityNotFoundException")
    void testGetCursoChat_EntityNotFound() {
        // Arrange
        Long idCurso = 1L;
        when(cursoChatService.getCursoChat(idCurso)).thenThrow(
                new EntityNotFoundException("Curso no encontrado"));

        // Act
        chatController.getCursoChat(idCurso.toString());

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/init_chat/" + idCurso),
                contains("Curso no encontrado"));
    }

    @Test
    @DisplayName("getCursoChat - debe manejar excepciones generales")
    void testGetCursoChat_GeneralException() {
        // Arrange
        Long idCurso = 1L;
        when(cursoChatService.getCursoChat(idCurso)).thenThrow(
                new RuntimeException("Error inesperado"));

        // Act
        chatController.getCursoChat(idCurso.toString());

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/init_chat/" + idCurso),
                contains("Error en obtener el chat del curso"));
    }

    @Test
    @DisplayName("guardaMensaje - debe guardar el mensaje exitosamente")
    void testGuardaMensaje_Success() {
        // Arrange
        String message = "Test message";
        doNothing().when(cursoChatService).guardaMensaje(message);

        // Act
        chatController.guardaMensaje(message);

        // Assert
        verify(cursoChatService, times(1)).guardaMensaje(message);
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("guardaMensaje - debe manejar IllegalArgumentException")
    void testGuardaMensaje_IllegalArgument() {
        // Arrange
        String message = "Invalid message";
        doThrow(new IllegalArgumentException("Mensaje inválido"))
                .when(cursoChatService).guardaMensaje(message);

        // Act
        chatController.guardaMensaje(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/chat/"),
                contains("Mensaje inválido"));
    }

    @Test
    @DisplayName("guardaMensaje - debe manejar NoSuchElementException")
    void testGuardaMensaje_NoSuchElement() {
        // Arrange
        String message = "Test message";
        doThrow(new NoSuchElementException("Elemento no encontrado"))
                .when(cursoChatService).guardaMensaje(message);

        // Act
        chatController.guardaMensaje(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/chat/"),
                contains("Elemento no encontrado"));
    }

    @Test
    @DisplayName("guardaMensaje - debe manejar DataAccessException")
    void testGuardaMensaje_DataAccessException() {
        // Arrange
        String message = "Test message";
        doThrow(new DataAccessException("Error de base de datos") {
        })
                .when(cursoChatService).guardaMensaje(message);

        // Act
        chatController.guardaMensaje(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/chat/"),
                contains("Error de base de datos"));
    }

    @Test
    @DisplayName("guardaMensaje - debe manejar excepciones generales")
    void testGuardaMensaje_GeneralException() {
        // Arrange
        String message = "Test message";
        doThrow(new RuntimeException("Error inesperado"))
                .when(cursoChatService).guardaMensaje(message);

        // Act
        chatController.guardaMensaje(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/chat/"),
                contains("Error en guardar mensaje"));
    }

    @Test
    @DisplayName("mensajeLeido - debe marcar el mensaje como leído exitosamente")
    void testMensajeLeido_Success() {
        // Arrange
        String message = "Test message";
        doNothing().when(cursoChatService).mensajeLeido(message);

        // Act
        chatController.mensajeLeido(message);

        // Assert
        verify(cursoChatService, times(1)).mensajeLeido(message);
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("mensajeLeido - debe manejar EntityNotFoundException")
    void testMensajeLeido_EntityNotFound() {
        // Arrange
        String message = "Test message";
        doThrow(new EntityNotFoundException("Mensaje no encontrado"))
                .when(cursoChatService).mensajeLeido(message);

        // Act
        chatController.mensajeLeido(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/leido"),
                contains("Mensaje no encontrado"));
    }

    @Test
    @DisplayName("mensajeLeido - debe manejar excepciones generales")
    void testMensajeLeido_GeneralException() {
        // Arrange
        String message = "Test message";
        doThrow(new RuntimeException("Error inesperado"))
                .when(cursoChatService).mensajeLeido(message);

        // Act
        chatController.mensajeLeido(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(eq("/leido"),
                contains("Error en marcar el mensaje como leido"));
    }

    @Test
    @DisplayName("refreshToken - debe refrescar el token exitosamente")
    void testRefreshToken_Success() {
        // Arrange
        String sessionId = "session-123";
        String newToken = "new-token-456";
        doNothing().when(cursoChatService).refreshTokenInOpenWebsocket(sessionId, newToken);

        // Act
        chatController.refreshToken(sessionId, newToken);

        // Assert
        verify(cursoChatService, times(1)).refreshTokenInOpenWebsocket(sessionId, newToken);
    }

    @Test
    @DisplayName("refreshToken - debe lanzar RuntimeException en caso de error")
    void testRefreshToken_Error() {
        // Arrange
        String sessionId = "session-123";
        String newToken = "new-token-456";
        doThrow(new RuntimeException("Error al refrescar token"))
                .when(cursoChatService).refreshTokenInOpenWebsocket(sessionId, newToken);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            chatController.refreshToken(sessionId, newToken);
        });
        verify(messagingTemplate, times(1)).convertAndSend(eq("/refresh-token"),
                contains("Error en refrescar el token"));
    }

    /* ======================= Tests para REST ======================= */

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaUsuarioChat - debe crear un usuario de chat exitosamente")
    void testCreaUsuarioChat_Success() throws Exception {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombreUsuario("Test User");

        String usuarioJson = objectMapper.writeValueAsString(usuario);

        doNothing().when(cursoChatService).creaUsuarioChat(any(Usuario.class));

        // Act & Assert
        mockMvc.perform(post("/crea_usuario_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(usuarioJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario chat creado con exito!!!"));

        verify(cursoChatService, times(1)).creaUsuarioChat(any(Usuario.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaUsuarioChat - debe manejar RuntimeException")
    void testCreaUsuarioChat_RuntimeException() throws Exception {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        String usuarioJson = objectMapper.writeValueAsString(usuario);

        doThrow(new RuntimeException("Error en base de datos"))
                .when(cursoChatService).creaUsuarioChat(any(Usuario.class));

        // Act & Assert
        mockMvc.perform(post("/crea_usuario_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(usuarioJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error en base de datos"));

        verify(cursoChatService, times(1)).creaUsuarioChat(any(Usuario.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaUsuarioChat - debe manejar JSON inválido")
    void testCreaUsuarioChat_InvalidJson() throws Exception {
        // Arrange
        String invalidJson = "{ invalid json }";

        // Act & Assert
        mockMvc.perform(post("/crea_usuario_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error al parsear JSON")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaUsuarioChat - debe manejar excepciones generales")
    void testCreaUsuarioChat_GeneralException() throws Exception {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);

        String usuarioJson = objectMapper.writeValueAsString(usuario);

        doThrow(new IllegalStateException("Estado inválido"))
                .when(cursoChatService).creaUsuarioChat(any(Usuario.class));

        // Act & Assert
        mockMvc.perform(post("/crea_usuario_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(usuarioJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaCursoChat - debe crear un curso de chat exitosamente")
    void testCreaCursoChat_Success() throws Exception {
        // Arrange
        String cursoJson = "{\"id\":1,\"nombre\":\"Curso Test\"}";
        doNothing().when(cursoChatService).creaCursoChat(anyString());

        // Act & Assert
        mockMvc.perform(post("/crea_curso_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cursoJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Curso chat creado con exito!!!"));

        verify(cursoChatService, times(1)).creaCursoChat(cursoJson);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaCursoChat - debe manejar RuntimeException")
    void testCreaCursoChat_RuntimeException() throws Exception {
        // Arrange
        String cursoJson = "{\"id\":1,\"nombre\":\"Curso Test\"}";
        doThrow(new RuntimeException("Error al crear curso"))
                .when(cursoChatService).creaCursoChat(anyString());

        // Act & Assert
        mockMvc.perform(post("/crea_curso_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cursoJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al crear curso"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaCursoChat - debe manejar excepciones generales")
    void testCreaCursoChat_GeneralException() throws Exception {
        // Arrange
        String cursoJson = "{\"id\":1,\"nombre\":\"Curso Test\"}";
        doThrow(new IllegalStateException("Estado inválido"))
                .when(cursoChatService).creaCursoChat(anyString());

        // Act & Assert
        mockMvc.perform(post("/crea_curso_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cursoJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaClaseChat - debe crear una clase de chat exitosamente")
    void testCreaClaseChat_Success() throws Exception {
        // Arrange
        String claseJson = "{\"id\":1,\"nombre\":\"Clase Test\"}";
        doNothing().when(cursoChatService).creaClaseChat(anyString());

        // Act & Assert
        mockMvc.perform(post("/crea_clase_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(claseJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Clase chat creado con exito!!!"));

        verify(cursoChatService, times(1)).creaClaseChat(claseJson);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaClaseChat - debe manejar RuntimeException")
    void testCreaClaseChat_RuntimeException() throws Exception {
        // Arrange
        String claseJson = "{\"id\":1,\"nombre\":\"Clase Test\"}";
        doThrow(new RuntimeException("Error al crear clase"))
                .when(cursoChatService).creaClaseChat(anyString());

        // Act & Assert
        mockMvc.perform(post("/crea_clase_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(claseJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al crear clase"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("creaClaseChat - debe manejar excepciones generales")
    void testCreaClaseChat_GeneralException() throws Exception {
        // Arrange
        String claseJson = "{\"id\":1,\"nombre\":\"Clase Test\"}";
        doThrow(new IllegalStateException("Estado inválido"))
                .when(cursoChatService).creaClaseChat(anyString());

        // Act & Assert
        mockMvc.perform(post("/crea_clase_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(claseJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarClaseChat - debe borrar una clase de chat exitosamente")
    void testBorrarClaseChat_Success() throws Exception {
        // Arrange
        Long idCurso = 1L;
        Long idClase = 1L;
        doNothing().when(cursoChatService).borrarClaseChat(idCurso, idClase);

        // Act & Assert
        mockMvc.perform(delete("/delete_clase_chat/{idCurso}/{idClase}", idCurso, idClase))
                .andExpect(status().isOk())
                .andExpect(content().string("Clase chat borrado con exito!!!"));

        verify(cursoChatService, times(1)).borrarClaseChat(idCurso, idClase);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarClaseChat - debe manejar EntityNotFoundException")
    void testBorrarClaseChat_EntityNotFound() throws Exception {
        // Arrange
        Long idCurso = 1L;
        Long idClase = 1L;
        doThrow(new EntityNotFoundException("Clase no encontrada"))
                .when(cursoChatService).borrarClaseChat(idCurso, idClase);

        // Act & Assert
        mockMvc.perform(delete("/delete_clase_chat/{idCurso}/{idClase}", idCurso, idClase))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Clase no encontrada"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarClaseChat - debe manejar excepciones generales")
    void testBorrarClaseChat_GeneralException() throws Exception {
        // Arrange
        Long idCurso = 1L;
        Long idClase = 1L;
        doThrow(new RuntimeException("Error al borrar"))
                .when(cursoChatService).borrarClaseChat(idCurso, idClase);

        // Act & Assert
        mockMvc.perform(delete("/delete_clase_chat/{idCurso}/{idClase}", idCurso, idClase))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error en borrar la clase del chat")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarCursoChat - debe borrar un curso de chat exitosamente")
    void testBorrarCursoChat_Success() throws Exception {
        // Arrange
        Long idCurso = 1L;
        doNothing().when(cursoChatService).borrarCursoChat(idCurso);

        // Act & Assert
        mockMvc.perform(delete("/delete_curso_chat/{idCurso}", idCurso))
                .andExpect(status().isOk())
                .andExpect(content().string("Curso chat borrado con exito!!!"));

        verify(cursoChatService, times(1)).borrarCursoChat(idCurso);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarCursoChat - debe manejar EntityNotFoundException")
    void testBorrarCursoChat_EntityNotFound() throws Exception {
        // Arrange
        Long idCurso = 1L;
        doThrow(new EntityNotFoundException("Curso no encontrado"))
                .when(cursoChatService).borrarCursoChat(idCurso);

        // Act & Assert
        mockMvc.perform(delete("/delete_curso_chat/{idCurso}", idCurso))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Curso no encontrado"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarCursoChat - debe manejar excepciones generales")
    void testBorrarCursoChat_GeneralException() throws Exception {
        // Arrange
        Long idCurso = 1L;
        doThrow(new RuntimeException("Error al borrar"))
                .when(cursoChatService).borrarCursoChat(idCurso);

        // Act & Assert
        mockMvc.perform(delete("/delete_curso_chat/{idCurso}", idCurso))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error en borrar el chat del curso")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarUsuarioChat - debe borrar un usuario de chat exitosamente")
    void testBorrarUsuarioChat_Success() throws Exception {
        // Arrange
        Long idUsuario = 1L;
        doNothing().when(cursoChatService).borrarUsuarioChat(idUsuario);

        // Act & Assert
        mockMvc.perform(delete("/delete_usuario_chat/{idUsuario}", idUsuario))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario chat borrado con exito!!!"));

        verify(cursoChatService, times(1)).borrarUsuarioChat(idUsuario);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarUsuarioChat - debe manejar EntityNotFoundException")
    void testBorrarUsuarioChat_EntityNotFound() throws Exception {
        // Arrange
        Long idUsuario = 1L;
        doThrow(new EntityNotFoundException("Usuario no encontrado"))
                .when(cursoChatService).borrarUsuarioChat(idUsuario);

        // Act & Assert
        mockMvc.perform(delete("/delete_usuario_chat/{idUsuario}", idUsuario))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Usuario no encontrado"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("borrarUsuarioChat - debe manejar excepciones generales")
    void testBorrarUsuarioChat_GeneralException() throws Exception {
        // Arrange
        Long idUsuario = 1L;
        doThrow(new RuntimeException("Error al borrar"))
                .when(cursoChatService).borrarUsuarioChat(idUsuario);

        // Act & Assert
        mockMvc.perform(delete("/delete_usuario_chat/{idUsuario}", idUsuario))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error en borrar el chat del usuario")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("actualizarCursoChat - debe actualizar un curso de chat exitosamente")
    void testActualizarCursoChat_Success() throws Exception {
        // Arrange
        Curso curso = new Curso();
        curso.setIdCurso(1L);
        curso.setNombreCurso("Curso Actualizado");

        String cursoJson = objectMapper.writeValueAsString(curso);
        doNothing().when(cursoChatService).actualizarCursoChat(any(Curso.class));

        // Act & Assert
        mockMvc.perform(post("/actualizar_curso_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cursoJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Curso chat actualizado con éxito!!!"));

        verify(cursoChatService, times(1)).actualizarCursoChat(any(Curso.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("actualizarCursoChat - debe manejar InternalServerException")
    void testActualizarCursoChat_InternalServerException() throws Exception {
        // Arrange
        Curso curso = new Curso();
        curso.setIdCurso(1L);

        String cursoJson = objectMapper.writeValueAsString(curso);
        doThrow(new InternalServerException("Error interno del servidor"))
                .when(cursoChatService).actualizarCursoChat(any(Curso.class));

        // Act & Assert
        mockMvc.perform(post("/actualizar_curso_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cursoJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error interno del servidor"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("actualizarCursoChat - debe manejar excepciones generales")
    void testActualizarCursoChat_GeneralException() throws Exception {
        // Arrange
        Curso curso = new Curso();
        curso.setIdCurso(1L);

        String cursoJson = objectMapper.writeValueAsString(curso);
        doThrow(new RuntimeException("Error inesperado"))
                .when(cursoChatService).actualizarCursoChat(any(Curso.class));

        // Act & Assert
        mockMvc.perform(post("/actualizar_curso_chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cursoJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error en actualizar el chat del curso")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getAllCursosChat - debe obtener todos los cursos del chat")
    void testGetAllCursosChat_Success() throws Exception {
        // Arrange
        List<CursoChatDTO> cursos = new ArrayList<>();
        CursoChatDTO cursoChat1 = new CursoChatDTO(1L, null, null, "Curso Test", null);
        cursos.add(cursoChat1);

        when(cursoChatService.getAllCursosChat()).thenReturn(cursos);

        // Act & Assert
        mockMvc.perform(get("/getAll"))
                .andExpect(status().isOk());

        verify(cursoChatService, times(1)).getAllCursosChat();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getAllCursosChat - debe manejar RuntimeException")
    void testGetAllCursosChat_RuntimeException() throws Exception {
        // Arrange
        when(cursoChatService.getAllCursosChat()).thenThrow(
                new RuntimeException("Error al obtener cursos"));

        // Act & Assert
        mockMvc.perform(get("/getAll"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al obtener cursos"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getAllCursosChat - debe manejar excepciones generales")
    void testGetAllCursosChat_GeneralException() throws Exception {
        // Arrange
        when(cursoChatService.getAllCursosChat()).thenThrow(
                new IllegalStateException("Estado inválido"));

        // Act & Assert
        mockMvc.perform(get("/getAll"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("init - debe rellenar la base de datos exitosamente")
    void testInit_Success() throws Exception {
        // Arrange
        doNothing().when(cursoChatService).init();

        // Act & Assert
        mockMvc.perform(get("/init"))
                .andExpect(status().isOk())
                .andExpect(content().string("Iniciado chat con exito!!!"));

        verify(cursoChatService, times(1)).init();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("init - debe manejar excepciones generales")
    void testInit_GeneralException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Error al inicializar"))
                .when(cursoChatService).init();

        // Act & Assert
        mockMvc.perform(get("/init"))
                .andExpect(status().isInternalServerError());

        verify(cursoChatService, times(1)).init();
    }
}
