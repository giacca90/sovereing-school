package com.sovereingschool.back_streaming.Repositories;

public interface UsuarioCursosRepositoryCustom {

    void updateProgress(Long idUsuario, Long idCurso, Long idClase, int segmentIndex);
}
