package com.sovereingschool.back_streaming.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
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

    @Override
    public String addNuevoUsuario(Usuario usuario) {
        if (usuarioCursosRepository.findByIdUsuario(usuario.getIdUsuario()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con el ID " + usuario.getIdUsuario());
        }
        List<Curso> courses = usuario.getCursosUsuario();
        List<StatusCurso> courseStatuses = new ArrayList<>();
        if (courses != null) {
            courseStatuses = courses.stream().map(course -> {
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
        }

        UsuarioCursos userCourses = new UsuarioCursos();
        userCourses.setIdUsuario(usuario.getIdUsuario());
        userCourses.setRolUsuario(usuario.getRollUsuario()); // Asegurarse de establecer el rol
        userCourses.setCursos(courseStatuses);

        usuarioCursosRepository.save(userCourses);
        return "Nuevo Usuario Insertado con Exito!!!";
    }

    @Override
    public String getClase(Long idUsuario, Long idCurso, Long idClase) {
        UsuarioCursos usuario = this.usuarioCursosRepository.findByIdUsuario(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el usuario del streaming. id_usuario: {}", idUsuario);
            throw new EntityNotFoundException("Error en obtener el usuario del streaming");
        });

        if (idClase == 0) {
            if (usuario.getRolUsuario().name().equals("PROFESOR") || usuario.getRolUsuario().name().equals("ADMIN")) {
                idClase = this.cursoRepository.findById(idCurso).get().getClasesCurso().get(0).getIdClase();
            }
        }

        if (usuario.getRolUsuario().name().equals("PROFESOR") || usuario.getRolUsuario().name().equals("ADMIN")) {
            try {

                String direccion = this.claseRepository.findById(idClase).get().getDireccionClase();
                if (direccion == null) {
                    logger.error("Clase no encontrada");
                    return null;
                }
                return this.claseRepository.findById(idClase).get().getDireccionClase();
            } catch (Exception e) {
                logger.error("Error en obtener la clase: {}", e.getMessage());
            }
        }

        List<StatusCurso> cursos = usuario.getCursos();

        for (StatusCurso curso : cursos) {
            if (curso.getIdCurso().equals(idCurso)) {
                List<StatusClase> clases = curso.getClases();
                for (StatusClase clase : clases) {
                    if (idClase == 0) {
                        if (!clase.isCompleted()) {
                            return this.claseRepository.findById(clase.getIdClase()).get().getDireccionClase();
                        }
                        continue;
                    }
                    if (clase.getIdClase().equals(idClase)) {
                        return this.claseRepository.findById(idClase).get().getDireccionClase();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean addClase(Long idCurso, Clase clase) {
        try {
            // Encuentra el documento que contiene el curso específico
            Query query = new Query();
            query.addCriteria(Criteria.where("cursos.id_curso").is(idCurso));
            List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

            if (usuarioCursos == null || usuarioCursos.isEmpty()) {
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

    public boolean deleteClase(Long idCurso, Long idClase) {
        try {
            // Encuentra el documento que contiene el curso específico
            Query query = new Query();
            query.addCriteria(Criteria.where("cursos.id_curso").is(idCurso));
            List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

            if (usuarioCursos == null || usuarioCursos.isEmpty()) {
                logger.error("No se encontró el documento.");
                return false;
            }

            for (UsuarioCursos usuario : usuarioCursos) {
                for (StatusCurso curso : usuario.getCursos()) {
                    if (curso.getIdCurso().equals(idCurso)) {
                        for (int i = 0; i < curso.getClases().size(); i++) {
                            if (curso.getClases().get(i).getIdClase().equals(idClase)) {
                                curso.getClases().remove(i);
                                mongoTemplate.save(usuario);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error en borrar la clase: {}", e.getMessage());
            return false;
        }
    }

    public Long getStatus(Long idUsuario, Long idCurso) {
        UsuarioCursos usuarioCursos = this.usuarioCursosRepository.findByIdUsuario(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el usuario del chat");
            throw new EntityNotFoundException("Error en obtener el usuario del chat");
        });
        if (usuarioCursos.getRolUsuario().name().equals("PROFESOR")
                || usuarioCursos.getRolUsuario().name().equals("ADMIN")) {
            return this.cursoRepository.findById(idCurso).get().getClasesCurso().get(0).getIdClase();
        } else {
            List<StatusCurso> lst = usuarioCursos.getCursos();
            for (StatusCurso sc : lst) {
                if (sc.getIdCurso().equals(idCurso)) {
                    List<StatusClase> lscl = sc.getClases();
                    for (StatusClase scl : lscl) {
                        if (!scl.isCompleted()) {
                            return scl.getIdClase();
                        }
                    }
                    return lscl.get(0).getIdClase();
                }
            }
        }
        return 0L;
    }

    public boolean deleteCurso(Long id) {
        // Encuentra el documento que contiene el curso específico
        Query query = new Query();
        query.addCriteria(Criteria.where("cursos.id_curso").is(id));
        List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

        if (usuarioCursos == null || usuarioCursos.isEmpty()) {
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

    public void actualizarCursoStream(Curso curso) {
        List<UsuarioCursos> usuarios = this.usuarioCursosRepository.findAllByIdCurso(curso.getIdCurso());
        if (usuarios != null && !usuarios.isEmpty()) {
            for (UsuarioCursos usuario : usuarios) {
                List<StatusCurso> cursosStatus = usuario.getCursos();
                for (StatusCurso cursoStatus : cursosStatus) {
                    if (cursoStatus.getIdCurso().equals(curso.getIdCurso())) {
                        cursoStatus.getClases().clear();
                        for (Clase clase : curso.getClasesCurso()) {
                            StatusClase claseStatus = new StatusClase();
                            claseStatus.setIdClase(clase.getIdClase());
                            claseStatus.setCompleted(false);
                            claseStatus.setProgress(0);
                            cursoStatus.getClases().add(claseStatus);
                        }
                        this.usuarioCursosRepository.save(usuario);
                        break;
                    }
                }
            }
        }

        // Convertir los videos del curso
        try {
            if (curso.getClasesCurso() != null && !curso.getClasesCurso().isEmpty()) {
                this.streamingService.convertVideos(curso);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error en convertir los videos del curso: {}", e.getMessage());
            throw new RuntimeException("Error en convertir los videos del curso: " + e.getMessage());
        }
    }
}
