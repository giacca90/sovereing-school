package com.sovereingschool.back_chat.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_chat.DTOs.ClaseChatDTO;
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
import jakarta.transaction.Transactional;

@Service
@Transactional
public class CursoChatService {

    private CursoChatRepository cursoChatRepo;
    private InitChatService initChatService;
    private CursoRepository cursoRepo;
    private ClaseRepository claseRepo;
    private UsuarioRepository usuarioRepo;
    private MensajeChatRepository mensajeChatRepo;
    private UsuarioChatRepository usuarioChatRepo;
    private JwtUtil jwtUtil;

    private Logger logger = LoggerFactory.getLogger(CursoChatService.class);

    public CursoChatService(CursoChatRepository cursoChatRepo, MensajeChatRepository mensajeChatRepo,
            UsuarioChatRepository usuarioChatRepo, InitChatService initChatService, ClaseRepository claseRepo) {
        this.cursoChatRepo = cursoChatRepo;
        this.mensajeChatRepo = mensajeChatRepo;
        this.usuarioChatRepo = usuarioChatRepo;
        this.initChatService = initChatService;
        this.claseRepo = claseRepo;
    }

    /**
     * Función para obtener el chat de un curso
     * 
     * @param idCurso Id del curso
     * @return CursoChatDTO con los datos del curso
     * @throws EntityNotFoundException si el curso no existe
     */
    public CursoChatDTO getCursoChat(Long idCurso) {
        CursoChat cursoChat = cursoChatRepo.findByIdCurso(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener el chat del curso");
            throw new EntityNotFoundException("Error en obtener el chat del curso");
        });
        Curso curso = cursoRepo.findById(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener el curso");
            throw new EntityNotFoundException("Error en obtener el curso");
        });
        CursoChatDTO cursoChatDTO = null;
        List<ClaseChat> clasesChat = cursoChat.getClases();
        List<ClaseChatDTO> clasesChatDTO = new ArrayList<>();
        if (clasesChat != null && !clasesChat.isEmpty()) {
            for (ClaseChat claseChat : clasesChat) {
                List<String> mensajesId = claseChat.getMensajes();
                List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
                if (mensajesId != null && !mensajesId.isEmpty()) {
                    List<MensajeChat> mensajesChat = mensajeChatRepo.findAllById(mensajesId);
                    if (mensajesChat != null && mensajesChat.size() > 0) {
                        mensajesDTO = initChatService.getMensajesDTO(mensajesChat);
                    }
                }
                String nombreClase = curso.getClases_curso().stream()
                        .filter(clase -> clase.getId_clase().equals(claseChat.getIdClase()))
                        .map(clase -> clase.getNombre_clase())
                        .findFirst() // Esto devuelve Optional<String>
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Error en obtener el nombre de la clase con ID " + claseChat.getIdClase()));

                clasesChatDTO.add(new ClaseChatDTO(
                        claseChat.getIdClase(),
                        cursoChat.getIdCurso(),
                        nombreClase,
                        mensajesDTO));
            }
        }

        List<String> mensajesId = cursoChat.getMensajes();
        List<MensajeChatDTO> mensajesChatDTO = new ArrayList<>();
        if (mensajesId != null && !mensajesId.isEmpty()) {
            List<MensajeChat> mensajesChat = mensajeChatRepo.findAllById(mensajesId);
            if (mensajesChat != null && !mensajesChat.isEmpty()) {
                mensajesChatDTO = initChatService.getMensajesDTO(mensajesChat);
            }
        }

        String nombreCurso = curso.getNombre_curso();
        String imagenCurso = curso.getImagen_curso();

        cursoChatDTO = new CursoChatDTO(
                idCurso,
                clasesChatDTO,
                mensajesChatDTO,
                nombreCurso,
                imagenCurso);
        return cursoChatDTO;
    }

    /**
     * Función para guardar un mensaje en el chat
     *
     * @param message Mensaje que se desea guardar
     * @throws IllegalArgumentException si el mensaje no es válido
     * @throws EntityNotFoundException  si no se puede encontrar un elemento
     */
    public void guardaMensaje(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        // Parse JSON
        MensajeChatDTO mensajeChatDTO;
        try {
            mensajeChatDTO = objectMapper.readValue(message, MensajeChatDTO.class);

            // Obtener ID de respuesta si existe
            String respId = null;
            if (mensajeChatDTO.getRespuesta() != null) {
                respId = mensajeChatDTO.getRespuesta().getId_mensaje();
                if (respId == null) {
                    throw new IllegalArgumentException("ID de respuesta nulo en MensajeChatDTO");
                }
            }

            // Crear y guardar el mensaje principal
            MensajeChat mensajeChat = new MensajeChat(
                    null,
                    mensajeChatDTO.getId_curso(),
                    mensajeChatDTO.getId_clase(),
                    mensajeChatDTO.getId_usuario(),
                    respId,
                    mensajeChatDTO.getPregunta(),
                    mensajeChatDTO.getMensaje(),
                    mensajeChatDTO.getFecha());
            MensajeChat savedMessage = mensajeChatRepo.save(mensajeChat);
            String savedId = savedMessage.getId();

            // Actualizar CursoChat
            CursoChat cursoChat = cursoChatRepo.findByIdCurso(mensajeChatDTO.getId_curso()).orElseThrow(() -> {
                logger.error("Error en obtener el curso del chat");
                throw new EntityNotFoundException("Error en obtener el curso del chat");
            });
            if (mensajeChatDTO.getId_clase() == null || mensajeChatDTO.getId_clase() == 0) {
                cursoChat.getMensajes().add(savedId);
            } else {
                cursoChat.getClases().stream()
                        .filter(c -> Objects.equals(c.getIdClase(), mensajeChatDTO.getId_clase()))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException(
                                "ClaseChat no encontrada para idClase: " + mensajeChatDTO.getId_clase()))
                        .getMensajes().add(savedId);
            }
            cursoChat.setUltimo(savedId);
            cursoChatRepo.save(cursoChat);

            // Actualizar UsuarioChat
            UsuarioChat usuarioChat = usuarioChatRepo.findByIdUsuario(mensajeChatDTO.getId_usuario())
                    .orElseThrow(() -> {
                        logger.error("Error en obtener el usuario del chat");
                        throw new EntityNotFoundException("Error en obtener el usuario del chat");
                    });
            if (usuarioChat.getCursos().stream().noneMatch(id -> Objects.equals(id, cursoChat.getId()))) {
                usuarioChat.getCursos().add(cursoChat.getId());
                usuarioChatRepo.save(usuarioChat);
            }
            // usuarioChat.getMensajes().add(savedId);

            // Notificar profesores si es pregunta
            if (mensajeChatDTO.getPregunta() != null) {
                Curso curso = cursoRepo.findById(mensajeChatDTO.getId_curso())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Curso no encontrado con id: " + mensajeChatDTO.getId_curso()));
                for (Usuario prof : curso.getProfesores_curso()) {
                    UsuarioChat profChat = usuarioChatRepo.findByIdUsuario(prof.getId_usuario()).orElseThrow(() -> {
                        logger.error("Error en obtener el profesor del curso");
                        throw new EntityNotFoundException("Error en obtener el profesor del curso");
                    });
                    profChat.getMensajes().add(savedId);
                    usuarioChatRepo.save(profChat);
                }
            }

            // Noitificar al usuario si es una respuesta a un su mensaje
            if (respId != null) {
                UsuarioChat respUsuario = usuarioChatRepo
                        .findByIdUsuario(mensajeChatRepo.findById(respId).get().getIdUsuario()).orElseThrow(() -> {
                            logger.error("Error en obtener el usuario del chat en la respuesta");
                            throw new EntityNotFoundException("Error en obtener el usuario del chat en la respuesta");
                        });
                respUsuario.getMensajes().add(savedId);
                usuarioChatRepo.save(respUsuario);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error al parsear JSON del mensaje: {} en el mensaje: {}", e.getMessage(), message);
            throw new IllegalArgumentException(
                    "Formato JSON inválido: " + e.getOriginalMessage() + " en el mensaje: " + message, e);
        }
    }

    /**
     * Función para crear un usuario en el chat
     * 
     * @param message
     * @throws RuntimeException
     */
    public void creaUsuarioChat(String message) {
        // Crear una instancia de ObjectMapper para parsear el JSON
        ObjectMapper objectMapper = new ObjectMapper();
        // Convertir el JSON en un objeto Usuario
        Usuario usuario;
        try {
            usuario = objectMapper.readValue(message, Usuario.class);
            if (usuarioChatRepo.findByIdUsuario(usuario.getId_usuario()).isPresent()) {
                throw new RuntimeException("Ya existe un usuario con el ID " + usuario.getId_usuario());
            }
            UsuarioChat usuarioChat = new UsuarioChat(
                    null, // String id
                    usuario.getId_usuario(), // Long id_usuario
                    new ArrayList<String>(), // List<CursoChat> cursos
                    new ArrayList<String>()); // List<String> mensajes
            usuarioChatRepo.save(usuarioChat);
        } catch (JsonProcessingException e) {
            logger.error("Error al parsear JSON del usuario: {}", e.getMessage());
            throw new RuntimeException("Error al parsear JSON del usuario: " + e.getMessage());
        }
    }

    /**
     * Función para crear un chat de curso
     * 
     * @param message String con el curso JSON
     * @throws RuntimeException si hay un error al parsear el JSON
     */
    public void creaCursoChat(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        // Convertir el JSON en un objeto MensajeChatDTO
        Curso curso;
        try {
            curso = objectMapper.readValue(message, Curso.class);

            List<Clase> clases = curso.getClases_curso();
            List<ClaseChat> clasesChat = new ArrayList<>();
            if (clases != null && clases.size() > 0) {
                for (Clase clase : clases) {
                    ClaseChat claseChat = new ClaseChat(
                            clase.getId_clase(), // Long id_clase
                            curso.getId_curso(), // Long id_curso
                            new ArrayList<String>()); // List<String> mensajes
                    clasesChat.add(claseChat);
                }
            }

            CursoChat cursoChat = new CursoChat(
                    null, // String id
                    curso.getId_curso(), // Long id_curso
                    clasesChat, // List<ClaseChat> clases
                    new ArrayList<String>(),
                    null); // List<String> mensajes
            cursoChatRepo.save(cursoChat);
        } catch (JsonProcessingException e) {
            logger.error("Error al parsear JSON del curso: {}", e.getMessage());
            throw new RuntimeException("Error al parsear JSON del curso: " + e.getMessage());
        }
    }

    /**
     * Función para crear un chat de clase
     * 
     * @param message String con la clase JSON
     * @throws RuntimeException        si hay un error al parsear el JSON
     * @throws EntityNotFoundException si no se puede encontrar un elemento
     */
    public void creaClaseChat(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        // Configurar ObjectMapper para manejar referencias circulares
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Clase clase;
        try {
            clase = objectMapper.readValue(message, Clase.class);
            ClaseChat claseChat = new ClaseChat(
                    clase.getId_clase(),
                    clase.getCurso_clase().getId_curso(),
                    new ArrayList<String>());

            CursoChat cursoChat = cursoChatRepo.findByIdCurso(clase.getCurso_clase().getId_curso()).orElseThrow(() -> {
                logger.error("Error en obtener el curso del chat");
                throw new EntityNotFoundException("Error en obtener el curso del chat");
            });
            List<ClaseChat> clasesChat = cursoChat.getClases();
            // Comprueba si clasesChat ya contiene una clase con el mismo id
            boolean exists = clasesChat.stream().anyMatch(c -> c.getIdClase() == clase.getId_clase());
            if (!exists) {
                clasesChat.add(claseChat);
                cursoChat.setClases(clasesChat);
                cursoChatRepo.save(cursoChat);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error al parsear JSON del chat de la clase: {}", e.getMessage());
            throw new RuntimeException("Error al parsear JSON del chat de la clase: " + e.getMessage());
        }
    }

    /**
     * Función para marcar un mensaje como leido, y quitarlo de los mensajes del
     * usuario
     * 
     * @param message String con el mensaje a marcar como leido
     * @throws EntityNotFoundException si no se puede encontrar un elemento
     */
    public void mensajeLeido(String message) {
        String[] lMex = message.replaceAll("\"", "").split(",");
        UsuarioChat usuario = this.usuarioChatRepo.findByIdUsuario(Long.parseLong(lMex[0])).orElseThrow(() -> {
            logger.error("Error en obtener el usuario del chat");
            throw new EntityNotFoundException("Error en obtener el usuario del chat");
        });
        if (usuario != null) {
            List<String> mensajes = usuario.getMensajes();
            mensajes.remove(lMex[1]);
            usuario.setMensajes(mensajes);
            this.usuarioChatRepo.save(usuario);
        }
    }

    /**
     * Función para borrar una clase del chat
     * 
     * @param idCurso id del curso
     * @param idClase id de la clase
     * @throws EntityNotFoundException si no se puede encontrar un elemento
     * 
     */
    public void borrarClaseChat(Long idCurso, Long idClase) {

        CursoChat cursoChat = this.cursoChatRepo.findByIdCurso(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener el curso del chat");
            throw new EntityNotFoundException("Error en obtener el curso del chat");
        });
        if (cursoChat != null) {
            List<ClaseChat> clases = cursoChat.getClases();
            for (ClaseChat clase : clases) {
                if (clase.getIdClase().equals(idClase)) {
                    clases.remove(clase);
                    break;
                }
            }
            cursoChat.setClases(clases);
            this.cursoChatRepo.save(cursoChat);
        } else {
            logger.error("No se pudo borrar la clase del chat, el curso no existe");
            throw new EntityNotFoundException("No se pudo borrar la clase del chat, el curso no existe");
        }
    }

    /**
     * Función para borrar un curso del chat
     * 
     * @param idCurso id del curso
     * @throws EntityNotFoundException si no se puede encontrar un elemento
     */
    public void borrarCursoChat(Long idCurso) {
        CursoChat cursoChat = this.cursoChatRepo.findByIdCurso(idCurso).orElseThrow(() -> {
            logger.error("Error en obtener el curso del chat");
            throw new EntityNotFoundException("Error en obtener el curso del chat");
        });
        this.cursoChatRepo.delete(cursoChat);
    }

    public void refreshTokenInOpenWebsocket(String sessionId, String newToken) {
        Authentication newAuth = jwtUtil.createAuthenticationFromToken(newToken);

        // Obtener el contexto actual y actualizar la autenticación
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(newAuth);

        // También actualizar en el accessor para que se conserve
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
        accessor.setUser(newAuth);

        logger.info("🔄 Token refrescado con éxito para sesión: {}", sessionId);
    }

    /**
     * Función para inicializar el chat de usuarios
     */
    public void init() {
        try {
            // Inicializar usuarios
            List<Usuario> usuarios = usuarioRepo.findAll();
            for (Usuario usuario : usuarios) {
                if (usuarioChatRepo.findByIdUsuario(usuario.getId_usuario()).isPresent())
                    continue;
                UsuarioChat usuarioChat = new UsuarioChat(null, usuario.getId_usuario(), new ArrayList<>(),
                        new ArrayList<>());
                usuarioChatRepo.save(usuarioChat);
            }
            // Inicializar cursos
            List<Curso> cursos = cursoRepo.findAll();
            for (Curso curso : cursos) {
                if (cursoChatRepo.findByIdCurso(curso.getId_curso()).isPresent())
                    continue;
                List<Clase> clases = curso.getClases_curso();
                List<ClaseChat> clasesChat = new ArrayList<>();
                for (Clase clase : clases) {
                    ClaseChat claseChat = new ClaseChat(clase.getId_clase(), curso.getId_curso(), new ArrayList<>());
                    clasesChat.add(claseChat);
                }
                CursoChat cursoChat = new CursoChat(null, curso.getId_curso(), clasesChat, new ArrayList<>(), null);
                cursoChatRepo.save(cursoChat);
                List<Usuario> profesores = curso.getProfesores_curso();
                for (Usuario profesor : profesores) {
                    UsuarioChat profChat = usuarioChatRepo.findByIdUsuario(profesor.getId_usuario()).orElseThrow(() -> {
                        logger.error("Error en obtener el profesor del curso");
                        throw new EntityNotFoundException("Error en obtener el profesor del curso");
                    });
                    profChat.getCursos().add(cursoChat.getId());
                    usuarioChatRepo.save(profChat);
                }
            }
        } catch (Exception e) {
            logger.error("Error en inicializar chat: {}", e.getMessage());
            throw new RuntimeException("Error en inicializar chat: " + e.getMessage());
        }
    }

    /**
     * Función para actualizar el chat del curso
     * Si el chat no existe, se crea
     * 
     * @param curso
     */
    public void actualizarCursoChat(Curso curso) {
        try {

            CursoChat cursoChat = cursoChatRepo.findByIdCurso(curso.getId_curso()).orElse(null);
            if (cursoChat == null) {
                cursoChat = new CursoChat(null, curso.getId_curso(), new ArrayList<>(), new ArrayList<>(), null);
                cursoChat = cursoChatRepo.save(cursoChat);
                // Añade el curso al chat de los profesores
                List<Usuario> profesores = curso.getProfesores_curso();
                for (Usuario profesor : profesores) {
                    UsuarioChat profChat = usuarioChatRepo.findByIdUsuario(profesor.getId_usuario()).orElseThrow(() -> {
                        logger.error("Error en obtener el profesor del curso");
                        throw new EntityNotFoundException("Error en obtener el profesor del curso");
                    });
                    profChat.getCursos().add(cursoChat.getId());
                    usuarioChatRepo.save(profChat);
                }
            }
            List<Clase> clases = curso.getClases_curso();
            List<ClaseChat> clasesChat = cursoChat.getClases();
            // Comprueba si las clases del curso es nueva
            if (clases != null && clases.size() > 0) {
                for (Clase clase : clases) {
                    ClaseChat claseChat = clasesChat.stream()
                            .filter(c -> c.getIdClase() == clase.getId_clase())
                            .findFirst()
                            .orElse(null);
                    if (claseChat == null) {
                        claseChat = new ClaseChat(clase.getId_clase(), curso.getId_curso(), new ArrayList<>());
                        clasesChat.add(claseChat);
                    }
                }
            }
            cursoChat.setClases(clasesChat);
            cursoChatRepo.save(cursoChat);
        } catch (RuntimeException e) {
            logger.error("Error en actualizar el curso en el chat: {}", e.getMessage());
            throw new RuntimeException("Error en actualizar el curso en el chat: " + e.getMessage());
        }
    }

    public List<CursoChatDTO> getAllCursosChat() {
        try {
            List<CursoChat> cursosChat = cursoChatRepo.findAll();
            List<CursoChatDTO> cursosChatDTO = new ArrayList<>();
            if (cursosChat != null && !cursosChat.isEmpty()) {
                for (CursoChat cursoChat : cursosChat) {
                    List<ClaseChatDTO> clasesChatDTO = new ArrayList<>();
                    cursoChat.getClases().forEach(clase -> {
                        clasesChatDTO.add(new ClaseChatDTO(
                                clase.getIdClase(),
                                cursoChat.getIdCurso(),
                                this.claseRepo.findNombreClaseById(clase.getIdClase()).get(),
                                new ArrayList<>()));
                    });
                    List<String> mensajesId = cursoChat.getMensajes();
                    List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
                    if (mensajesId != null && !mensajesId.isEmpty()) {
                        List<MensajeChat> mensajesChat = mensajeChatRepo.findAllById(mensajesId);
                        if (mensajesChat != null && mensajesChat.size() > 0) {
                            mensajesDTO = initChatService.getMensajesDTO(mensajesChat);
                        }
                    }
                    Curso curso = this.cursoRepo.findById(cursoChat.getIdCurso()).get();
                    cursosChatDTO.add(new CursoChatDTO(
                            curso.getId_curso(),
                            clasesChatDTO,
                            mensajesDTO,
                            curso.getNombre_curso(),
                            curso.getImagen_curso()));
                }
                return cursosChatDTO;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error al obtener todos los cursos del chat: {}", e.getMessage());
            throw new RuntimeException("Error al obtener todos los cursos del chat: " + e.getMessage());
        }
    }
}
