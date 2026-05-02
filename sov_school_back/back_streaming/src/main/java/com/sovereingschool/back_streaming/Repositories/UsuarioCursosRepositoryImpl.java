package com.sovereingschool.back_streaming.Repositories;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_streaming.Models.UsuarioCursos;

@Repository
public class UsuarioCursosRepositoryImpl implements UsuarioCursosRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public UsuarioCursosRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void updateProgress(Long idUsuario, Long idCurso, Long idClase, int segmentIndex) {
        Query query = new Query(Criteria.where("idUsuario").is(idUsuario)
                .and("cursos.idCurso").is(idCurso)
                .and("cursos.clases.idClase").is(idClase));

        Update update = new Update()
                .addToSet("cursos.$[course].clases.$[class].progress", segmentIndex)
                .filterArray(Criteria.where("course.idCurso").is(idCurso))
                .filterArray(Criteria.where("class.idClase").is(idClase));

        mongoTemplate.updateFirst(query, update, UsuarioCursos.class);
    }
}
