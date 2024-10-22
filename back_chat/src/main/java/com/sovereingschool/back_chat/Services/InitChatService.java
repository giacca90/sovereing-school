package com.sovereingschool.back_chat.Services;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
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
import com.sovereingschool.back_chat.Repositories.ClaseRepository;
import com.sovereingschool.back_chat.Repositories.CursoRepository;
import com.sovereingschool.back_chat.Repositories.MensajeChatRepository;
import com.sovereingschool.back_chat.Repositories.UsuarioChatRepository;
import com.sovereingschool.back_chat.Repositories.UsuarioRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Flux;

@Service
@Transactional
public class InitChatService {
    @Autowired
    private UsuarioChatRepository usuarioChatRepo;

    @Autowired
    private MensajeChatRepository mensajeChatRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CursoRepository cursoRepo;

    @Autowired
    private ClaseRepository claseRepo;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    public InitChatDTO initChat(Long idUsuario) {

        UsuarioChat usuarioChat = this.usuarioChatRepo.findByIdUsuario(idUsuario);
        System.out.println("USUARIOCHAT: " + usuarioChat);
        if (usuarioChat == null) {
            usuarioChat = new UsuarioChat(null, 0L, null, null); // Objeto por defecto si no se encuentra
            return new InitChatDTO();
        }
        List<MensajeChat> mensajes = new ArrayList<>();
        usuarioChat.getMensajes().forEach((mexID) -> {
            mensajes.add(this.mensajeChatRepo.findById(mexID).get());
        });
        System.out.println("MENSAJESCHAT: " + mensajes);
        List<CursoChat> cursos = usuarioChat.getCursos();
        System.out.println("CURSOCHAT: " + cursos);

        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        if (mensajes != null && mensajes.size() > 0) {
            mensajesDTO = getMensajesDTO(mensajes);
        }

        List<CursoChatDTO> cursosDTO = new ArrayList<>();
        if (cursos != null && cursos.size() > 0) {
            for (CursoChat curso : cursos) {
                CursoChatDTO cursoDTO = new CursoChatDTO();
                cursoDTO.setId_curso(curso.getIdCurso());
                cursoDTO.setNombre_curso(cursoRepo.findNombreCursoById(curso.getIdCurso()));
                cursoDTO.setFoto_curso(cursoRepo.findImagenCursoById(curso.getIdCurso()));
                List<ClaseChatDTO> clasesDTO = new ArrayList<>();
                List<ClaseChat> clases = curso.getClases();
                if (clases != null && clases.size() > 0) {
                    for (ClaseChat clase : clases) {
                        List<MensajeChatDTO> mensajeChatDTOs = new ArrayList<>();
                        String respuestaId = null;
                        List<String> mensajeIDs = clase.getMensajes();
                        String primerMensajeID = null;
                        if (mensajeIDs != null && mensajeIDs.size() > 0) {
                            primerMensajeID = mensajeIDs.getFirst();
                        }
                        if (primerMensajeID != null) {
                            respuestaId = this.mensajeChatRepo.findById(primerMensajeID).get().getRespuesta();
                        }
                        MensajeChatDTO respuestaDTO = null;
                        if (respuestaId != null) {
                            MensajeChat respuesta = this.mensajeChatRepo.findById(respuestaId).get();
                            respuestaDTO = new MensajeChatDTO(
                                    respuesta.getId(), // String id_mensaje
                                    respuesta.getIdCurso(), // Long id_curso
                                    respuesta.getIdClase(), // Long id_clase
                                    respuesta.getIdUsuario(), // Long id_usuario
                                    null, // String nombre_curso
                                    null, // String nombre_clase
                                    this.usuarioRepo.findNombreUsuarioForId(respuesta.getIdUsuario()), // String
                                    // nombre_usuario
                                    null, // String foto_curso
                                    this.usuarioRepo.findFotosUsuarioForId(respuesta.getIdUsuario()).get(0), // String
                                    // foto_usuario
                                    null, // MensajeChatDTO respuesta
                                    respuesta.getMensaje(), // String mensaje
                                    respuesta.getFecha()); // Date fecha
                        }
                        if (primerMensajeID != null) {
                            MensajeChat mex = this.mensajeChatRepo.findById(primerMensajeID).get();
                            mensajeChatDTOs.add(new MensajeChatDTO(
                                    mex.getId(), // String id_mensaje
                                    mex.getIdCurso(), // Long id_curso
                                    mex.getIdClase(), // Long id_clase
                                    mex.getIdUsuario(), // Long id_usuario
                                    this.cursoRepo.findNombreCursoById(
                                            (this.mensajeChatRepo.findById(clase.getMensajes().getFirst()).get())
                                                    .getIdCurso()), // String nombre_curso
                                    this.claseRepo.findNombreClaseById(
                                            (this.mensajeChatRepo.findById(clase.getMensajes().getFirst()).get())
                                                    .getIdClase()), // String nombre_clase
                                    this.usuarioRepo.findNombreUsuarioForId(
                                            (this.mensajeChatRepo.findById(clase.getMensajes().getFirst()).get())
                                                    .getIdUsuario()), // String nombre_usuario
                                    this.cursoRepo.findImagenCursoById(
                                            (this.mensajeChatRepo.findById(clase.getMensajes().getFirst()).get())
                                                    .getIdCurso()), // String foto_curso
                                    this.usuarioRepo.findFotosUsuarioForId(
                                            (this.mensajeChatRepo.findById(clase.getMensajes().getFirst()).get())
                                                    .getIdUsuario())
                                            .get(0), // String foto_usuario
                                    respuestaDTO, // MensajeChatDTO respuesta
                                    mex.getMensaje(), // String mensaje
                                    mex.getFecha())); // Date fecha

                        }
                        clasesDTO.add(new ClaseChatDTO(
                                clase.getIdClase(), // Long id_clase
                                clase.getIdCurso(), // Long id_curso
                                this.claseRepo.findById(clase.getIdClase()).get().getNombre_clase(),
                                mensajeChatDTOs));
                    }
                }
                List<MensajeChatDTO> mensajesCursoDTO = getMensajesDTO(
                        this.mensajeChatRepo.findAllById(curso.getMensajes()));
                cursoDTO.setMensajes(mensajesCursoDTO);
                cursosDTO.add(cursoDTO);
            }
        }
        return new InitChatDTO(usuarioChat.getIdUsuario(), mensajesDTO, cursosDTO);
    }

