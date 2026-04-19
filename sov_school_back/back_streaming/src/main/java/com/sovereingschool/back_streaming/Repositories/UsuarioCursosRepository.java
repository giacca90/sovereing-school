package com.sovereingschool.back_streaming.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_streaming.Models.UsuarioCursos;

@Repository
public interface UsuarioCursosRepository extends MongoRepository<UsuarioCursos, String> {

        @Query(value = "{ 'idUsuario' : ?0 }")
        Optional<UsuarioCursos> findByIdUsuario(Long idUsuario);

        @Query("{ 'cursos': { $elemMatch: { 'id_curso': ?0 } } }")
        List<UsuarioCursos> findAllByIdCurso(Long idCurso);

        /**
         * Actualiza el progreso de un fragmento de video para un usuario en una clase
         * específica.
         * 
         * @param idUsuario     ID del usuario
         * @param idCurso       ID del curso
         * @param idClase       ID de la clase
         * @param segmentIndex  Índice del fragmento reproducido
         * @param mongoTemplate Template para operaciones de MongoDB
         */
        default void updateProgress(Long idUsuario, Long idCurso, Long idClase, int segmentIndex,
                        MongoTemplate mongoTemplate) {
                org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                                org.springframework.data.mongodb.core.query.Criteria.where("idUsuario").is(idUsuario)
                                                .and("cursos.idCurso").is(idCurso)
                                                .and("cursos.clases.idClase").is(idClase));

                org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                                .addToSet("cursos.$[course].clases.$[class].progress", segmentIndex);

                update.filterArray(org.springframework.data.mongodb.core.query.Criteria.where("course.idCurso")
                                .is(idCurso));
                update.filterArray(org.springframework.data.mongodb.core.query.Criteria.where("class.idClase")
                                .is(idClase));

                mongoTemplate.updateFirst(query, update, "user_courses");
        }
}
