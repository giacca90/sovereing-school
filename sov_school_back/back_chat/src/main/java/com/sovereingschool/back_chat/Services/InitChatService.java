package com.sovereingschool.back_chat.Services;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Service
@Transactional
public class InitChatService {
    private UsuarioChatRepository usuarioChatRepo;
    private MensajeChatRepository mensajeChatRepo;
    private UsuarioRepository usuarioRepo;
    private CursoRepository cursoRepo;
    private CursoChatRepository cursoChatRepo;
    private ClaseRepository claseRepo;
    private SimpMessagingTemplate simpMessagingTemplate;
    private ReactiveMongoTemplate reactiveMongoTemplate;

    private Logger logger = LoggerFactory.getLogger(InitChatService.class);

    /**
     * Constructor de InitChatService
     *
     * @param cursoChatRepo   Repositorio de cursos de chat
     * @param initAppService  Servicio de inicialización
     * @param cursoRepo       Repositorio de cursos
     * @param claseRepo       Repositorio de clases
     * @param usuarioRepo     Repositorio de usuarios
     * @param mensajeChatRepo Repositorio de mensajes de chat
     * @param usuarioChatRepo Repositorio de usuarios de chat
     * @param jwtUtil         Utilidad de JWT
     */
    public InitChatService(CursoChatRepository cursoChatRepo, ClaseRepository claseRepo,
            UsuarioChatRepository usuarioChatRepo, MensajeChatRepository mensajeChatRepo,
            UsuarioRepository usuarioRepo, CursoRepository cursoRepo, SimpMessagingTemplate simpMessagingTemplate,
            ReactiveMongoTemplate reactiveMongoTemplate) {
        this.cursoChatRepo = cursoChatRepo;
        this.claseRepo = claseRepo;
        this.usuarioChatRepo = usuarioChatRepo;
        this.mensajeChatRepo = mensajeChatRepo;
        this.usuarioRepo = usuarioRepo;
        this.cursoRepo = cursoRepo;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    /**
     * Función para inicializar el chat de un usuario
     *
     * @param idUsuario el id del usuario
     * @return InitChatDTO con los los chats del usuario
     * @throws IllegalArgumentException si el idUsuario es nulo
     * @throws EntityNotFoundException  si el usuario no existe
     */
    public InitChatDTO initChat(Long idUsuario) {
        // Validar que el ID no sea nulo
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo");
        }
        UsuarioChat usuarioChat = this.usuarioChatRepo.findByIdUsuario(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el usuario chat: no se encuentra el documento");
            throw new EntityNotFoundException("Error en obtener el usuario chat: no se encuentra el documento");
        });

        // Obtiene los mensajes del usuario
        List<MensajeChatDTO> mensajesDTO = this.getMessageUser(usuarioChat);

        // Obtiene los cursos del usuario
        List<CursoChatDTO> cursosChatDTO = this.getCursosUser(usuarioChat);