    @PostConstruct
    public void observeMultipleCollections() {
        System.out.println("Observing multiple collections");

        // Configura las opciones de ChangeStream
        ChangeStreamOptions options = ChangeStreamOptions.builder()
                .build();

        Flux<Document> userChatFlux = reactiveMongoTemplate
                .changeStream("users_chat", options, Document.class)
                .map(ChangeStreamEvent::getBody); // Extrae el documento directamente

        // Configura el ChangeStream y escucha los eventos

        userChatFlux.subscribe(changedDocument -> {
            System.out.println("Users_chat modificado: " + changedDocument);
            notifyUsersChat(changedDocument);
        });

        Flux<Document> coursesChatFlux = reactiveMongoTemplate
                .changeStream("courses_chat", options, Document.class)
                .map(ChangeStreamEvent::getBody); // Extrae el documento directamente

        coursesChatFlux.subscribe(changedDocument -> {
            System.out.println("Courses_chat modificado: " + changedDocument);
            notifyCoursesChat(changedDocument);
        });
    }

    /**
     * Convierte una lista de Mensajes en una lista de MensajesDTO
     * Gestiona tambien la respuesta de los mensajes
     * 
     * @param mensajes Lista de mensajes
     * @return Lista de MensajesDTO
     */
    List<MensajeChatDTO> getMensajesDTO(List<MensajeChat> mensajes) {
        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        for (MensajeChat mensaje : mensajes) {
            MensajeChat respuesta = null;
            if (mensaje.getRespuesta() != null) {
                respuesta = mensajeChatRepo.findById(mensaje.getRespuesta()).get();
            }
            MensajeChatDTO respuestaDTO = null;
            if (respuesta != null) {
                respuestaDTO = new MensajeChatDTO(
                        respuesta.getId(), // String id_mensaje
                        respuesta.getIdCurso(), // Long id_curso
                        respuesta.getIdClase(), // Long id_clase
                        respuesta.getIdUsuario(), // Long id_usuario
                        null, // String nombre_curso
                        null, // String nombre_clase
                        this.usuarioRepo.findNombreUsuarioForId(respuesta.getIdUsuario()), // String
                        // nombre_usuario
                        null, // String foto_curso
                        this.usuarioRepo.findFotosUsuarioForId(respuesta.getIdUsuario()).get(0), // String
                        // foto_usuario
                        null, // MensajeChatDTO respuesta
                        respuesta.getMensaje(), // String mensaje
                        respuesta.getFecha()); // Date fecha
            }

            MensajeChatDTO mensajeDTO = new MensajeChatDTO(
                    mensaje.getId(), // String id_mensaje
                    mensaje.getIdCurso(), // Long id_curso
                    mensaje.getIdClase(), // Long id_clase
                    mensaje.getIdUsuario(), // Long id_usuario
                    cursoRepo.findNombreCursoById(mensaje.getIdCurso()), // String nombre_curso
                    claseRepo.findNombreClaseById(mensaje.getIdClase()), // String nombre_clase
                    usuarioRepo.findNombreUsuarioForId(mensaje.getIdUsuario()), // String nombre_usuario
                    cursoRepo.findImagenCursoById(mensaje.getIdCurso()), // String foto_curso
                    usuarioRepo.findNombreUsuarioForId(mensaje.getIdUsuario()), // String foto_usuario
                    respuestaDTO, // MensajeChatDTO respuesta
                    mensaje.getMensaje(), // String mensaje
                    mensaje.getFecha()); // Date fecha
            mensajesDTO.add(mensajeDTO);
        }
        return mensajesDTO;
    }

