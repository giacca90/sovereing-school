package com.sovereingschool.back_streaming.Repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sovereingschool.back_streaming.Models.Preset;

@Repository
public interface PresetRepository extends MongoRepository<Preset, String> {
    @Query(value = "{ 'id_usuario' : ?0 }")
    Optional<Preset> findByIdUsuario(Long idUsuario);

    @Query(value = "{ 'id_usuario' : ?0 }", delete = true)
    void deleteByIdUsuario(Long idUsuario);
}