        return new InitChatDTO(usuarioChat.getIdUsuario(), mensajesDTO, cursosChatDTO);
    }

    /**
     * Convierte una lista de Mensajes en una lista de MensajesDTO
     * Gestiona tambien la respuesta de los mensajes y las preguntas
     * 
     * @param mensajes Lista de mensajes
     * @return Lista de MensajesDTO
     */
    public List<MensajeChatDTO> getMensajesDTO(List<MensajeChat> mensajes) {
        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        for (MensajeChat mensaje : mensajes) {
            MensajeChat respuesta = null;
            if (mensaje.getRespuesta() != null) {
                respuesta = mensajeChatRepo.findById(mensaje.getRespuesta()).orElseThrow(() -> {
                    logger.error("Error en obtener la respuesta del mensaje");
                    throw new EntityNotFoundException("Error en obtener la respuesta del mensaje");
                });
            }
            MensajeChatDTO respuestaDTO = null;
            if (respuesta != null) {
                String nombreUsuario = this.usuarioRepo.findNombreUsuarioForId(respuesta.getIdUsuario())
                        .orElseThrow(() -> {
                            logger.error("Error en obtener el nombre del usuario");
                            throw new EntityNotFoundException("Error en obtener el nombre del usuario");
                        });
                respuestaDTO = new MensajeChatDTO(
                        respuesta.getId(), // String id_mensaje
                        respuesta.getIdCurso(), // Long id_curso
                        respuesta.getIdClase(), // Long id_clase
                        respuesta.getIdUsuario(), // Long id_usuario
                        null, // String nombre_curso
                        null, // String nombre_clase
                        nombreUsuario, // String
                        // nombre_usuario
                        null, // String foto_curso
                        this.usuarioRepo.findFotosUsuarioForId(respuesta.getIdUsuario()).get(0).split(",")[0], // String
                        // foto_usuario
                        null, // MensajeChatDTO respuesta
                        respuesta.getMomento(), // int momento
                        respuesta.getMensaje(), // String mensaje
                        respuesta.getFecha()); // Date fecha
            }

            String nombreUsuario = this.usuarioRepo.findNombreUsuarioForId(mensaje.getIdUsuario())
                    .orElseThrow(() -> {
                        logger.error("Error en obtener el nombre del usuario");
                        throw new EntityNotFoundException("Error en obtener el nombre del usuario");
                    });

            Curso curso = cursoRepo.findById(mensaje.getIdCurso()).orElseThrow(() -> {
                logger.error("InitChatService: getMensajeDTO: Error en obtener el curso con ID {}",
                        mensaje.getIdCurso());
                throw new EntityNotFoundException("Error en obtener el curso con ID " + mensaje.getIdCurso());
            });

            String nombreCurso = curso.getNombreCurso();
            String imagenCurso = curso.getImagenCurso();
            String nombreClase = curso.getClasesCurso().stream()
                    .filter(clase -> clase.getIdClase().equals(mensaje.getIdClase()))
                    .map(Clase::getNombreClase)
                    .findFirst()
                    .orElse(null);

            MensajeChatDTO mensajeDTO = new MensajeChatDTO(
                    mensaje.getId(), // String id_mensaje
                    mensaje.getIdCurso(), // Long id_curso
                    mensaje.getIdClase(), // Long id_clase
                    mensaje.getIdUsuario(), // Long id_usuario
                    nombreCurso, // String nombre_curso
                    nombreClase, // String nombre_clase
                    nombreUsuario, // String nombre_usuario
                    imagenCurso, // String foto_curso
                    usuarioRepo.findFotosUsuarioForId(mensaje.getIdUsuario()).get(0).split(",")[0], // String
                                                                                                    // foto_usuario
                    respuestaDTO, // MensajeChatDTO respuesta
                    mensaje.getMomento(), // int momento
                    mensaje.getMensaje(), // String mensaje
                    mensaje.getFecha()); // Date fecha
            mensajesDTO.add(mensajeDTO);
        }
        return mensajesDTO;
    }

    /**
     * Función para observar los cambios en las colecciones de MongoDB
     */
    @PostConstruct
    protected void observeMultipleCollections() {
        ChangeStreamOptions options = ChangeStreamOptions.builder().build();

        Flux<Document> userChatFlux = reactiveMongoTemplate
                .changeStream("users_chat", options, Document.class)
                .map(event -> {
                    Document doc = event.getBody();
                    if (doc == null) {
                        logger.error("[users_chat] Documento nulo detectado");
                        return new Document();
                    }
                    return doc;
                })
                .doOnError(err -> logger.error("[users_chat] Error en el stream: {}", err.getMessage()))
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(5))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()));

        userChatFlux.subscribe(doc -> {
            try {
                notifyUsersChat(doc);
            } catch (Exception e) {
                logger.error("[users_chat] Error al procesar documento: {}", e.getMessage());
            }
        });

        Flux<Document> coursesChatFlux = reactiveMongoTemplate
                .changeStream("courses_chat", options, Document.class)
                .map(event -> {
                    Document doc = event.getBody();
                    if (doc == null) {
                        logger.error("[courses_chat] Documento nulo detectado");
                        return new Document();
                    }
                    return doc;
                })
                .doOnError(err -> logger.error("[courses_chat] Error en el stream: {}", err.getMessage()))
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(5))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()));

        coursesChatFlux.subscribe(doc -> {
            try {
                notifyCoursesChat(doc);
            } catch (Exception e) {
                logger.error("[courses_chat] Error al procesar documento: {}", e.getMessage());
            }
        });
    }

    /**
     * Función que notifica los cambios en el chat de un curso
     * 
     * @param document Documento que contiene los cambios
     *                 El documento tiene que ser de tipo CursoChat
     */
    protected void notifyCoursesChat(Document document) {
        Long idCurso = document.getLong("idCurso");
        List<ClaseChatDTO> clases = new ArrayList<>();
        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        Curso curso = cursoRepo.findById(idCurso).orElseThrow(() -> {
            logger.error("InitChatService: notifyCoursesChat: Error en obtener el curso con ID {}", idCurso);
            throw new EntityNotFoundException("Error en obtener el curso con ID " + idCurso);
        });
        String nombreCurso = curso.getNombreCurso();
        String fotoCurso = curso.getImagenCurso();

        List<String> mensajesIDs = document.getList("mensajes", String.class);
        if (mensajesIDs != null && !mensajesIDs.isEmpty()) {
            List<MensajeChat> mensajesChat = this.mensajeChatRepo.findAllById(mensajesIDs);
            if (mensajesChat != null && !mensajesChat.isEmpty()) {
                mensajesDTO = getMensajesDTO(mensajesChat);
            }
        }

        List<Document> clasesChatDocument = document.getList("clases", Document.class);
        if (clasesChatDocument != null && !clasesChatDocument.isEmpty()) {
            for (Document claseChatDocument : clasesChatDocument) {
                List<String> mensajesClase = claseChatDocument.getList("mensajes", String.class);
                List<MensajeChatDTO> mensajesClaseDTO = new ArrayList<>();
                if (mensajesClase != null && !mensajesClase.isEmpty()) {
                    List<MensajeChat> mensajesChatClase = this.mensajeChatRepo.findAllById(mensajesClase);
                    if (mensajesChatClase != null && !mensajesChatClase.isEmpty()) {
                        mensajesClaseDTO = getMensajesDTO(mensajesChatClase);
                    }
                }
                String nombreClase = this.claseRepo.findNombreClaseById(claseChatDocument.getLong("idClase"))
                        .orElseThrow(() -> {
                            logger.error("Error en obtener el nombre de la clase en notifyCourseChat");
                            throw new EntityNotFoundException("Error en obtener el nombre de la clase");
                        });
                clases.add(new ClaseChatDTO(
                        claseChatDocument.getLong("idClase"),
                        claseChatDocument.getLong("idCurso"),
                        nombreClase, mensajesClaseDTO));
            }
        }

        CursoChatDTO cursoChatDTO = new CursoChatDTO(idCurso, clases, mensajesDTO, nombreCurso, fotoCurso);
        simpMessagingTemplate.convertAndSend("/init_chat/" + idCurso, cursoChatDTO);
    }

    /**
     * Notifica al usuario de cambios en sus chats
     * 
     * @param document Documento de chat del usuario
     */
    protected void notifyUsersChat(Document document) {
        Long idUsuario = document.getLong("idUsuario");
        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        List<CursoChatDTO> cursosDTO = new ArrayList<>();

        List<String> mensajesId = document.getList("mensajes", String.class);
        if (mensajesId != null && !mensajesId.isEmpty()) {
            List<MensajeChat> mensajes = new ArrayList<>();
            mensajesId.forEach(mexs -> mensajes.add(this.mensajeChatRepo.findById(mexs).get()));
            mensajesDTO = getMensajesDTO(mensajes);
        }

        List<String> cursosId = document.getList("cursos", String.class);
        if (cursosId != null && !cursosId.isEmpty()) {
            for (String cursoId : cursosId) {
                CursoChat cursoChat = cursoChatRepo.findById(cursoId).orElseThrow(() -> {
                    logger.error("Error en obtener el curso del chat");
                    throw new EntityNotFoundException("Error en obtener el curso del chat");
                });
                Long idCurso = cursoChat.getIdCurso();
                List<ClaseChatDTO> clasesDTO = new ArrayList<>();
                List<ClaseChat> clasesChat = cursoChat.getClases();
                if (clasesChat != null && !clasesChat.isEmpty()) {
                    for (ClaseChat claseChat : clasesChat) {
                        List<MensajeChatDTO> mensajesChatDTO = new ArrayList<>();
                        List<String> mex = claseChat.getMensajes();
                        List<MensajeChat> mensajesChat = this.mensajeChatRepo.findAllById(mex);
                        if (mensajesChat != null && !mensajesChat.isEmpty()) {
                            mensajesChatDTO = getMensajesDTO(mensajesChat);
                        }
                        String nombreClase = this.claseRepo.findNombreClaseById(claseChat.getIdClase())
                                .orElseThrow(() -> {
                                    logger.error("Error en obtener el nombre de la clase");
                                    throw new EntityNotFoundException("Error en obtener el nombre de la clase");
                                });
                        clasesDTO.add(new ClaseChatDTO(
                                claseChat.getIdClase(), // id_clase;
                                claseChat.getIdCurso(), // id_curso;
                                nombreClase, // nombre_clase;
                                mensajesChatDTO// mensajes;
                        ));
                    }
                }
                Curso curso = cursoRepo.findById(idCurso).orElseThrow(() -> {
                    logger.error("InitChatService: notifyUsersChat: Error en obtener el curso con ID {}", idCurso);
                    throw new EntityNotFoundException("Error en obtener el curso con ID " + idCurso);
                });
                String nombreCurso = curso.getNombreCurso();
                String fotoCurso = curso.getImagenCurso();
                cursosDTO.add(new CursoChatDTO(
                        idCurso,
                        clasesDTO,
                        mensajesDTO,
                        nombreCurso, // nombre_curso;
                        fotoCurso // foto_curso;
                ));
            }
        }

        InitChatDTO updateDTO = new InitChatDTO(idUsuario, mensajesDTO, cursosDTO);
        simpMessagingTemplate.convertAndSendToUser(idUsuario.toString(), "/init_chat/result", updateDTO);
    }

    /**
     * Función para obtener los mensajes del usuario
     * 
     * @param usuarioChat UsuarioChat del usuario
     * @return Lista de MensajeChatDTO
     */
    protected List<MensajeChatDTO> getMessageUser(UsuarioChat usuarioChat) {
        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        if (usuarioChat.getMensajes() != null && !usuarioChat.getMensajes().isEmpty()) {
            List<MensajeChat> mensajes = this.mensajeChatRepo.findAllById(usuarioChat.getMensajes());
            if (!mensajes.isEmpty()) {
                mensajesDTO = getMensajesDTO(mensajes);
            }
        }
        return mensajesDTO;
    }

    /**
     * Función para obtener los cursos del usuario
     * 
     * @param usuarioChat UsuarioChat del usuario
     * @return Lista de CursoChatDTO
     */
    protected List<CursoChatDTO> getCursosUser(UsuarioChat usuarioChat) {
        List<CursoChatDTO> cursosChatDTO = new ArrayList<>();
        if (usuarioChat.getCursos() != null && !usuarioChat.getCursos().isEmpty()) {
            List<CursoChat> cursosChat = this.cursoChatRepo.findAllById(usuarioChat.getCursos());
            if (cursosChat.isEmpty()) {
                for (CursoChat cursoChat : cursosChat) {
                    Curso curso = cursoRepo.findById(cursoChat.getIdCurso()).orElseThrow(() -> {
                        logger.error("InitChatService: initChat: Error en obtener el curso con ID {}",
                                cursoChat.getIdCurso());
                        throw new EntityNotFoundException("Error en obtener el curso con ID " + cursoChat.getIdCurso());
                    });
                    CursoChatDTO cursoChatDTO = new CursoChatDTO();
                    cursoChatDTO.setIdCurso(cursoChat.getIdCurso());
                    cursoChatDTO.setNombreCurso(curso.getNombreCurso());
                    cursoChatDTO.setFotoCurso(curso.getImagenCurso());
                    if (cursoChat.getUltimo() != null) {
                        Optional<MensajeChat> ultimoMensaje = this.mensajeChatRepo.findById(cursoChat.getUltimo());
                        if (ultimoMensaje.isPresent()) {
                            List<MensajeChatDTO> ultimoDTO = this.getMensajesDTO(Arrays.asList(ultimoMensaje.get()));
                            cursoChatDTO.setMensajes(ultimoDTO);
                        } else {
                            MensajeChatDTO noMessage = this.generateNoMessage(cursoChat, curso);
                            cursoChatDTO.setMensajes(Arrays.asList(noMessage));
                        }
                    } else {
                        MensajeChatDTO noMessage = this.generateNoMessage(cursoChat, curso);
                        cursoChatDTO.setMensajes(Arrays.asList(noMessage));
                    }
                    cursosChatDTO.add(cursoChatDTO);
                }
            }
        }
        return cursosChatDTO;
    }

    /**
     * Función para generar un mensaje vacío
     * 
     * @param cursoChat CursoChat del curso
     * @param curso     Curso del curso
     * @return MensajeChatDTO con los datos del mensaje
     */
    protected MensajeChatDTO generateNoMessage(CursoChat cursoChat, Curso curso) {
        return MensajeChatDTO.builder()
                .idCurso(cursoChat.getIdCurso())
                .nombreCurso(curso.getNombreCurso())
                .fotoCurso(curso.getImagenCurso())
                .mensaje("No hay mensajes en este curso")
                .build();
    }
}