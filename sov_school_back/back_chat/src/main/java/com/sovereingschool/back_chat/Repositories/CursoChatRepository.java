package com.sovereingschool.back_chat.Repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sovereingschool.back_chat.Models.CursoChat;

public interface CursoChatRepository extends MongoRepository<CursoChat, String> {

    @Aggregation(pipeline = {
            "{ '$match': { 'idCurso' : ?0 } }",
            "{ '$project': { 'mensajes': { '$slice': ['$mensajes', 20] }, 'idCurso': 1, 'clases': 1 } }"
    })

    // @Query(value = "{ 'idCurso' : ?0 }")
    Optional<CursoChat> findByIdCurso(Long idCurso);
}
