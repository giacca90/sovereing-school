package com.sovereingschool.back_streaming.Interfaces;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Usuario;

public interface IUsuarioCursosService {

    public void syncUserCourses();

    public String addNuevoUsuario(Usuario usuario) throws InternalServerException;

    public String getClase(Long idUsuario, Long idCurso, Long idClase) throws InternalServerException;

    public boolean addClase(Long idCurso, Clase clase);

    public boolean deleteClase(Long idCurso, Long idClase);

    public Long getStatus(Long idUsuario, Long idCurso) throws InternalServerException;

    public void actualizarCursoStream(Curso curso) throws InternalServerException;

    public boolean deleteUsuarioCursos(Long idUsuario);

    public boolean deleteCurso(Long id);
}
