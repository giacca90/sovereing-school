package com.sovereingschool.back_streaming.Services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.ClaseRepository;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_streaming.Interfaces.IUsuarioCursosService;
import com.sovereingschool.back_streaming.Models.StatusClase;
import com.sovereingschool.back_streaming.Models.StatusCurso;
import com.sovereingschool.back_streaming.Models.UsuarioCursos;
import com.sovereingschool.back_streaming.Repositories.UsuarioCursosRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class UsuarioCursosService implements IUsuarioCursosService {

    private StreamingService streamingService;
    private UsuarioRepository usuarioRepository; // Repositorio de PostgreSQL para usuarios
    private CursoRepository cursoRepository; // Repositorio de PostgreSQL para clases
    private ClaseRepository claseRepository; // Repositorio de PostgreSQL para clases
    private UsuarioCursosRepository usuarioCursosRepository; // Repositorio de MongoDB
    private MongoTemplate mongoTemplate;

    private Logger logger = LoggerFactory.getLogger(UsuarioCursosService.class);

    /**
     * Constructor de UsuarioCursosService
     *
     * @param streamingService        Servicio de streaming
     * @param usuarioRepository       Repositorio de usuarios
     * @param cursoRepository         Repositorio de cursos
     * @param claseRepository         Repositorio de clases
     * @param usuarioCursosRepository Repositorio de usuarios de cursos
     * @param mongoTemplate           Template de MongoDB
     */
    public UsuarioCursosService(StreamingService streamingService, UsuarioRepository usuarioRepository,
            CursoRepository cursoRepository,
            ClaseRepository claseRepository, UsuarioCursosRepository usuarioCursosRepository,
            MongoTemplate mongoTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.claseRepository = claseRepository;
        this.usuarioCursosRepository = usuarioCursosRepository;
        this.mongoTemplate = mongoTemplate;
        this.streamingService = streamingService;
    }

    /**
     * Función para sincronizar los cursos de los usuarios
     */
    @Override
    public void syncUserCourses() {
        List<Usuario> users = usuarioRepository.findAll();
        for (Usuario user : users) {
            if (usuarioCursosRepository.findByIdUsuario(user.getIdUsuario()).isPresent())
                continue;
            List<Curso> courses = user.getCursosUsuario();
            List<StatusCurso> courseStatuses = courses.stream().map(course -> {
                List<Clase> classes = course.getClasesCurso();
                List<StatusClase> classStatuses = classes.stream().map(clazz -> {
                    StatusClase classStatus = new StatusClase();
                    classStatus.setIdClase(clazz.getIdClase());
                    classStatus.setCompleted(false);
                    classStatus.setProgress(0);
                    return classStatus;
                }).toList();

                StatusCurso courseStatus = new StatusCurso();
                courseStatus.setIdCurso(course.getIdCurso());
                courseStatus.setClases(classStatuses);
                return courseStatus;
            }).toList();

            UsuarioCursos userCourses = new UsuarioCursos();
            userCourses.setIdUsuario(user.getIdUsuario());
            userCourses.setRolUsuario(user.getRollUsuario());
            userCourses.setCursos(courseStatuses);
            usuarioCursosRepository.save(userCourses);
        }

    }

    /**
     * Función para añadir un nuevo usuario
     * 
     * @param usuario Usuario a añadir
     * @return String con el mensaje de añadido
     * @throws InternalServerException
     */
    @Override
    public String addNuevoUsuario(Usuario usuario) throws InternalServerException {

        UsuarioCursos usuarioCursos = this.usuarioCursosRepository.findByIdUsuario(usuario.getIdUsuario())
                .orElseGet(() -> {
                    // Si no existe, lo creamos
                    UsuarioCursos nuevo = new UsuarioCursos(
                            null,
                            usuario.getIdUsuario(),
                            usuario.getRollUsuario(),
                            new ArrayList<>());
                    return this.usuarioCursosRepository.save(nuevo); // devolvemos el creado
                });

        this.updateCursosUsuario(usuario, usuarioCursos);
        return "Nuevo Usuario Insertado con Exito!!!";
    }

    /**
     * Función para obtener la clase del usuario
     * 
     * @param idUsuario ID del usuario
     * @param idCurso   ID del curso
     * @param idClase   ID de la clase
     * @return String con la dirección de la clase
     * @throws InternalServerException
     */
    @Override
    public String getClase(Long idUsuario, Long idCurso, Long idClase) throws InternalServerException {
        UsuarioCursos usuario = this.usuarioCursosRepository.findByIdUsuario(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el usuario del streaming. id_usuario: {}", idUsuario);
            throw new EntityNotFoundException("Error en obtener el usuario del streaming");
        });

        Long resolvedIdClase = idClase;

        // Si no se pide una clase
        if (resolvedIdClase == 0) {
            // Si es un profesor o admin, obtenemos la primera clase del curso
            if (usuario.getRolUsuario() == RoleEnum.PROF || usuario.getRolUsuario() == RoleEnum.ADMIN) {
                Curso curso = cursoRepository.findById(idCurso)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Curso con id " + idCurso + " no encontrado el en repositorio"));

                if (curso.getClasesCurso() == null || curso.getClasesCurso().isEmpty()) {
                    throw new InternalServerException("El curso no tiene clases registradas");
                }

                resolvedIdClase = curso.getClasesCurso().get(0).getIdClase();
            } else {
                // Si es un usuario normal, obtenemos la clase actual
                StatusCurso cursoStatus = usuario.getCursos().stream()
                        .filter(c -> c.getIdCurso().equals(idCurso))
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con id " + idCurso));

                if (cursoStatus.getClases().isEmpty()) {
                    throw new InternalServerException("El curso no tiene clases registradas");
                }

                resolvedIdClase = cursoStatus.getClases().stream()
                        .filter(c -> !c.isCompleted())
                        .findFirst()
                        .orElseGet(() -> cursoStatus.getClases().get(0))
                        .getIdClase();
            }
        }

        Clase clase = claseRepository.findById(resolvedIdClase).orElseThrow(
                () -> new EntityNotFoundException("Clase no encontrada con id " + idClase));
        String direccion = clase.getDireccionClase();
        if (direccion == null) {
            logger.error("Clase sin direccion");
            return null;
        }
        return direccion;
    }

    /**
     * Función para añadir una clase al usuario
     * 
     * @param idCurso ID del curso
     * @param clase   Clase a añadir
     * @return Booleano con el resultado de la operación
     */
    @Override
    public boolean addClase(Long idCurso, Clase clase) {
        try {
            // Encuentra el documento que contiene el curso específico
            Query query = new Query();
            query.addCriteria(Criteria.where("cursos.id_curso").is(idCurso));
            List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

            if (usuarioCursos.isEmpty()) {
                logger.error("No se encontró el documento.");
                return false;
            }

            for (UsuarioCursos usuario : usuarioCursos) {
                for (StatusCurso curso : usuario.getCursos()) {
                    if (curso.getIdCurso().equals(idCurso)) {
                        curso.getClases()
                                .add(new StatusClase(clase.getIdClase(), false, 0));
                        mongoTemplate.save(usuario);
                        break;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error en añadir la cueva clase: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Función para eliminar una clase del usuario
     * 
     * @param idCurso ID del curso
     * @param idClase ID de la clase
     * @return Booleano con el resultado de la operación
     */
    @Override
    public boolean deleteClase(Long idCurso, Long idClase) {
        try {
            // Encuentra el documento que contiene el curso específico
            Query query = new Query();
            query.addCriteria(Criteria.where("cursos.id_curso").is(idCurso));
            List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

            if (usuarioCursos.isEmpty()) {
                logger.error("No se encontró el documento.");
                return false;
            }

            for (UsuarioCursos usuario : usuarioCursos) {
                usuario.getCursos().stream()
                        .filter(c -> c.getIdCurso().equals(idCurso))
                        .findFirst()
                        .ifPresent(curso -> curso.getClases().removeIf(c -> c.getIdClase().equals(idClase)));

                mongoTemplate.save(usuario);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error en borrar la clase: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Función para obtener el estado del curso
     * 
     * @param idUsuario ID del usuario
     * @param idCurso   ID del curso
     * @return Long con el estado del curso
     * @throws InternalServerException
     */
    @Override
    public Long getStatus(Long idUsuario, Long idCurso) throws InternalServerException {
        UsuarioCursos usuarioCursos = usuarioCursosRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    logger.error("Usuario no encontrado para chat: {}", idUsuario);
                    return new EntityNotFoundException("Error en obtener el usuario del chat");
                });

        // 1. Lógica para PROF o ADMIN
        if (usuarioCursos.getRolUsuario() == RoleEnum.PROF || usuarioCursos.getRolUsuario() == RoleEnum.ADMIN) {
            return getFirstClaseIdFromCurso(idCurso);
        }

        // 2. Lógica para Alumnos (usando Streams para mayor claridad)
        return usuarioCursos.getCursos().stream()
                .filter(sc -> sc.getIdCurso().equals(idCurso))
                .findFirst()
                .map(this::findNextOrFirstClase)
                .orElse(0L);
    }

    /**
     * Función para actualizar el streaming del curso
     * 
     * @param curso Curso a actualizar
     * @throws InternalServerException
     */
    @Override
    // TODO: Revisar - Implementado procesamiento por lotes y Streams
    public void actualizarCursoStream(Curso curso) throws InternalServerException {
        List<UsuarioCursos> usuarios = this.usuarioCursosRepository.findAllByIdCurso(curso.getIdCurso());

        if (usuarios != null && !usuarios.isEmpty()) {
            // 1. Procesar cada usuario
            usuarios.forEach(usuario -> actualizarStatusUsuario(usuario, curso));

            // 2. Guardar todos los usuarios de una vez (Batch Save)
            // SonarQube S2118: Es mucho más eficiente guardar la lista completa fuera del
            // bucle
            this.usuarioCursosRepository.saveAll(usuarios);
        }

        // 3. Convertir los videos del curso
        procesarVideosAsync(curso);
    }

    /**
     * Función para eliminar el curso del usuario
     * 
     * @param idUsuario ID del usuario
     * @return Booleano con el resultado de la operación
     */
    @Override
    public boolean deleteUsuarioCursos(Long idUsuario) {
        UsuarioCursos usuarioCursos = this.usuarioCursosRepository.findByIdUsuario(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el usuario del chat");
            throw new EntityNotFoundException("Error en obtener el usuario del chat");
        });

        this.usuarioCursosRepository.delete(usuarioCursos);
        return true;
    }

    /**
     * Función para eliminar el curso
     * 
     * @param id ID del curso
     * @return Booleano con el resultado de la operación
     */
    @Override
    public boolean deleteCurso(Long id) {
        // Encuentra el documento que contiene el curso específico
        Query query = new Query();
        query.addCriteria(Criteria.where("cursos.id_curso").is(id));
        List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

        if (usuarioCursos.isEmpty()) {
            logger.error("No se encontró el documento.");
            return false;
        }

        for (UsuarioCursos usrCursos : usuarioCursos) {
            List<StatusCurso> statusCurso = usrCursos.getCursos();
            for (int i = 0; i < statusCurso.size(); i++) {
                if (statusCurso.get(i).getIdCurso().equals(id)) {
                    statusCurso.remove(i);
                    this.usuarioCursosRepository.save(usrCursos);
                    break;
                }
            }
        }
        return true;
    }

    protected Long getFirstClaseIdFromCurso(Long idCurso) throws InternalServerException {
        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con id " + idCurso));

        if (curso.getClasesCurso() == null || curso.getClasesCurso().isEmpty()) {
            throw new InternalServerException("El curso no tiene clases registradas");
        }
        return curso.getClasesCurso().get(0).getIdClase();
    }

    protected Long findNextOrFirstClase(StatusCurso statusCurso) {
        List<StatusClase> clases = statusCurso.getClases();
        if (clases == null || clases.isEmpty()) {
            return 0L;
        }

        // Buscamos la primera clase no completada, si todas están completas, devolvemos
        // la primera
        return clases.stream()
                .filter(scl -> !scl.isCompleted())
                .map(StatusClase::getIdClase)
                .findFirst()
                .orElseGet(() -> clases.get(0).getIdClase());
    }

    protected void updateCursosUsuario(Usuario usuario, UsuarioCursos usuarioCursos) {
        List<Curso> cursos = usuario.getCursosUsuario();
        List<StatusCurso> cursosStatus = usuarioCursos.getCursos();
        // Buscamos cursos nuevos
        for (Curso curso : cursos) {
            cursosStatus.stream()
                    .filter(c -> c.getIdCurso().equals(curso.getIdCurso()))
                    .findFirst().orElseGet(() -> {
                        // Es un curso nuevo, creamos el StatusCurso
                        StatusCurso cursoStatus = new StatusCurso();
                        cursoStatus.setIdCurso(curso.getIdCurso());
                        List<StatusClase> clases = this.createClasesCurso(curso);
                        cursoStatus.setClases(clases);
                        // Añadimos el StatusCurso al usuario
                        usuarioCursos.getCursos().add(cursoStatus);
                        return cursoStatus;
                    });
        }
        // Actualizamos el usuarioCursos
        this.usuarioCursosRepository.save(usuarioCursos);
    }

    protected List<StatusClase> createClasesCurso(Curso curso) {
        List<Clase> clases = curso.getClasesCurso();
        return clases.stream().map(clazz -> {
            StatusClase classStatus = new StatusClase();
            classStatus.setIdClase(clazz.getIdClase());
            classStatus.setCompleted(false);
            classStatus.setProgress(0);
            return classStatus;
        }).toList();
    }

    protected void actualizarStatusUsuario(UsuarioCursos usuario, Curso curso) {
        usuario.getCursos().stream()
                .filter(cs -> cs.getIdCurso().equals(curso.getIdCurso()))
                .findFirst()
                .ifPresent(cursoStatus -> {
                    // Limpiar y reconstruir la lista de clases
                    cursoStatus.getClases().clear();

                    List<StatusClase> nuevasClases = curso.getClasesCurso().stream()
                            .map(this::mapToStatusClase)
                            .toList();

                    cursoStatus.getClases().addAll(nuevasClases);
                });
    }

    protected StatusClase mapToStatusClase(Clase clase) {
        StatusClase claseStatus = new StatusClase();
        claseStatus.setIdClase(clase.getIdClase());
        claseStatus.setCompleted(false);
        claseStatus.setProgress(0);
        return claseStatus;
    }

    protected void procesarVideosAsync(Curso curso) throws InternalServerException {
        try {
            if (curso.getClasesCurso() != null && !curso.getClasesCurso().isEmpty()) {
                this.streamingService.convertVideos(curso);
            }
        } catch (NotFoundException e) {
            // Restaurar interrupción si es necesario
            Thread.currentThread().interrupt();
            throw new InternalServerException("Error al convertir los videos del curso: " + e.getMessage());
        }
    }
}