    /**
     * Función que notifica al usuario de los cambios en el chat de un curso
     * 
     * @param document Documento que contiene los cambios
     *                 El documento tiene que ser de tipo CursoChat
     */
    private void notifyCoursesChat(Document document) {
        System.out.println("Documento modificado: " + document);
        Long id_curso = document.getLong("idCurso");
        List<ClaseChatDTO> clases = new ArrayList<>();
        List<MensajeChatDTO> mensajes = new ArrayList<>();
        String nombre_curso = this.cursoRepo.findNombreCursoById(id_curso);
        String foto_curso = this.cursoRepo.findImagenCursoById(id_curso);

        List<String> mensajesIDs = document.getList("mensajes", String.class);
        if (mensajesIDs != null && mensajesIDs.size() > 0) {
            List<MensajeChat> mensajesChat = this.mensajeChatRepo.findAllById(mensajesIDs);
            if (mensajesChat != null && mensajesChat.size() > 0) {
                mensajes = getMensajesDTO(mensajesChat);
            }
        }

        List<Document> clasesChatD = document.getList("clases", Document.class);
        if (clasesChatD != null && clasesChatD.size() > 0) {
            for (Document claseChatD : clasesChatD) {
                List<String> mensajesClase = claseChatD.getList("mensajes", String.class);
                List<MensajeChatDTO> mensajesClaseDTO = new ArrayList<>();
                if (mensajesClase != null && mensajesClase.size() > 0) {
                    List<MensajeChat> mensajesChatClase = this.mensajeChatRepo.findAllById(mensajesClase);
                    if (mensajesChatClase != null && mensajesChatClase.size() > 0) {
                        mensajesClaseDTO = getMensajesDTO(mensajesChatClase);
                    }
                }
                clases.add(new ClaseChatDTO(claseChatD.getLong("idClase"), claseChatD.getLong("idCurso"),
                        this.claseRepo.findNombreClaseById(claseChatD.getLong("idClase")), mensajesClaseDTO));
            }
        }

        CursoChatDTO cursoChatDTO = new CursoChatDTO(id_curso, clases, mensajes, nombre_curso, foto_curso);
        simpMessagingTemplate.convertAndSend("/init_chat/" + id_curso, cursoChatDTO);
    }

    /**
     * Notifica al usuario de cambios en sus chats
     * 
     * @param document Documento de chat del usuario
     */
    private void notifyUsersChat(Document document) {
        System.out.println("DOCUMENTO MODIFICADO: " + document);
        Long idUsuario = document.getLong("idUsuario");
        List<MensajeChatDTO> mensajesDTO = new ArrayList<>();
        List<CursoChatDTO> cursosDTO = new ArrayList<>();

        List<MensajeChat> mensajes = document.getList("mensajes", MensajeChat.class);
        if (mensajes != null && mensajes.size() > 0) {
            mensajesDTO = getMensajesDTO(mensajes);
        }

        List<Document> cursosD = document.getList("cursos", Document.class);
        if (cursosD != null && cursosD.size() > 0) {
            for (Document cursoD : cursosD) {
                Long idCurso = cursoD.getLong("idCurso");
                List<ClaseChatDTO> clasesDTO = new ArrayList<>();
                List<Document> clasesD = cursoD.getList("clases", Document.class);
                if (clasesD != null && clasesD.size() > 0) {
                    for (Document claseD : clasesD) {
                        List<MensajeChatDTO> mensajesChatDTO = new ArrayList<>();
                        List<String> mex = claseD.getList("mensajes", String.class);
                        List<MensajeChat> mensajesChat = this.mensajeChatRepo.findAllById(mex);
                        if (mensajesChat != null && mensajesChat.size() > 0) {
                            mensajesChatDTO = getMensajesDTO(mensajesChat);
                        }
                        clasesDTO.add(new ClaseChatDTO(
                                claseD.getLong("idClase"), // id_clase;
                                claseD.getLong("idCurso"), // id_curso;
                                this.claseRepo.findNombreClaseById(claseD.getLong("idClase")), // nombre_clase;
                                mensajesChatDTO// mensajes;
                        ));
                    }
                }
                cursosDTO.add(new CursoChatDTO(
                        idCurso,
                        clasesDTO,
                        mensajesDTO,
                        this.cursoRepo.findNombreCursoById(idCurso), // nombre_curso;
                        this.cursoRepo.findImagenCursoById(idCurso) // foto_curso;
                ));
            }
        }

        InitChatDTO updateDTO = new InitChatDTO(idUsuario, mensajesDTO, cursosDTO);
        simpMessagingTemplate.convertAndSend("/init_chat/result", updateDTO);
    }
}