package com.sovereingschool.back_streaming.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_streaming.Models.UsuarioCursos;

@Repository
public interface UsuarioCursosRepository extends MongoRepository<UsuarioCursos, String> {

    @Query(value = "{ 'id_usuario' : ?0 }")
    Optional<UsuarioCursos> findByIdUsuario(Long idUsuario);

    @Query("{ 'cursos': { $elemMatch: { 'id_curso': ?0 } } }")
    List<UsuarioCursos> findAllByIdCurso(Long idCurso);
}
