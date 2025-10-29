package com.sovereingschool.back_streaming.Interfaces;

import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Usuario;

public interface IUsuarioCursosService {

    public void syncUserCourses();

    public String addNuevoUsuario(Usuario usuario);

    public boolean addClase(Long idCurso, Clase clase);

    public String getClase(Long idUsuario, Long idCurso, Long idClase);

}
