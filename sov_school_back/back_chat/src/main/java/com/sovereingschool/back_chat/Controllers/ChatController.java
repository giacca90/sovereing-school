package com.sovereingschool.back_chat.Controllers;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sovereingschool.back_chat.DTOs.CursoChatDTO;
import com.sovereingschool.back_chat.Services.CursoChatService;
import com.sovereingschool.back_chat.Services.InitChatService;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Usuario;

import jakarta.persistence.EntityNotFoundException;

@RestController
@PreAuthorize("hasAnyRole('USER', 'PROF', 'ADMIN')")
public class ChatController {

    private SimpMessagingTemplate messagingTemplate;
    private InitChatService initChatService;
    private CursoChatService cursoChatService;

    private Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(SimpMessagingTemplate messagingTemplate, InitChatService initChatService,
            CursoChatService cursoChatService) {
        this.messagingTemplate = messagingTemplate;
        this.initChatService = initChatService;
        this.cursoChatService = cursoChatService;
    }

    /* Sección para el websocket */
    @MessageMapping("/init")
    @SendToUser("/init_chat/result")
    public Object handleInitChat() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return "Token inválido: no autenticado";
            }
            Long idUsuario = (Long) authentication.getDetails(); // 👈 aquí recuperas el ID
            return initChatService.initChat(idUsuario);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return e.getMessage();
        } catch (Exception e) {
            logger.error("Error en el websocket de init: {}", e.getMessage());
            return "Error en obtener en init del chat: " + e.getMessage();
        }
    }

    @MessageMapping("/curso")
    public void getCursoChat(String message) {
        Long idCurso = Long.parseLong(message);
        try {
            CursoChatDTO cursoChat = cursoChatService.getCursoChat(idCurso);
            messagingTemplate.convertAndSend("/init_chat/" + idCurso, cursoChat);
        } catch (EntityNotFoundException e) {
            messagingTemplate.convertAndSend("/init_chat/" + idCurso, e.getMessage());
        } catch (Exception e) {
            logger.error("Error en obtener el chat del curso: {}", e.getMessage());
            messagingTemplate.convertAndSend("/init_chat/" + idCurso,
                    "Error en obtener el chat del curso: " + e.getMessage());
        }
    }

    @MessageMapping("/chat")
    public void guardaMensaje(String message) {
        try {
            this.cursoChatService.guardaMensaje(message);
        } catch (IllegalArgumentException | NoSuchElementException | DataAccessException e) {
            messagingTemplate.convertAndSend("/chat/", e.getMessage());
        } catch (Exception e) {
            logger.error("Error en guardar mensaje: {}", e.getMessage());
            messagingTemplate.convertAndSend("/chat/", "Error en guardar mensaje: " + e.getMessage());
        }
    }

    @MessageMapping("/leido")
    public void mensajeLeido(String message) {
        try {
            this.cursoChatService.mensajeLeido(message);
        } catch (EntityNotFoundException e) {
            messagingTemplate.convertAndSend("/leido", e.getMessage());
        } catch (Exception e) {
            messagingTemplate.convertAndSend("/leido", "Error en marcar el mensaje como leido: " + e.getMessage());
        }
    }

    @MessageMapping("/refresh-token")
    public void refreshToken(@Header("simpSessionId") String sessionId, String newToken) {
        try {
            this.cursoChatService.refreshTokenInOpenWebsocket(sessionId, newToken);
        } catch (Exception e) {
            messagingTemplate.convertAndSend("/refresh-token", "Error en refrescar el token: " + e.getMessage());
            // Devuelvo un error para cerrar el websocket
            throw new RuntimeException("Error en refrescar el token: " + e.getMessage());
        }
    }

    /* Sección para REST */
    @PostMapping("/crea_usuario_chat")
    public ResponseEntity<?> creaUsuarioChat(@RequestBody String message) {
        try {
            this.cursoChatService.creaUsuarioChat(message);
            return new ResponseEntity<String>("Usuario chat creado con exito!!!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en crear el usuario del chat: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/crea_curso_chat")
    public ResponseEntity<?> creaCursoChat(@RequestBody String message) {
        try {
            this.cursoChatService.creaCursoChat(message);
            return new ResponseEntity<String>("Curso chat creado con exito!!!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al crear en chat del curso: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/crea_clase_chat")
    public ResponseEntity<?> creaClaseChat(@RequestBody String message) {
        try {
            this.cursoChatService.creaClaseChat(message);
            return new ResponseEntity<String>("Clase chat creado con exito!!!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en crear el chat de la clase: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete_clase_chat/{idCurso}/{idClase}")
    public ResponseEntity<?> borrarClaseChat(@PathVariable Long idCurso, @PathVariable Long idClase) {
        try {
            this.cursoChatService.borrarClaseChat(idCurso, idClase);
            return new ResponseEntity<String>("Clase chat borrado con exito!!!", HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en borrar la clase del chat: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete_curso_chat/{idCurso}")
    public ResponseEntity<?> borrarCursoChat(@PathVariable Long idCurso) {
        try {
            this.cursoChatService.borrarCursoChat(idCurso);
            return new ResponseEntity<String>("Curso chat borrado con exito!!!", HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en borrar el chat del curso: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para actualizar el chat del curso
     * Si el chat no existe, se crea
     * 
     * @return
     */

    @PostMapping("/actualizar_curso_chat")
    public ResponseEntity<?> actualizarCursoChat(@RequestBody Curso curso) {
        try {
            this.cursoChatService.actualizarCursoChat(curso);
            return new ResponseEntity<String>("Curso chat actualizado con éxito!!!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en actualizar el chat del curso: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllCursosChat() {
        try {
            return new ResponseEntity<List<CursoChatDTO>>(this.cursoChatService.getAllCursosChat(), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error en obtener todos los cursos del chat: " + e.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para rellenar la base de datos
     */
    @GetMapping("/init")
    public ResponseEntity<?> init() {
        try {
            this.cursoChatService.init();
            return new ResponseEntity<String>("Iniciado chat con exito!!!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/nuevoUsuarioChat")
    public Boolean addUsuarioChat(@RequestBody Usuario usuario) {

        return true;
    }

}