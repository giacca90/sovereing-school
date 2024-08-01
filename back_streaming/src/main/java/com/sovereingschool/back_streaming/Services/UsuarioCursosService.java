package com.sovereingschool.back_streaming.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.sovereingschool.back_streaming.Interfaces.IUsuarioCursosService;
import com.sovereingschool.back_streaming.Models.Clase;
import com.sovereingschool.back_streaming.Models.Curso;
import com.sovereingschool.back_streaming.Models.StatusClase;
import com.sovereingschool.back_streaming.Models.StatusCurso;
import com.sovereingschool.back_streaming.Models.Usuario;
import com.sovereingschool.back_streaming.Models.UsuarioCursos;
import com.sovereingschool.back_streaming.Repositories.UsuarioCursosRepository;
import com.sovereingschool.back_streaming.Repositories.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UsuarioCursosService implements IUsuarioCursosService {

    @Autowired
    private UsuarioRepository usuarioRepository; // Repositorio de PostgreSQL para usuarios

    @Autowired
    private UsuarioCursosRepository usuarioCursosRepository; // Repositorio de MongoDB

    @Autowired
    private MongoTemplate mongoTemplate; // MongoDB Template

    @Override
    public void syncUserCourses() {
        List<Usuario> users = usuarioRepository.findAll();
        for (Usuario user : users) {
            List<Curso> courses = user.getCursos_usuario();
            List<StatusCurso> courseStatuses = courses.stream().map(course -> {
                List<Clase> classes = course.getClases_curso();
                List<StatusClase> classStatuses = classes.stream().map(clazz -> {
                    StatusClase classStatus = new StatusClase();
                    classStatus.setId_clase(clazz.getId_clase());
                    classStatus.setCompleted(false);
                    classStatus.setProgress(0);
                    classStatus.setDireccion_clase(clazz.getDireccion_clase());
                    return classStatus;
                }).collect(Collectors.toList());

                StatusCurso courseStatus = new StatusCurso();
                courseStatus.setId_curso(course.getId_curso());
                courseStatus.setClases(classStatuses);
                return courseStatus;
            }).collect(Collectors.toList());

            UsuarioCursos userCourses = new UsuarioCursos();
            userCourses.setId_usuario(user.getId_usuario());
            userCourses.setCursos(courseStatuses);

            usuarioCursosRepository.save(userCourses);
        }
    }

    @Override
    public String addNuevoUsuario(Usuario usuario) {
        List<Curso> courses = usuario.getCursos_usuario();
        List<StatusCurso> courseStatuses = new ArrayList<>();
        if (courses != null) {
            courseStatuses = courses.stream().map(course -> {
                List<Clase> classes = course.getClases_curso();
                List<StatusClase> classStatuses = classes.stream().map(clazz -> {
                    StatusClase classStatus = new StatusClase();
                    classStatus.setId_clase(clazz.getId_clase());
                    classStatus.setCompleted(false);
                    classStatus.setProgress(0);
                    return classStatus;
                }).collect(Collectors.toList());

                StatusCurso courseStatus = new StatusCurso();
                courseStatus.setId_curso(course.getId_curso());
                courseStatus.setClases(classStatuses);
                return courseStatus;
            }).collect(Collectors.toList());
        }

        UsuarioCursos userCourses = new UsuarioCursos();
        userCourses.setId_usuario(usuario.getId_usuario());
        userCourses.setCursos(courseStatuses);

        usuarioCursosRepository.save(userCourses);
        return "Nuevo Usuario Insertado con Exito!!!";
    }

    @Override
    public String addCursoUsuario(Usuario usuario) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCursoUsuario'");
    }

    @Override
    public String getClase(Long id_usuario, Long id_curso, Long id_clase) {
        UsuarioCursos usuario = this.usuarioCursosRepository.findByIdUsuario(id_usuario);
        if (usuario == null)
            return null;
        List<StatusCurso> cursos = usuario.getCursos();

        for (StatusCurso curso : cursos) {
            if (curso.getId_curso().equals(id_curso)) {
                List<StatusClase> clases = curso.getClases();
                for (StatusClase clase : clases) {
                    if (clase.getId_clase().equals(id_clase)) {
                        return clase.getDireccion_clase();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Boolean editClase(Long idCurso, Clase clase) {
        try {
            // Encuentra el documento que contiene el curso específico
            Query query = new Query();
            query.addCriteria(Criteria.where("cursos.id_curso").is(idCurso));
            List<UsuarioCursos> usuarioCursos = mongoTemplate.find(query, UsuarioCursos.class);

            if (usuarioCursos == null || usuarioCursos.size() == 0) {
                System.err.println("No se encontró el documento.");
                return false;
            }

            // Itera sobre los cursos y clases para actualizar el campo deseado
            for (UsuarioCursos usuario : usuarioCursos) {
                for (StatusCurso curso : usuario.getCursos()) {
                    if (curso.getId_curso().equals(idCurso)) {
                        for (StatusClase sclase : curso.getClases()) {
                            if (sclase.getId_clase().equals(clase.getId_clase())) {
                                sclase.setDireccion_clase(clase.getDireccion_clase());
                                // Guarda el documento actualizado en la base de datos
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
            System.err.println("Error en actualizar la clase: " + e.getMessage());
            return false;
        }
    }
}
