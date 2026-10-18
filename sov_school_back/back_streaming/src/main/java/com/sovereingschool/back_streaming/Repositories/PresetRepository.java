package com.sovereingschool.back_streaming.Repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sovereingschool.back_streaming.Models.Preset;

public interface PresetRepository extends MongoRepository<Preset, String> {
    @Query(value = "{ 'idUsuario' : ?0 }", delete = true)
    void deleteByIdUsuario(Long idUsuario);

    @Query(value = "{ 'idUsuario' : ?0 }")
    Optional<Preset> findByIdUsuario(Long idUsuario);
}
